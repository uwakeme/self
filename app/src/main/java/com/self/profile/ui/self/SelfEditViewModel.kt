package com.self.profile.ui.self

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Anniversary
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Photo
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.domain.model.SelfModule
import com.self.profile.domain.model.WishItem
import com.self.profile.util.PhotoStorage
import com.self.profile.work.ReminderScheduler
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 自我档案 ViewModel（读 + 写）。
 *
 * 读：[state] 暴露 SelfDetailUiState（self + reminders）
 * 写：[updatePerson] 接收 transform lambda，构造新 Person 持久化
 */
@HiltViewModel
class SelfEditViewModel @Inject constructor(
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val settingsStore: SettingsDataStore,
    private val photoStorage: PhotoStorage,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val state: StateFlow<SelfDetailUiState> = combine(
        personRepo.observeSelf(),
        reminderRepo.observeAll(),
        settingsStore.settings
    ) { self, reminders, settings ->
        if (self == null) SelfDetailUiState.Empty
        else SelfDetailUiState.Loaded(
            self = self,
            reminders = reminders.filter { it.personId == null || it.personId == self.id },
            moduleOrder = settings.selfProfileModuleOrder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SelfDetailUiState.Loading
    )

    /**
     * 上移/下移某个模块（US-016）。
     *
     * [from] / [to] 是 moduleOrder 当前列表里的索引。
     */
    fun moveModule(from: Int, to: Int) {
        val current = (state.value as? SelfDetailUiState.Loaded)?.moduleOrder ?: return
        if (from == to || from !in current.indices || to !in current.indices) return
        val newOrder = current.toMutableList().apply {
            add(to, removeAt(from))
        }
        viewModelScope.launch { settingsStore.setSelfProfileModuleOrder(newOrder) }
    }

    /**
     * 把模块顺序重置回默认。
     */
    fun resetModuleOrder() {
        viewModelScope.launch {
            settingsStore.setSelfProfileModuleOrder(SelfModule.defaultOrder)
        }
    }

    /**
     * 通用更新：transform 现有 self，生成新 Person，自动更新 updatedAt，持久化。
     *
     * 不用 inline（inline 不能访问 private 属性）。需要"条件性跳过更新"
     * 的场景在调用方自己提早 return。
     */
    fun updatePerson(transform: (Person) -> Person) {
        val current = state.value
        if (current !is SelfDetailUiState.Loaded) return
        viewModelScope.launch {
            val updated = transform(current.self).copy(
                updatedAt = System.currentTimeMillis()
            )
            personRepo.upsert(updated)
        }
    }

    // ====== 列表项操作快捷方法 ======

    fun addWishItem(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(
                wishList = p.wishList + WishItem(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    completed = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleWishItem(id: String) {
        updatePerson { p ->
            p.copy(
                wishList = p.wishList.map { if (it.id == id) it.copy(completed = !it.completed) else it }
            )
        }
    }

    fun removeWishItem(id: String) {
        updatePerson { p -> p.copy(wishList = p.wishList.filter { it.id != id }) }
    }

    fun addAspiration(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(
                aspirations = p.aspirations + Aspiration(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeAspiration(id: String) {
        updatePerson { p -> p.copy(aspirations = p.aspirations.filter { it.id != id }) }
    }

    fun addNote(content: String) {
        if (content.isBlank()) return
        updatePerson { p ->
            p.copy(
                thoughtNotes = p.thoughtNotes + Note(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    date = System.currentTimeMillis() / 86_400_000L  // epoch day
                )
            )
        }
    }

    fun removeNote(id: String) {
        updatePerson { p -> p.copy(thoughtNotes = p.thoughtNotes.filter { it.id != id }) }
    }

    fun addLifeJourney(year: String, title: String, type: String, description: String) {
        if (title.isBlank()) return
        updatePerson { p ->
            p.copy(
                lifeJourney = p.lifeJourney + LifeJourneyEntry(
                    id = UUID.randomUUID().toString(),
                    year = year,
                    title = title,
                    type = type,
                    description = description
                )
            )
        }
    }

    fun removeLifeJourney(id: String) {
        updatePerson { p -> p.copy(lifeJourney = p.lifeJourney.filter { it.id != id }) }
    }

    fun addAnniversary(title: String, date: Long, type: String) {
        if (title.isBlank()) return
        updatePerson { p ->
            p.copy(
                anniversaries = p.anniversaries + Anniversary(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    date = date,
                    type = type
                )
            )
        }
    }

    fun removeAnniversary(id: String) {
        updatePerson { p -> p.copy(anniversaries = p.anniversaries.filter { it.id != id }) }
    }

    // ====== 照片墙 ======

    /**
     * 从相册选择返回的 URI 复制到 app 私有目录并加入 photos 列表。
     * 复制失败（被系统拒绝 / 编码异常）静默跳过。
     */
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
        val current = (state.value as? SelfDetailUiState.Loaded)?.self ?: return
        val photo = current.photos.firstOrNull { it.id == photoId } ?: return
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

    // ====== 喜欢/不喜欢 类别操作 ======

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
        val current = state.value as? SelfDetailUiState.Loaded ?: return
        val source = if (isLiked) current.self.thingsLiked else current.self.thingsDisliked
        if (source.any { it.category == category }) return
        val updated = if (isLiked) {
            current.self.copy(thingsLiked = source + CategorizedItems(category, emptyList()))
        } else {
            current.self.copy(thingsDisliked = source + CategorizedItems(category, emptyList()))
        }
        viewModelScope.launch {
            personRepo.upsert(updated.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * 为指定纪念日添加一条提醒。Reminder.personId = self.id。
     */
    fun addReminderForAnniversary(
        anniversaryId: String,
        advanceDays: Int,
        reminderTime: String,
        repeat: RepeatRule,
        notes: String
    ) {
        val current = state.value as? SelfDetailUiState.Loaded ?: return
        val ann = current.self.anniversaries.firstOrNull { it.id == anniversaryId } ?: return
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            id = java.util.UUID.randomUUID().toString(),
            personId = current.self.id,
            type = ReminderType.fromLabel(ann.type),
            title = ann.title,
            date = ann.date,
            advanceDays = advanceDays,
            reminderTime = reminderTime,
            repeat = repeat,
            enabled = true,
            notes = notes.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            reminderRepo.upsert(reminder)
            // 调度首次触发
            ReminderScheduler.scheduleReminder(
                appContext,
                reminder.id,
                nextTriggerMillisFor(reminder)
            )
        }
    }
}

/**
 * 计算 reminder 首次触发的 epoch millis。
 */
private fun nextTriggerMillisFor(reminder: Reminder): Long {
    val now = java.time.LocalDateTime.now()
    val baseTime = java.time.LocalTime.of(
        reminder.reminderTime.substringBefore(":").toIntOrNull() ?: 8,
        reminder.reminderTime.substringAfter(":").toIntOrNull() ?: 0
    )
    val triggerDate = java.time.LocalDate.ofEpochDay(reminder.date)
    val triggerDateTime = java.time.LocalDateTime.of(triggerDate, baseTime)
    val advanceLocal = triggerDateTime.minusDays(reminder.advanceDays.toLong())
    val target = if (advanceLocal.isAfter(now)) advanceLocal else advanceLocal.plusYears(1)
    return target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}

sealed interface SelfDetailUiState {
    data object Loading : SelfDetailUiState
    data object Empty : SelfDetailUiState
    data class Loaded(
        val self: Person,
        val reminders: List<Reminder>,
        val moduleOrder: List<Int>
    ) : SelfDetailUiState
}
