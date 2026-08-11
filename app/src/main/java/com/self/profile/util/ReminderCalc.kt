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
 * 单一来源：ReminderWorker 触发后算下次触发、SettingsViewModel 开启时算下次触发，
 * 都走这里。
 */
object ReminderCalc {

    /**
     * 计算下一次触发的 epoch millis。
     *
     * - 不重复：返回原始日期-advance（若已过则 null）
     * - 每年/每月/每周：按当前时间找下一个未过的候选，扣 advance
     */
    fun nextTriggerMillis(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): Long? {
        val today = now.toLocalDate()
        val baseTime = parseTimeOrDefault(reminder.reminderTime)
        val zone = ZoneId.systemDefault()
        val triggerDate = LocalDate.ofEpochDay(reminder.date)

        val advanceLocal = when (reminder.repeat) {
            RepeatRule.NONE -> {
                val triggerDateTime = LocalDateTime.of(triggerDate, baseTime)
                val advance = triggerDateTime.minusDays(reminder.advanceDays.toLong())
                return if (advance.isAfter(now)) {
                    advance.atZone(zone).toInstant().toEpochMilli()
                } else null
            }
            RepeatRule.YEARLY -> {
                val thisYear = LocalDateTime.of(
                    today.withMonth(triggerDate.monthValue).withDayOfMonth(triggerDate.dayOfMonth),
                    baseTime
                )
                (if (thisYear.isAfter(now)) thisYear else thisYear.plusYears(1))
                    .minusDays(reminder.advanceDays.toLong())
            }
            RepeatRule.MONTHLY -> {
                val thisMonth = today.withDayOfMonth(
                    triggerDate.dayOfMonth.coerceAtMost(today.lengthOfMonth())
                ).atTime(baseTime)
                (if (thisMonth.isAfter(now)) thisMonth else thisMonth.plusMonths(1))
                    .minusDays(reminder.advanceDays.toLong())
            }
            RepeatRule.WEEKLY -> {
                val daysUntil = (triggerDate.dayOfWeek.value - today.dayOfWeek.value + 7) % 7
                val thisWeek = today.plusDays(daysUntil.toLong()).atTime(baseTime)
                (if (thisWeek.isAfter(now)) thisWeek else thisWeek.plusWeeks(1))
                    .minusDays(reminder.advanceDays.toLong())
            }
        }
        return advanceLocal.atZone(zone).toInstant().toEpochMilli()
    }

    private fun parseTimeOrDefault(time: String): LocalTime = try {
        val parts = time.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        LocalTime.of(8, 0)
    }
}
