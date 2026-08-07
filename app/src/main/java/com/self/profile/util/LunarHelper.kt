package com.self.profile.util

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * 农历 / 星座 工具。
 *
 * 农历基于 [cn.6tail:lunar] 1.7.7，星座用日期范围查表。
 */
object LunarHelper {

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
