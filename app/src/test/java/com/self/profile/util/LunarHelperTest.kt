package com.self.profile.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate

/**
 * 农历换算单测。基准日均为可公开验证的农历锚点：
 * - 2024-02-10 = 甲辰年正月初一
 * - 2025-01-29 = 乙巳年正月初一
 * - 2025-05-31 = 端午（五月初五）
 * - 2025-10-06 = 中秋（八月十五）
 * - 2022-01-31 = 辛丑年腊月廿九（该年腊月为小月，无三十，除夕即廿九）
 */
class LunarHelperTest {

    @Test
    fun `春节锚点 公历转农历`() {
        assertEquals(1 to 1, LunarHelper.solarToLunarMonthDay(LocalDate.of(2024, 2, 10)))
        assertEquals(1 to 1, LunarHelper.solarToLunarMonthDay(LocalDate.of(2025, 1, 29)))
    }

    @Test
    fun `农历转公历 端午与中秋`() {
        assertEquals(LocalDate.of(2025, 5, 31), LunarHelper.lunarToSolar(2025, 5, 5))
        assertEquals(LocalDate.of(2025, 10, 6), LunarHelper.lunarToSolar(2025, 8, 15))
    }

    @Test
    fun `农历转公历 正月初一`() {
        assertEquals(LocalDate.of(2024, 2, 10), LunarHelper.lunarToSolar(2024, 1, 1))
        assertEquals(LocalDate.of(2025, 1, 29), LunarHelper.lunarToSolar(2025, 1, 1))
    }

    @Test
    fun `小月无三十时回退到廿九（除夕兼容）`() {
        // 2021 年农历腊月只有 29 天，"腊月三十"应回退为腊月廿九 = 2022-01-31（除夕）
        val solar = LunarHelper.lunarToSolar(2021, 12, 30)
        assertEquals(LocalDate.of(2022, 1, 31), solar)
        assertEquals(12 to 29, LunarHelper.solarToLunarMonthDay(solar))
    }

    @Test
    fun `下一次农历日期 含今天与跨年`() {
        // 今天恰逢当日：返回今天
        val qingming2025 = LunarHelper.lunarToSolar(2025, 5, 5)
        assertEquals(qingming2025, LunarHelper.nextLunarDateOnOrAfter(5, 5, qingming2025))
        // 已过：返回次年
        val after = qingming2025.plusDays(1)
        assertEquals(
            LunarHelper.lunarToSolar(2026, 5, 5),
            LunarHelper.nextLunarDateOnOrAfter(5, 5, after)
        )
    }

    @Test
    fun `农历显示标签`() {
        assertTrue(LunarHelper.lunarLabel(LocalDate.of(2025, 1, 29)).contains("正月初一"))
        assertTrue(LunarHelper.lunarLabel(LocalDate.of(2025, 10, 6)).contains("八月十五"))
    }

    @Test
    fun `换算往返稳定`() {
        // 同一农历年内、非闰月：公历 → 农历月日 → 当年公历应回到同一天
        // （注意不能用闰月日 / 农历跨年日，如 2025 闰六月，往返语义不成立）
        val solar = LocalDate.of(2025, 6, 1) // 五月初六（乙巳年）
        val (m, d) = LunarHelper.solarToLunarMonthDay(solar)
        assertEquals(solar, LunarHelper.lunarToSolar(2025, m, d))
    }
}
