package com.self.profile.ui.self

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.R
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
import com.self.profile.domain.model.RelationshipType
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.domain.model.SelfModule
import com.self.profile.domain.model.WishItem
import com.self.profile.util.PhotoStorage
import com.self.profile.util.ReminderCalc
import com.self.profile.work.ReminderScheduler
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
     * [from] / [to] 是「实际渲染顺序」（[SelfModule.orderedFrom] 归一化后）里的索引，
     * 与屏幕上看到的模块一一对应；持久化时统一走 [SelfModule.orderOf]。
     */
    fun moveModule(from: Int, to: Int) {
        val current = (state.value as? SelfDetailUiState.Loaded)?.moduleOrder ?: return
        val ordered = SelfModule.orderedFrom(current).toMutableList()
        if (from == to || from !in ordered.indices || to !in ordered.indices) return
        ordered.add(to, ordered.removeAt(from))
        viewModelScope.launch {
            settingsStore.setSelfProfileModuleOrder(SelfModule.orderOf(ordered))
        }
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
     * 串行化读-改-写：快速连点（勾两个愿望、连加两条笔记）时，
     * 每次操作都在锁内重读 DB 最新值再 transform，避免旧快照整行覆盖丢掉前一次修改。
     */
    private val writeMutex = Mutex()

    // ====== 自我档案创建 ======

    /**
     * 空态下创建一份空白自我档案。
     *
     * self 行原本只由种子数据或备份导入产生，全仓没有第二个入口。禁止重新播种之后，
     * 用户执行过「清除全部数据」就只能靠导入备份找回档案，没导出过的用户无路可走，
     * 所以补上这个入口。
     */
    fun createSelf() {
        viewModelScope.launch { createSelfIfAbsent() }
    }

    /**
     * [createSelf] 的逻辑本体，单独拆出便于单测（外层要走 viewModelScope，需要 Main dispatcher）。
     *
     * 写入走同一把 [writeMutex] 并在锁内重读库：空态按钮连点两次、或空态期间导入了备份，
     * 都不会写出第二行 is_self = 1。
     */
    internal suspend fun createSelfIfAbsent() {
        writeMutex.withLock {
            if (personRepo.observeSelf().first() != null) return@withLock
            val now = System.currentTimeMillis()
            // 姓名沿用设置页里的昵称；昵称被清成空白时兜底成「我」，不要给档案留个空名字
            val name = settingsStore.settings.first().userNickname
                .ifBlank { appContext.getString(R.string.self_default_name) }
            personRepo.upsert(
                Person(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    avatarColor = "#C4785B",
                    avatarInitial = name.take(1),
                    relationshipType = RelationshipType.FRIEND,  // isSelf 时无所谓
                    isSelf = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            // 既然已经有数据了，把「已初始化」开关一并置上，seed 判据始终与库状态自洽
            settingsStore.setUserInitialized(true)
        }
    }

    /**
     * 通用更新：锁内读库最新 self → transform → 持久化（自动更新 updatedAt）。
     *
     * transform 返回 null 表示跳过本次写入（如校验不通过）。
     */
    fun updatePerson(transform: (Person) -> Person?) {
        viewModelScope.launch {
            writeMutex.withLock {
                val latest = personRepo.observeSelf().first() ?: return@withLock
                val updated = transform(latest)?.copy(
                    updatedAt = System.currentTimeMillis()
                ) ?: return@withLock
                personRepo.upsert(updated)
            }
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

    /** 先从 DB 摘除记录，落库成功后再删文件（文件删了就找不回，顺序不能反） */
    fun removePhoto(photoId: String) {
        viewModelScope.launch {
            var removed: Photo? = null
            writeMutex.withLock {
                val latest = personRepo.observeSelf().first() ?: return@withLock
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
        updatePerson { p ->
            val source = if (isLiked) p.thingsLiked else p.thingsDisliked
            if (source.any { it.category == category }) null
            else if (isLiked) p.copy(thingsLiked = source + CategorizedItems(category, emptyList()))
            else p.copy(thingsDisliked = source + CategorizedItems(category, emptyList()))
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
            // 首次触发统一走 ReminderCalc（农历 / 重复规则 / 防连环触发一份逻辑）；
            // 一次性提醒已过期（无下次触发）则建成即关闭，与冷启动重注册语义一致
            val next = ReminderCalc.nextTriggerMillis(reminder)
            val toSave = if (next == null) reminder.copy(enabled = false) else reminder
            reminderRepo.upsert(toSave)
            if (next != null) {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, next)
            } else {
                ReminderScheduler.cancel(appContext, reminder.id)
            }
        }
    }
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
