package com.self.profile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.backup.ClearSummary
import com.self.profile.data.backup.DataExporter
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.DarkModePref
import com.self.profile.domain.model.FontSizePref
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.util.ReminderCalc
import com.self.profile.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * 设置页 / 提醒中心 ViewModel（US-012 / US-013 / US-014 / US-015 / US-018）。
 *
 * 提醒中心已合并到本屏：统计 + 即将到来 + Tab 过滤 + 全部列表 + 添加/编辑/删除 都在 [SettingsScreen] 里。
 * 原独立的 ReminderCenterScreen 已在 Phase 3.1 移除。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val personRepo: PersonRepository,
    private val settingsStore: SettingsDataStore,
    private val appLockManager: AppLockManager,
    private val dataExporter: DataExporter,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _clearState = MutableStateFlow<ClearState>(ClearState.Idle)
    val clearState: StateFlow<ClearState> = _clearState.asStateFlow()

    private val _typeFilter = MutableStateFlow(ReminderTab.ALL)
    val typeFilter: StateFlow<ReminderTab> = _typeFilter.asStateFlow()

    private val today = LocalDate.now().toEpochDay()

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            reminderRepo.observeAll(),
            reminderRepo.observeCount(),
            reminderRepo.observeEnabledCount(),
            reminderRepo.observeUpcoming(today, today + 7)
        ) { reminders, total, enabledCount, upcoming -> Quad(reminders, total, enabledCount, upcoming) },
        personRepo.observeAll(),
        settingsStore.settings,
        _typeFilter
    ) { quad, allPeople, settings, tab ->
        val personById = allPeople.associateBy { it.id }
        val upcoming = quad.upcoming.mapNotNull { r ->
            personById[r.personId]?.let { p -> UpcomingReminder(r, p.avatarInitial, p.avatarColor, p.name) }
        }
        val all = quad.reminders.mapNotNull { r ->
            val person = personById[r.personId]
            AllReminder(
                reminder = r,
                personInitial = person?.avatarInitial ?: "·",
                personColor = person?.avatarColor ?: "#C4785B",
                personName = person?.name ?: "自定义"
            )
        }
        val filtered = all.filter { row ->
            when (tab) {
                ReminderTab.ALL -> true
                ReminderTab.BIRTHDAY -> row.reminder.type == ReminderType.BIRTHDAY
                ReminderTab.ANNIVERSARY -> row.reminder.type == ReminderType.ANNIVERSARY
                ReminderTab.CUSTOM -> row.reminder.type == ReminderType.CUSTOM
                ReminderTab.DISABLED -> !row.reminder.enabled
            }
        }
        SettingsUiState(
            settings = settings,
            total = quad.total,
            enabledCount = quad.enabledCount,
            upcomingCount = quad.upcoming.size,
            upcoming = upcoming,
            all = all,
            filtered = filtered,
            peopleById = personById,
            currentTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.empty()
    )

    // ====== 提醒操作 ======

    fun setTab(tab: ReminderTab) {
        _typeFilter.value = tab
    }

    fun toggleEnabled(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepo.setEnabled(reminderId, enabled, System.currentTimeMillis())
        }
    }

    /**
     * 新增或更新一条 reminder。保存后立即按 next trigger 调度。
     */
    fun saveReminder(
        existingId: String?,
        personId: String?,
        type: ReminderType,
        title: String,
        date: LocalDate,
        advanceDays: Int,
        reminderTime: String,
        repeat: RepeatRule,
        notes: String?
    ) {
        if (title.isBlank()) return
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
                updatedAt = now
            )
            reminderRepo.upsert(reminder)
            val trigger = ReminderCalc.nextTriggerMillis(reminder)
            if (trigger != null) {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, trigger)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepo.delete(reminder.id)
        }
    }

    // ====== 应用设置 setter（Stage 6 接线） ======

    fun setDarkMode(value: DarkModePref) {
        viewModelScope.launch { settingsStore.setDarkMode(value) }
    }

    fun setFontScale(value: FontSizePref) {
        viewModelScope.launch { settingsStore.setFontScale(value) }
    }

    fun setAppLockEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsStore.setAppLockEnabled(value)
            if (value) appLockManager.onLockEnabledChanged(true)
        }
    }

    fun setShowLunarBirthday(value: Boolean) {
        viewModelScope.launch { settingsStore.setShowLunarBirthday(value) }
    }

    // ====== 数据导出 / 清除 ======

    /**
     * 触发数据导出。完成后 [exportState] 会变成 Success/Failure。
     */
    fun exportData() {
        viewModelScope.launch {
            _exportState.value = ExportState.Running
            try {
                val path = dataExporter.exportToDownloads()
                _exportState.value = ExportState.Success(path)
            } catch (e: Exception) {
                _exportState.value = ExportState.Failure(e.message ?: "未知错误")
            }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    /**
     * 触发清空所有用户数据。完成后 [clearState] 会变成 Success/Failure。
     *
     * 调用方在收到 Success 后应当再调 [clearClearState] 把状态重置回 Idle，
     * 否则 UI 会一直显示结果对话框。
     */
    fun clearAllData() {
        viewModelScope.launch {
            _clearState.value = ClearState.Running
            try {
                val summary = dataExporter.clearAllData()
                _clearState.value = ClearState.Success(summary)
            } catch (e: Exception) {
                _clearState.value = ClearState.Failure(e.message ?: "未知错误")
            }
        }
    }

    fun clearClearState() {
        _clearState.value = ClearState.Idle
    }
}

/**
 * 导出状态机。
 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Success(val path: String) : ExportState
    data class Failure(val error: String) : ExportState
}

/**
 * 清除状态机。
 */
sealed interface ClearState {
    data object Idle : ClearState
    data object Running : ClearState
    data class Success(val summary: ClearSummary) : ClearState
    data class Failure(val error: String) : ClearState
}

/**
 * 提醒中心 5 个 Tab：全部 / 生日 / 纪念日 / 自定义 / 已关闭
 */
enum class ReminderTab(val label: String) {
    ALL("全部"),
    BIRTHDAY("生日"),
    ANNIVERSARY("纪念日"),
    CUSTOM("自定义"),
    DISABLED("已关闭");
}

data class SettingsUiState(
    val settings: AppSettings,
    val total: Int,
    val enabledCount: Int,
    val upcomingCount: Int,
    val upcoming: List<UpcomingReminder>,
    val all: List<AllReminder>,
    val filtered: List<AllReminder>,
    val peopleById: Map<String, Person>,
    val currentTab: ReminderTab
) {
    companion object {
        fun empty() = SettingsUiState(
            settings = AppSettings(),
            total = 0, enabledCount = 0, upcomingCount = 0,
            upcoming = emptyList(),
            all = emptyList(),
            filtered = emptyList(),
            peopleById = emptyMap(),
            currentTab = ReminderTab.ALL
        )
    }
}

data class UpcomingReminder(
    val reminder: Reminder,
    val personInitial: String,
    val personColor: String,
    val personName: String
)

data class AllReminder(
    val reminder: Reminder,
    val personInitial: String,
    val personColor: String,
    val personName: String
)

private data class Quad(
    val reminders: List<Reminder>,
    val total: Int,
    val enabledCount: Int,
    val upcoming: List<Reminder>
)
