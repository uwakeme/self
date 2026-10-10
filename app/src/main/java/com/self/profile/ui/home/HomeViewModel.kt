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
 * [todayFlow] 单次等待的上限。
 *
 * delay 走的是单调时钟（设备深睡 / 手动改系统时间都不会让挂起的 delay 提前到期），
 * 所以不能完全信任「睡到明天 0 点」。封顶后最长半小时一定会重新读一次 [LocalDate.now]，
 * 用户手动把系统时间调到次日时倒计时也能跟上。
 */
private const val TODAY_FLOW_MAX_DELAY_MS = 30 * 60 * 1000L

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
     *
     * 该日期必须进 [HomeUiState]（见 `today` 字段）：否则跨零点时重算出来的
     * state 与旧 state 结构相等，StateFlow 会直接 conflate 掉，组合层拿不到新值，
     * 倒计时就停在昨天。
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
            today = today,
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
    /**
     * 当前「今天」，由 [HomeViewModel.todayFlow] 跨零点刷新。
     *
     * 必须留在 state 里：行组件的倒计时靠它取"今天"，而不是在组合期自己读系统时间
     * （组合期读到的值不会因为跨零点而变化，且数据没变时 state 结构相等 → 不重组）。
     */
    val today: LocalDate,
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
            today = LocalDate.now(),
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
