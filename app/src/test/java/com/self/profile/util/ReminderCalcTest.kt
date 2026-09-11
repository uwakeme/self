package com.self.profile.util

import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderCalcTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun reminder(
        date: LocalDate,
        repeat: RepeatRule = RepeatRule.YEARLY,
        advanceDays: Int = 0,
        time: String = "08:00",
        isLunar: Boolean = false
    ) = Reminder(
        id = "r1",
        personId = null,
        type = ReminderType.BIRTHDAY,
        title = "t",
        date = date.toEpochDay(),
        advanceDays = advanceDays,
        reminderTime = time,
        repeat = repeat,
        enabled = true,
        createdAt = 0L,
        updatedAt = 0L,
        isLunar = isLunar
    )

    private fun millisOf(dt: LocalDateTime): Long = dt.atZone(zone).toInstant().toEpochMilli()

    // ====== 不重复 ======

    @Test
    fun `NONE 未来日期 返回日期减提前量`() {
        val r = reminder(LocalDate.of(2026, 6, 1), repeat = RepeatRule.NONE, advanceDays = 3)
        val now = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(millisOf(LocalDateTime.of(2026, 5, 29, 8, 0)), ReminderCalc.nextTriggerMillis(r, now))
    }

    @Test
    fun `NONE 已过期 返回 null`() {
        val r = reminder(LocalDate.of(2025, 6, 1), repeat = RepeatRule.NONE)
        val now = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertNull(ReminderCalc.nextTriggerMillis(r, now))
    }

    // ====== 每年 ======

    @Test
    fun `YEARLY 取今年的月日`() {
        val r = reminder(LocalDate.of(2020, 3, 15), advanceDays = 3)
        val now = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(millisOf(LocalDateTime.of(2026, 3, 12, 8, 0)), ReminderCalc.nextTriggerMillis(r, now))
    }

    @Test
    fun `YEARLY 提前量窗口内重复触发 严格顺延到下一年`() {
        // 回归：Worker 在 (正日 - 提前量) 时刻触发后，旧实现会用已过去的时刻重排，
        // 造成 0 延迟连环触发刷通知；现在必须严格晚于 now
        val r = reminder(LocalDate.of(2020, 3, 15), advanceDays = 3)
        val fireMoment = LocalDateTime.of(2026, 3, 12, 8, 0) // 恰好等于下次触发时刻
        val next = ReminderCalc.nextTriggerMillis(r, fireMoment)
        assertNotNull(next)
        assertEquals(millisOf(LocalDateTime.of(2027, 3, 12, 8, 0)), next)
    }

    @Test
    fun `YEARLY 正日当天时间已过 顺延下一年`() {
        val r = reminder(LocalDate.of(2020, 3, 15), advanceDays = 0)
        val now = LocalDateTime.of(2026, 3, 15, 9, 0) // 08:00 已过
        assertEquals(millisOf(LocalDateTime.of(2027, 3, 15, 8, 0)), ReminderCalc.nextTriggerMillis(r, now))
    }

    // ====== 农历每年 ======

    @Test
    fun `YEARLY 农历 按当年农历换算公历`() {
        // 存储日 = 2025 年端午的公历日；nextOccurrence 按 2026 年端午换算
        val stored = LunarHelper.lunarToSolar(2025, 5, 5)
        val r = reminder(stored, isLunar = true)
        val today = LocalDate.of(2025, 6, 15) // 2025 端午已过
        assertEquals(LunarHelper.lunarToSolar(2026, 5, 5), ReminderCalc.nextOccurrenceDate(r, today))
    }

    @Test
    fun `YEARLY 农历 当日未过 返回今年对应日`() {
        val stored = LunarHelper.lunarToSolar(2025, 5, 5)
        val r = reminder(stored, isLunar = true)
        val today = LocalDate.of(2025, 1, 1)
        assertEquals(stored, ReminderCalc.nextOccurrenceDate(r, today))
    }

    @Test
    fun `YEARLY 农历 倒计时用换算后的公历而非原始存储日`() {
        // 回归：农历生日每年公历漂移，倒计时若直接用 reminder.date 会越算越偏
        val stored = LunarHelper.lunarToSolar(2024, 5, 5)
        val r = reminder(stored, isLunar = true)
        val today = LocalDate.of(2026, 6, 1) // 2024 存储日早已过去
        val next = ReminderCalc.nextOccurrenceDate(r, today)
        assertTrue(next.isAfter(today))
        assertEquals(LunarHelper.lunarToSolar(2026, 5, 5), next)
    }

    // ====== 每月 / 每周 ======

    @Test
    fun `MONTHLY 月末越界收敛到当月最后一天`() {
        val r = reminder(LocalDate.of(2024, 1, 31), repeat = RepeatRule.MONTHLY)
        val now = LocalDateTime.of(2025, 2, 10, 0, 0)
        assertEquals(millisOf(LocalDateTime.of(2025, 2, 28, 8, 0)), ReminderCalc.nextTriggerMillis(r, now))
    }

    @Test
    fun `WEEKLY 找下一个同星期几`() {
        // 2025-09-08 周一；now = 2025-09-10 周三 → 下一个周一 2025-09-15
        val r = reminder(LocalDate.of(2025, 9, 8), repeat = RepeatRule.WEEKLY)
        val now = LocalDateTime.of(2025, 9, 10, 0, 0)
        assertEquals(LocalDate.of(2025, 9, 15), ReminderCalc.nextOccurrenceDate(r, now.toLocalDate()))
    }

    // ====== 健壮性 ======

    @Test
    fun `非法时间串回退 08点 仍能算出下次触发`() {
        val r = reminder(LocalDate.of(2020, 3, 15), time = "bad")
        val next = ReminderCalc.nextTriggerMillis(r, LocalDateTime.of(2026, 1, 1, 0, 0))
        assertNotNull(next)
        assertFalse(next!! <= 0)
    }
}
