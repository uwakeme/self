package com.self.profile.util

import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.RepeatRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 提醒时间计算工具。
 *
 * 单一来源：ReminderWorker 触发后算下次触发、SettingsViewModel 保存/恢复时算下次触发，
 * 都走这里。农历提醒（isLunar）按农历月日换算当年公历，每年自动跟随漂移。
 */
object ReminderCalc {

    /**
     * 计算下一次触发的 epoch millis（已含提前量），找不到返回 null。
     *
     * 修复：提前量窗口内 Worker 被重复触发时，旧的实现会用一个已过去的时刻重排任务，
     * 导致 0 延迟连环触发刷通知；现在保证返回值严格晚于 now。
     */
    fun nextTriggerMillis(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): Long? {
        val baseTime = parseTimeOrDefault(reminder.reminderTime)
        val zone = ZoneId.systemDefault()

        if (reminder.repeat == RepeatRule.NONE) {
            val trigger = LocalDate.ofEpochDay(reminder.date).atTime(baseTime)
                .minusDays(reminder.advanceDays.toLong())
            return if (trigger.isAfter(now)) {
                trigger.atZone(zone).toInstant().toEpochMilli()
            } else null
        }

        var occurrence = nextOccurrenceDate(reminder, now.toLocalDate())
        var trigger = occurrence.atTime(baseTime).minusDays(reminder.advanceDays.toLong())
        var guard = 0
        while (!trigger.isAfter(now) && guard++ < 400) {
            occurrence = nextOccurrenceStrict(reminder, occurrence)
            trigger = occurrence.atTime(baseTime).minusDays(reminder.advanceDays.toLong())
        }
        return trigger.atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * 下一次「正日」日期（含今天，不含提前量）。UI 倒计时 / 即将到来过滤用这个，
     * 不要直接用 reminder.date —— 农历与跨年的重复提醒 date 字段可能已过期。
     * NONE 重复返回原始日期（可能已过去）。
     */
    fun nextOccurrenceDate(reminder: Reminder, today: LocalDate = LocalDate.now()): LocalDate {
        if (reminder.repeat == RepeatRule.NONE) return LocalDate.ofEpochDay(reminder.date)
        val stored = LocalDate.ofEpochDay(reminder.date)
        return when (reminder.repeat) {
            RepeatRule.YEARLY -> {
                if (reminder.isLunar) {
                    val (m, d) = LunarHelper.solarToLunarMonthDay(stored)
                    LunarHelper.nextLunarDateOnOrAfter(m, d, today)
                } else {
                    val thisYear = today.withMonth(stored.monthValue).withDayOfMonth(stored.dayOfMonth)
                    if (!thisYear.isBefore(today)) thisYear else thisYear.plusYears(1)
                }
            }
            RepeatRule.MONTHLY -> {
                val thisMonth = today.withDayOfMonth(stored.dayOfMonth.coerceAtMost(today.lengthOfMonth()))
                if (!thisMonth.isBefore(today)) thisMonth else thisMonth.plusMonths(1)
            }
            RepeatRule.WEEKLY -> {
                val daysUntil = (stored.dayOfWeek.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(daysUntil.toLong())
            }
            RepeatRule.NONE -> stored
        }
    }

    /** 严格晚于 [current] 的下一个正日 */
    private fun nextOccurrenceStrict(reminder: Reminder, current: LocalDate): LocalDate =
        when (reminder.repeat) {
            RepeatRule.YEARLY -> {
                if (reminder.isLunar) {
                    val (m, d) = LunarHelper.solarToLunarMonthDay(LocalDate.ofEpochDay(reminder.date))
                    LunarHelper.nextLunarDateOnOrAfter(m, d, current.plusDays(1))
                } else current.plusYears(1)
            }
            RepeatRule.MONTHLY -> current.plusMonths(1)
            RepeatRule.WEEKLY -> current.plusWeeks(1)
            RepeatRule.NONE -> current
        }

    private fun parseTimeOrDefault(time: String): LocalTime = try {
        val parts = time.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        LocalTime.of(8, 0)
    }
}
