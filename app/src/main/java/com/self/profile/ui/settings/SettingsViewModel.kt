package com.self.profile.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.backup.ClearSummary
import com.self.profile.data.backup.DataExporter
import com.self.profile.data.backup.ExportSummary
import com.self.profile.data.backup.ImportResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 设置页 / 提醒中心 ViewModel（US-012 / US-013 / US-014 / US-015 / US-018）。
 *
 * 提醒中心已合并到本屏：统计 + 即将到来 + Tab 过滤 + 全部列表 + 添加/编辑/删除 都在 [SettingsScreen] 里。
 * 原独立的 ReminderCenterScreen 已在 Phase 3.1 移除。
 */

/**
 * [SettingsViewModel.todayFlow] 单次等待的上限。
 *
 * delay 走的是单调时钟（深睡 / 手动改系统时间都不会让挂起的 delay 提前到期），
 * 封顶后最长半小时一定会重新读一次 [LocalDate.now]。
 */
private const val TODAY_FLOW_MAX_DELAY_MS = 30 * 60 * 1000L

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

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /** SAF 选好待导入的文件，等用户二次确认覆盖 */
    private val _pendingImportUri = MutableStateFlow<Uri?>(null)
    val pendingImportUri: StateFlow<Uri?> = _pendingImportUri.asStateFlow()

    private val _clearState = MutableStateFlow<ClearState>(ClearState.Idle)
    val clearState: StateFlow<ClearState> = _clearState.asStateFlow()

    private val _typeFilter = MutableStateFlow(ReminderTab.ALL)
    val typeFilter: StateFlow<ReminderTab> = _typeFilter.asStateFlow()

    /** 每天 0 点发射一次新日期：跨午夜存活时"7 天内"窗口不能用 VM 构造时的旧日期 */
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

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            reminderRepo.observeAll(),
            reminderRepo.observeCount(),
            reminderRepo.observeEnabledCount()
        ) { reminders, total, enabledCount -> Triple(reminders, total, enabledCount) },
        personRepo.observeAll(),
        settingsStore.settings,
        _typeFilter,
        todayFlow
    ) { triple, allPeople, settings, tab, today ->
        val (reminders, total, enabledCount) = triple
        val personById = allPeople.associateBy { it.id }
        val todayEpochDay = today.toEpochDay()
        // 倒计时用"下一次正日"：农历 / 跨年的重复提醒 reminder.date 可能已过期
        val upcoming = reminders.filter { it.enabled }
            .map { r -> r to ReminderCalc.nextOccurrenceDate(r) }
            .filter { (_, d) -> d.toEpochDay() in todayEpochDay..(todayEpochDay + 7) }
            .sortedBy { (_, d) -> d.toEpochDay() }
            .mapNotNull { (r, d) ->
                personById[r.personId]?.let { p ->
                    UpcomingReminder(r, p.avatarInitial, p.avatarColor, p.name, d)
                }
            }
        val all = reminders.mapNotNull { r ->
            val person = personById[r.personId]
            AllReminder(
                reminder = r,
                personInitial = person?.avatarInitial ?: "·",
                personColor = person?.avatarColor ?: "#C4785B",
                personName = person?.name ?: "自定义",
                nextDate = ReminderCalc.nextOccurrenceDate(r)
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
            total = total,
            enabledCount = enabledCount,
            upcomingCount = upcoming.size,
            upcoming = upcoming,
            all = all,
            filtered = filtered,
            peopleById = personById,
            currentTab = tab,
            today = today
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
            val now = System.currentTimeMillis()
            reminderRepo.setEnabled(reminderId, enabled, now)
            if (!enabled) {
                ReminderScheduler.cancel(appContext, reminderId)
                return@launch
            }
            // 开启时要重新挂上 Worker（否则到点不响，只能等下次冷启动重注册）；
            // 一次性提醒已过期（无下次触发）直接关掉，不留永不触发的僵尸
            val reminder = reminderRepo.getById(reminderId) ?: return@launch
            val next = ReminderCalc.nextTriggerMillis(reminder)
            if (next != null) {
                ReminderScheduler.scheduleReminder(appContext, reminderId, next)
            } else {
                reminderRepo.setEnabled(reminderId, false, now)
            }
        }
    }

    /**
     * 新增或更新一条 reminder。保存后立即按 next trigger 调度。
     * isLunar=true 时（仅 YEARLY 生效）调度按农历逐年换算公历。
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
        notes: String?,
        isLunar: Boolean
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
                updatedAt = now,
                isLunar = isLunar
            )
            reminderRepo.upsert(reminder)
            val trigger = ReminderCalc.nextTriggerMillis(reminder)
            if (trigger != null) {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, trigger)
            } else {
                ReminderScheduler.cancel(appContext, reminder.id)
            }
        }
    }

    /** 删除提醒：进回收站（软删），同时取消已挂的调度 */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepo.softDelete(reminder.id, System.currentTimeMillis())
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

    // ====== 数据导出 / 导入 / 清除 ======

    /**
     * 导出 zip 备份（含照片）到 SAF 选定的位置。完成后 [exportState] 变 Success/Failure。
     */
    fun exportData(uri: Uri) {
        if (_exportState.value is ExportState.Running) return  // 防重入
        viewModelScope.launch {
            _exportState.value = ExportState.Running
            try {
                val summary = dataExporter.exportToUri(uri)
                _exportState.value = ExportState.Success(summary)
            } catch (e: Exception) {
                _exportState.value = ExportState.Failure(e.message ?: "未知错误")
            }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    /** SAF 选好待导入文件 → 弹覆盖确认 */
    fun onImportFilePicked(uri: Uri) {
        _pendingImportUri.value = uri
    }

    fun cancelImportConfirm() {
        _pendingImportUri.value = null
    }

    /**
     * 确认导入：恢复前自动对当前数据做安全备份（cache），成功后重挂提醒调度。
     */
    fun confirmImport(uri: Uri) {
        if (_importState.value is ImportState.Running) return  // 防重入
        _pendingImportUri.value = null
        viewModelScope.launch {
            _importState.value = ImportState.Running
            try {
                val result = dataExporter.importFromUri(uri)
                _importState.value = ImportState.Success(result)
            } catch (e: Exception) {
                _importState.value = ImportState.Failure(e.message ?: "未知错误")
            }
        }
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }

    /**
     * 触发清空所有用户数据。完成后 [clearState] 会变成 Success/Failure。
     *
     * 调用方在收到 Success 后应当再调 [clearClearState] 把状态重置回 Idle，
     * 否则 UI 会一直显示结果对话框。
     */
    fun clearAllData() {
        if (_clearState.value is ClearState.Running) return  // 防重入
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
    data class Success(val summary: ExportSummary) : ExportState
    data class Failure(val error: String) : ExportState
}

/**
 * 导入状态机。
 */
sealed interface ImportState {
    data object Idle : ImportState
    data object Running : ImportState
    data class Success(val result: ImportResult) : ImportState
    data class Failure(val error: String) : ImportState
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
    val currentTab: ReminderTab,
    /**
     * 当前「今天」，由 [SettingsViewModel.todayFlow] 跨零点刷新。
     *
     * 必须留在 state 里：行组件的倒计时靠它取"今天"，而不是在组合期自己读系统时间
     * （组合期读到的值不会因为跨零点而变化，且数据没变时 state 结构相等 → 不重组）。
     */
    val today: LocalDate
) {
    companion object {
        fun empty() = SettingsUiState(
            settings = AppSettings(),
            total = 0, enabledCount = 0, upcomingCount = 0,
            upcoming = emptyList(),
            all = emptyList(),
            filtered = emptyList(),
            peopleById = emptyMap(),
            currentTab = ReminderTab.ALL,
            today = LocalDate.now()
        )
    }
}

data class UpcomingReminder(
    val reminder: Reminder,
    val personInitial: String,
    val personColor: String,
    val personName: String,
    val nextDate: LocalDate
)

data class AllReminder(
    val reminder: Reminder,
    val personInitial: String,
    val personColor: String,
    val personName: String,
    val nextDate: LocalDate
)
