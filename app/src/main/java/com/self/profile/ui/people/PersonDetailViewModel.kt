package com.self.profile.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.CommonGround
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Photo
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.domain.model.WishItem
import com.self.profile.ui.navigation.Route
import com.self.profile.util.PhotoStorage
import com.self.profile.util.ReminderCalc
import com.self.profile.work.ReminderScheduler
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * 人物详情 ViewModel。
 *
 * 阶段 1：只读
 * 阶段 3：编辑（14 模块 + 互动记录 + 共同点）+ 删除
 * Phase 2：相关提醒展示 / 增改删（软删进回收站）+ 愿望勾选
 */
@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val photoStorage: PhotoStorage,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val personId: String = savedState.get<String>(Route.PersonDetail.ARG_PERSON_ID).orEmpty()

    val state: StateFlow<PersonDetailUiState> = combine(
        personRepo.observeById(personId),
        reminderRepo.observeByPerson(personId)
    ) { person, reminders ->
        if (person == null) PersonDetailUiState.NotFound
        else PersonDetailUiState.Loaded(person, reminders)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonDetailUiState.Loading
    )

    /**
     * 通用更新：transform 现有 person，生成新 Person，自动更新 updatedAt，持久化。
     */
    fun updatePerson(transform: (Person) -> Person) {
        val current = state.value
        if (current !is PersonDetailUiState.Loaded) return
        viewModelScope.launch {
            val updated = transform(current.person).copy(
                updatedAt = System.currentTimeMillis()
            )
            personRepo.upsert(updated)
        }
    }

    /**
     * 进入详情页时更新 lastViewed_at（用于"最近查看"排序）。
     */
    fun markViewed() {
        val current = state.value
        if (current !is PersonDetailUiState.Loaded) return
        if (current.person.isSelf) return
        viewModelScope.launch {
            personRepo.updateLastViewed(current.person.id, System.currentTimeMillis())
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = state.value
        if (current !is PersonDetailUiState.Loaded) return
        if (current.person.isSelf) return  // self 不能删
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // 软删进回收站，30 天内可恢复；关联提醒一并隐藏
            personRepo.softDelete(current.person.id, now)
            reminderRepo.softDeleteByPerson(current.person.id, now)
            onDone()
        }
    }

    // ====== 互动记录 ======

    fun addInteractionNote(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(
                interactionNotes = p.interactionNotes + Note(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    date = System.currentTimeMillis() / 86_400_000L
                )
            )
        }
    }

    fun removeInteractionNote(id: String) {
        updatePerson { p -> p.copy(interactionNotes = p.interactionNotes.filter { it.id != id }) }
    }

    // ====== 共同点 ======

    fun addCommonGround(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(
                commonGrounds = p.commonGrounds + CommonGround(
                    id = UUID.randomUUID().toString(),
                    content = content
                )
            )
        }
    }

    fun removeCommonGround(id: String) {
        updatePerson { p -> p.copy(commonGrounds = p.commonGrounds.filter { it.id != id }) }
    }

    // ====== 喜欢 / 不喜欢 ======

    fun addCategoryItem(isLiked: Boolean, category: String, item: String) {
        if (category.isBlank() || item.isBlank()) return
        updatePerson { p ->
            val source = if (isLiked) p.thingsLiked else p.thingsDisliked
            val newList = source.toMutableList()
            val idx = newList.indexOfFirst { it.category == category }
            if (idx >= 0) {
                val existing = newList[idx]
                if (!existing.items.contains(item)) {
                    newList[idx] = existing.copy(items = existing.items + item)
                }
            } else {
                newList.add(CategorizedItems(category, listOf(item)))
            }
            if (isLiked) p.copy(thingsLiked = newList) else p.copy(thingsDisliked = newList)
        }
    }

    fun removeCategoryItem(isLiked: Boolean, category: String, item: String) {
        updatePerson { p ->
            val source = if (isLiked) p.thingsLiked else p.thingsDisliked
            val newList = source.mapNotNull { c ->
                if (c.category == category) {
                    val items = c.items.filter { it != item }
                    if (items.isEmpty()) null else c.copy(items = items)
                } else c
            }
            if (isLiked) p.copy(thingsLiked = newList) else p.copy(thingsDisliked = newList)
        }
    }

    fun addCategory(isLiked: Boolean, category: String) {
        if (category.isBlank()) return
        val current = state.value as? PersonDetailUiState.Loaded ?: return
        val source = if (isLiked) current.person.thingsLiked else current.person.thingsDisliked
        if (source.any { it.category == category }) return
        val updated = if (isLiked) {
            current.person.copy(thingsLiked = source + CategorizedItems(category, emptyList()))
        } else {
            current.person.copy(thingsDisliked = source + CategorizedItems(category, emptyList()))
        }
        viewModelScope.launch {
            personRepo.upsert(updated.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // ====== 愿望 / 憧憬 / 轨迹（人物版也用，但 self 不存） ======

    fun addWishItem(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(wishList = p.wishList + WishItem(
                id = UUID.randomUUID().toString(),
                content = content,
                completed = false,
                createdAt = System.currentTimeMillis()
            ))
        }
    }

    fun toggleWishItem(id: String) {
        updatePerson { p ->
            p.copy(wishList = p.wishList.map {
                if (it.id == id) it.copy(completed = !it.completed) else it
            })
        }
    }

    fun removeWishItem(id: String) {
        updatePerson { p -> p.copy(wishList = p.wishList.filter { it.id != id }) }
    }

    fun addAspiration(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(aspirations = p.aspirations + Aspiration(
                id = UUID.randomUUID().toString(),
                content = content,
                createdAt = System.currentTimeMillis()
            ))
        }
    }

    fun removeAspiration(id: String) {
        updatePerson { p -> p.copy(aspirations = p.aspirations.filter { it.id != id }) }
    }

    fun addLifeJourney(year: String, title: String, type: String, description: String) {
        if (title.isBlank()) return
        updatePerson { p ->
            p.copy(lifeJourney = p.lifeJourney + LifeJourneyEntry(
                id = UUID.randomUUID().toString(),
                year = year, title = title, type = type, description = description
            ))
        }
    }

    fun removeLifeJourney(id: String) {
        updatePerson { p -> p.copy(lifeJourney = p.lifeJourney.filter { it.id != id }) }
    }

    // ====== 照片墙 ======

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.saveFromUri(uri) ?: return@launch
            val now = System.currentTimeMillis()
            updatePerson { p ->
                p.copy(photos = p.photos + Photo(
                    id = UUID.randomUUID().toString(),
                    url = path,
                    description = null,
                    createdAt = now
                ))
            }
        }
    }

    fun removePhoto(photoId: String) {
        val current = state.value as? PersonDetailUiState.Loaded ?: return
        val photo = current.person.photos.firstOrNull { it.id == photoId } ?: return
        photoStorage.delete(photo.url)
        updatePerson { p -> p.copy(photos = p.photos.filter { it.id != photoId }) }
    }

    fun updatePhotoDescription(photoId: String, description: String) {
        updatePerson { p ->
            p.copy(photos = p.photos.map {
                if (it.id == photoId) it.copy(description = description.takeIf { d -> d.isNotBlank() }) else it
            })
        }
    }

    // ====== 相关提醒 ======

    /**
     * 新增/更新该人物的提醒并立即调度。isLunar 仅 YEARLY 生效（按农历逐年换算公历）。
     */
    fun saveReminder(
        existingId: String?,
        type: ReminderType,
        title: String,
        date: LocalDate,
        advanceDays: Int,
        reminderTime: String,
        repeat: RepeatRule,
        notes: String?,
        isLunar: Boolean
    ) {
        if (title.isBlank()) return
        val personId = (state.value as? PersonDetailUiState.Loaded)?.person?.id ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val reminder = Reminder(
                id = existingId ?: UUID.randomUUID().toString(),
                personId = personId,
                type = type,
                title = title.trim(),
                date = date.toEpochDay(),
                advanceDays = advanceDays.coerceIn(0, 30),
                reminderTime = reminderTime,
                repeat = repeat,
                enabled = true,
                notes = notes?.takeIf { it.isNotBlank() },
                triggeredAt = null,
                createdAt = now,
                updatedAt = now,
                isLunar = isLunar
            )
            reminderRepo.upsert(reminder)
            ReminderCalc.nextTriggerMillis(reminder)?.let {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, it)
            } ?: ReminderScheduler.cancel(appContext, reminder.id)
        }
    }

    fun toggleReminder(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepo.setEnabled(reminderId, enabled, System.currentTimeMillis())
            val reminder = reminderRepo.getById(reminderId) ?: return@launch
            if (enabled) {
                ReminderCalc.nextTriggerMillis(reminder)?.let {
                    ReminderScheduler.scheduleReminder(appContext, reminderId, it)
                }
            } else {
                ReminderScheduler.cancel(appContext, reminderId)
            }
        }
    }

    /** 删除提醒：软删进回收站，同时取消调度 */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepo.softDelete(reminder.id, System.currentTimeMillis())
        }
    }
}

sealed interface PersonDetailUiState {
    data object Loading : PersonDetailUiState
    data object NotFound : PersonDetailUiState
    data class Loaded(val person: Person, val reminders: List<Reminder>) : PersonDetailUiState
}
