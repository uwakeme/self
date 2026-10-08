package com.self.profile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.util.ReminderCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 首页 ViewModel。汇总问候语所需的数据：
 * - 自身（昵称、头像）
 * - 即将到来提醒（前 3 条）
 * - 关系概览（按类型计数 + 头像列表）
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val personRepo: PersonRepository,
    reminderRepo: ReminderRepository,
    settingsStore: SettingsDataStore
) : ViewModel() {

    /**
     * 每天 0 点发射一次新日期。app 跨午夜存活时"30 天内 / 本月"窗口
     * 不能继续用 VM 构造时缓存的旧日期。
     */
    private val todayFlow = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val nextMidnight = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        personRepo.observeSelf(),
        personRepo.observeAll(),
        reminderRepo.observeEnabled(),
        settingsStore.settings,
        todayFlow
    ) { self, allPeople, enabledReminders, settings, today ->
        val others = allPeople.filter { !it.isSelf }
        val personById = allPeople.associateBy { it.id }
        // 近期 = 下一次正日在未来 30 天内（农历 / 跨年提醒按重算日期过滤）
        val upcoming = enabledReminders
            .map { r -> r to ReminderCalc.nextOccurrenceDate(r, today) }
            .filter { (_, d) -> d.toEpochDay() in today.toEpochDay()..(today.toEpochDay() + 30) }
            .sortedBy { (_, d) -> d.toEpochDay() }
            .map { (r, _) -> r }
        // 本月重要日子：下一次正日落在本月的提醒
        val monthEvents = enabledReminders
            .map { r ->
                MonthEvent(
                    reminder = r,
                    personName = personById[r.personId]?.name,
                    date = ReminderCalc.nextOccurrenceDate(r, today)
                )
            }
            .filter { it.date.year == today.year && it.date.monthValue == today.monthValue }
            .sortedBy { it.date.dayOfMonth }
        HomeUiState(
            nickname = settings.userNickname.takeIf { it.isNotBlank() } ?: "用户",
            avatarUrl = settings.userAvatar,
            fallbackColor = self?.avatarColor ?: "#C4785B",
            fallbackInitial = self?.avatarInitial ?: "我",
            upcomingReminders = upcoming.take(3),
            others = others,
            relationshipCounts = RelationshipCounts(
                family = others.count { it.relationshipType.label == "家人" },
                friend = others.count { it.relationshipType.label == "朋友" },
                colleague = others.count { it.relationshipType.label == "同事" },
                classmate = others.count { it.relationshipType.label == "同学" }
            ),
            showAllCount = others.size,
            monthEvents = monthEvents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.empty()
    )
}

data class HomeUiState(
    val nickname: String,
    val avatarUrl: String?,
    val fallbackColor: String,
    val fallbackInitial: String,
    val upcomingReminders: List<Reminder>,
    val others: List<Person>,
    val relationshipCounts: RelationshipCounts,
    val showAllCount: Int,
    val monthEvents: List<MonthEvent> = emptyList()
) {
    companion object {
        fun empty() = HomeUiState(
            nickname = "用户",
            avatarUrl = null,
            fallbackColor = "#C4785B",
            fallbackInitial = "我",
            upcomingReminders = emptyList(),
            others = emptyList(),
            relationshipCounts = RelationshipCounts(0, 0, 0, 0),
            showAllCount = 0,
            monthEvents = emptyList()
        )
    }
}

/** 本月重要日子：提醒 + 下一次正日（农历提醒已换算公历） */
data class MonthEvent(
    val reminder: Reminder,
    val personName: String?,
    val date: LocalDate
)

data class RelationshipCounts(
    val family: Int,
    val friend: Int,
    val colleague: Int,
    val classmate: Int
) {
    val total: Int get() = family + friend + colleague + classmate
}
