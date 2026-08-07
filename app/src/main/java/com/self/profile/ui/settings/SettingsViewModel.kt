package com.self.profile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.backup.DataExporter
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.DarkModePref
import com.self.profile.domain.model.FontSizePref
import com.self.profile.domain.model.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 设置页 / 提醒中心 ViewModel。
 *
 * 阶段 1：渲染静态区块（4 个）+ 提醒列表（只读）
 * 阶段 5/6：编辑、删除、切换开关、深色模式切换、字体大小、数据导出
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val personRepo: PersonRepository,
    private val settingsStore: SettingsDataStore,
    private val appLockManager: AppLockManager,
    private val dataExporter: DataExporter
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val today = LocalDate.now().toEpochDay()

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            reminderRepo.observeAll(),
            reminderRepo.observeCount(),
            reminderRepo.observeEnabledCount(),
            reminderRepo.observeUpcoming(today, today + 7)
        ) { reminders, total, enabledCount, upcoming -> Quad(reminders, total, enabledCount, upcoming) },
        personRepo.observeAll(),
        settingsStore.settings
    ) { quad, allPeople, settings ->
        val personById = allPeople.associateBy { it.id }
        SettingsUiState(
            settings = settings,
            total = quad.total,
            enabledCount = quad.enabledCount,
            upcomingCount = quad.upcoming.size,
            upcoming = quad.upcoming.mapNotNull { r ->
                personById[r.personId]?.let { p -> UpcomingReminder(r, p.avatarInitial, p.avatarColor, p.name) }
            },
            all = quad.reminders.mapNotNull { r ->
                val person = personById[r.personId]
                AllReminder(
                    reminder = r,
                    personInitial = person?.avatarInitial ?: "·",
                    personColor = person?.avatarColor ?: "#C4785B",
                    personName = person?.name ?: "自定义"
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.empty()
    )

    fun toggleEnabled(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepo.setEnabled(reminderId, enabled, System.currentTimeMillis())
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

data class SettingsUiState(
    val settings: AppSettings,
    val total: Int,
    val enabledCount: Int,
    val upcomingCount: Int,
    val upcoming: List<UpcomingReminder>,
    val all: List<AllReminder>
) {
    companion object {
        fun empty() = SettingsUiState(
            settings = AppSettings(),
            total = 0, enabledCount = 0, upcomingCount = 0,
            upcoming = emptyList(),
            all = emptyList()
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
