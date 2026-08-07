package com.self.profile.ui.reminder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
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
 * 提醒中心 ViewModel。
 *
 * 数据组合：
 * - reminderRepo.observeAll() + personRepo.observeAll() → 拼出每条 reminder 对应的人物信息
 * - 派生统计：已设置总数、已开启、7 天内即将到来
 * - 当前 Tab 过滤后的列表
 */
@HiltViewModel
class ReminderCenterViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val personRepo: PersonRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _typeFilter = MutableStateFlow(ReminderTab.ALL)
    val typeFilter: StateFlow<ReminderTab> = _typeFilter.asStateFlow()

    val state: StateFlow<ReminderCenterUiState> = combine(
        reminderRepo.observeAll(),
        personRepo.observeAll(),
        _typeFilter
    ) { reminders, people, tab ->
        val byId = people.associateBy { it.id }
        val today = LocalDate.now()
        val sevenDaysLater = today.plusDays(7)

        val upcoming = reminders
            .filter { it.enabled && LocalDate.ofEpochDay(it.date) in today..sevenDaysLater }
            .sortedBy { it.date }
            .take(5)

        val filtered = when (tab) {
            ReminderTab.ALL -> reminders
            ReminderTab.BIRTHDAY -> reminders.filter { it.type == ReminderType.BIRTHDAY }
            ReminderTab.ANNIVERSARY -> reminders.filter { it.type == ReminderType.ANNIVERSARY }
            ReminderTab.CUSTOM -> reminders.filter { it.type == ReminderType.CUSTOM }
            ReminderTab.DISABLED -> reminders.filter { !it.enabled }
        }.sortedBy { it.date }

        ReminderCenterUiState(
            all = reminders,
            upcoming = upcoming,
            filtered = filtered,
            peopleById = byId,
            totalCount = reminders.size,
            enabledCount = reminders.count { it.enabled },
            upcomingCount = upcoming.size,
            currentTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderCenterUiState()
    )

    fun setTab(tab: ReminderTab) {
        _typeFilter.value = tab
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
            // 立即调度
            val trigger = ReminderCalc.nextTriggerMillis(reminder)
            if (trigger != null) {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, trigger)
            }
        }
    }

    fun toggleEnabled(reminder: Reminder) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newEnabled = !reminder.enabled
            reminderRepo.setEnabled(reminder.id, newEnabled, now)
            if (newEnabled) {
                val updated = reminder.copy(enabled = true, updatedAt = now)
                val trigger = ReminderCalc.nextTriggerMillis(updated)
                if (trigger != null) {
                    ReminderScheduler.scheduleReminder(appContext, reminder.id, trigger)
                }
            } else {
                ReminderScheduler.cancel(appContext, reminder.id)
            }
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepo.delete(reminder.id)
        }
    }
}

/**
 * 5 个 Tab：全部 / 生日 / 纪念日 / 自定义 / 已关闭
 */
enum class ReminderTab(val label: String) {
    ALL("全部"),
    BIRTHDAY("生日"),
    ANNIVERSARY("纪念日"),
    CUSTOM("自定义"),
    DISABLED("已关闭");
}

data class ReminderCenterUiState(
    val all: List<Reminder> = emptyList(),
    val upcoming: List<Reminder> = emptyList(),
    val filtered: List<Reminder> = emptyList(),
    val peopleById: Map<String, Person> = emptyMap(),
    val totalCount: Int = 0,
    val enabledCount: Int = 0,
    val upcomingCount: Int = 0,
    val currentTab: ReminderTab = ReminderTab.ALL
)
