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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 每天 0 点发射一次新日期。
 *
 * 单次 delay 封顶 [TODAY_FLOW_MAX_DELAY_MS]：delay 走单调时钟（深睡 / 手动改系统时间
 * 都不会让挂起的 delay 提前到期），封顶后最长半小时一定重读一次 [LocalDate.now]。
 */
private const val TODAY_FLOW_MAX_DELAY_MS = 30 * 60 * 1000L

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

    /**
     * 跨零点刷新的「今天」。必须进 [PersonDetailUiState.Loaded]，否则跨零点时重算出的
     * state 与旧 state 结构相等、StateFlow 直接 conflate 掉，相关提醒的倒计时会停在昨天。
     */
    private val todayFlow = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val nextMidnight = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            delay(
                (nextMidnight - System.currentTimeMillis())
                    .coerceIn(1_000L, TODAY_FLOW_MAX_DELAY_MS)
            )
        }
    }

    val state: StateFlow<PersonDetailUiState> = combine(
        personRepo.observeById(personId),
        reminderRepo.observeByPerson(personId),
        todayFlow
    ) { person, reminders, today ->
        if (person == null) PersonDetailUiState.NotFound
        else PersonDetailUiState.Loaded(person, reminders, today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonDetailUiState.Loading
    )

    /**
     * 串行化读-改-写：快速连续操作（勾选、添加）时每次都在锁内重读 DB 最新值，
     * 避免旧快照整行覆盖丢掉前一次修改。
     */
    private val writeMutex = Mutex()

    /**
     * 通用更新：锁内读库最新 person → transform → 持久化（自动更新 updatedAt）。
     *
     * transform 返回 null 表示跳过本次写入（如校验不通过）。
     */
    fun updatePerson(transform: (Person) -> Person?) {
        viewModelScope.launch {
            writeMutex.withLock {
                val latest = personRepo.observeById(personId).first() ?: return@withLock
                val updated = transform(latest)?.copy(
                    updatedAt = System.currentTimeMillis()
                ) ?: return@withLock
                personRepo.upsert(updated)
            }
        }
    }

    /**
     * 进入详情页时更新 last_viewed_at（用于"最近查看"排序）。
     * 直接按 personId 写，不依赖 UI state（state 首帧还是 Loading 时也能写上）。
     */
    fun markViewed() {
        viewModelScope.launch {
            val person = personRepo.observeById(personId).first() ?: return@launch
            if (person.isSelf) return@launch
            personRepo.updateLastViewed(person.id, System.currentTimeMillis())
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
        updatePerson { p ->
            val source = if (isLiked) p.thingsLiked else p.thingsDisliked
            if (source.any { it.category == category }) null
            else if (isLiked) p.copy(thingsLiked = source + CategorizedItems(category, emptyList()))
            else p.copy(thingsDisliked = source + CategorizedItems(category, emptyList()))
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

    /** 先从 DB 摘除记录，落库成功后再删文件（文件删了就找不回，顺序不能反） */
    fun removePhoto(photoId: String) {
        viewModelScope.launch {
            var removed: Photo? = null
            writeMutex.withLock {
                val latest = personRepo.observeById(personId).first() ?: return@withLock
                val photo = latest.photos.firstOrNull { it.id == photoId } ?: return@withLock
                personRepo.upsert(
                    latest.copy(
                        photos = latest.photos.filter { it.id != photoId },
                        updatedAt = System.currentTimeMillis()
                    )
                )
                removed = photo
            }
            removed?.let { photoStorage.delete(it.url) }
        }
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
                } ?: reminderRepo.setEnabled(reminderId, false, System.currentTimeMillis())
            } else {
                ReminderScheduler.cancel(appContext, reminderId)
            }
        }
    }

    /**
     * 为该人物的指定纪念日添加一条提醒（生日/纪念日按正日 + 提前量调度）。
     */
    fun addReminderForAnniversary(
        anniversaryId: String,
        advanceDays: Int,
        reminderTime: String,
        repeat: RepeatRule,
        notes: String
    ) {
        viewModelScope.launch {
            val person = personRepo.observeById(personId).first() ?: return@launch
            val ann = person.anniversaries.firstOrNull { it.id == anniversaryId } ?: return@launch
            val now = System.currentTimeMillis()
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                personId = person.id,
                type = ReminderType.fromLabel(ann.type),
                title = ann.title,
                date = ann.date,
                advanceDays = advanceDays.coerceIn(0, 30),
                reminderTime = reminderTime,
                repeat = repeat,
                enabled = true,
                notes = notes.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )
            // 一次性提醒已过期（无下次触发）则建成即关闭，与冷启动重注册语义一致
            val next = ReminderCalc.nextTriggerMillis(reminder)
            reminderRepo.upsert(if (next == null) reminder.copy(enabled = false) else reminder)
            if (next != null) {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, next)
            } else {
                ReminderScheduler.cancel(appContext, reminder.id)
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

    /**
     * @param today 倒计时基准的「今天」，由 VM 的跨零点流刷新后透传给行组件。
     *   行组件不在组合期自己调 `LocalDate.now()`，否则跨零点不重组、倒计时停在昨天。
     */
    data class Loaded(
        val person: Person,
        val reminders: List<Reminder>,
        val today: LocalDate
    ) : PersonDetailUiState
}
