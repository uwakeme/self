package com.self.profile.util

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.time.LocalDate
import kotlin.math.abs

/**
 * 农历 / 星座 工具。
 *
 * 农历基于 [cn.6tail:lunar] 1.7.7，星座用日期范围查表。
 */
object LunarHelper {

    /** 农历月份显示名（索引 + 1 = 月数：1=正月 … 12=腊月）。不支持闰月，闰月按当月处理。 */
    val lunarMonths = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    /** 农历日显示名（索引 + 1 = 日数：1=初一 … 30=三十） */
    val lunarDays = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    /** 公历 → 农历月日（1-12, 1-30；闰月取绝对值当普通月） */
    fun solarToLunarMonthDay(date: LocalDate): Pair<Int, Int> {
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
        return abs(lunar.month) to lunar.day
    }

    /**
     * 农历 → 当年公历。若当年无此农历日（如小月腊月三十），回退到月内前一日
     * （即"那年腊月没三十就过廿九"），保证永远返回有效日期。
     */
    fun lunarToSolar(year: Int, lunarMonth: Int, lunarDay: Int): LocalDate {
        var day = lunarDay
        while (day >= 1) {
            val candidate = runCatching {
                val solar = Lunar.fromYmd(year, lunarMonth, day).solar
                LocalDate.of(solar.year, solar.month, solar.day)
            }.getOrNull()
            if (candidate != null) {
                val (m, d) = solarToLunarMonthDay(candidate)
                if (m == lunarMonth && d == day) return candidate
            }
            day--
        }
        // 理论不可达：农历每月初一必存在
        val solar = Lunar.fromYmd(year, lunarMonth, 1).solar
        return LocalDate.of(solar.year, solar.month, solar.day)
    }

    /** 从 [today] 起（含当天）最近的一个农历 [month] 月 [day] 日对应的公历日期 */
    fun nextLunarDateOnOrAfter(month: Int, day: Int, today: LocalDate): LocalDate {
        val thisYear = lunarToSolar(today.year, month, day)
        return if (!thisYear.isBefore(today)) thisYear else lunarToSolar(today.year + 1, month, day)
    }

    /** 农历显示串："农历五月初一" */
    fun lunarLabel(date: LocalDate): String {
        val (m, d) = solarToLunarMonthDay(date)
        return "农历${lunarMonths[m - 1]}${lunarDays[d - 1]}"
    }

    /**
     * 阳历日期 → 农历字符串（"五月廿三"）。
     */
    fun solarToLunarString(date: LocalDate): String {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar
        return "${lunar.monthInChinese}月${lunar.dayInChinese}"
    }

    /**
     * 阳历日期 → 农历简写（"五月初三"），不显示月份数字。
     */
    fun solarToLunarShort(date: LocalDate): String {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar
        return "${lunar.monthInChinese}月${lunar.dayInChinese}"
    }

    /**
     * 阳历日期 → 星座字符串（"双子座"）。
     */
    fun dateToConstellation(date: LocalDate): String {
        val m = date.monthValue
        val d = date.dayOfMonth
        return when {
            (m == 3 && d >= 21) || (m == 4 && d <= 19) -> "白羊座"
            (m == 4 && d >= 20) || (m == 5 && d <= 20) -> "金牛座"
            (m == 5 && d >= 21) || (m == 6 && d <= 21) -> "双子座"
            (m == 6 && d >= 22) || (m == 7 && d <= 22) -> "巨蟹座"
            (m == 7 && d >= 23) || (m == 8 && d <= 22) -> "狮子座"
            (m == 8 && d >= 23) || (m == 9 && d <= 22) -> "处女座"
            (m == 9 && d >= 23) || (m == 10 && d <= 23) -> "天秤座"
            (m == 10 && d >= 24) || (m == 11 && d <= 22) -> "天蝎座"
            (m == 11 && d >= 23) || (m == 12 && d <= 21) -> "射手座"
            (m == 12 && d >= 22) || (m == 1 && d <= 19) -> "摩羯座"
            (m == 1 && d >= 20) || (m == 2 && d <= 18) -> "水瓶座"
            else -> "双鱼座"
        }
    }

    /**
     * 全部星座（按日期顺序）。
     */
    val allConstellations = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座",
        "狮子座", "处女座", "天秤座", "天蝎座",
        "射手座", "摩羯座", "水瓶座", "双鱼座"
    )

    /**
     * 全部 MBTI 类型（16 种）。
     */
    val allMbti = listOf(
        "INTJ", "INTP", "ENTJ", "ENTP",
        "INFJ", "INFP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP"
    )

    /**
     * 全部血型。
     */
    val allBloodTypes = listOf("A", "B", "AB", "O", "其他")

    /**
     * 全部关系类型（中文 label）。
     */
    val allRelationshipTypes = listOf("家人", "朋友", "同事", "同学")

    /**
     * 衣服尺码（中国大陆常见）。
     */
    val allTopSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
    val allPantsSizes = listOf("XS", "S", "M", "L", "XL", "XXL", "26", "27", "28", "29", "30", "31", "32")
    val allSkirtSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
    val allShoeSizes = listOf("35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45")
}
