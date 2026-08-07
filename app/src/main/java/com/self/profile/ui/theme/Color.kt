package com.self.profile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 暖手账色板。
 * 与 design/colors_and_type.css 一一对应。
 */
object WarmJournalColors {
    // 主色
    val Primary = Color(0xFFC4785B)
    val PrimaryLight = Color(0xFFD99B82)
    val PrimaryLighter = Color(0xFFEBC8B8)
    val PrimaryLightest = Color(0xFFF5E6DD)
    val PrimaryDark = Color(0xFFA65D42)
    val PrimaryDarker = Color(0xFF8B4A33)

    // 背景
    val Bg = Color(0xFFFBF7F3)
    val BgWarm = Color(0xFFF7F0E8)
    val BgCard = Color(0xFFFFFFFF)
    val BgCardAlt = Color(0xFFFDF9F5)
    val BgSurface = Color(0xFFF5EDE4)

    // 文字
    val TextPrimary = Color(0xFF3D2B1F)
    val TextSecondary = Color(0xFF7A6558)
    val TextTertiary = Color(0xFFA89585)
    val TextInverse = Color(0xFFFFFFFF)

    // 边框
    val Border = Color(0xFFE8DDD3)
    val BorderLight = Color(0xFFF0E8DF)
    val Divider = Color(0xFFEDE5DC)

    // 状态
    val Success = Color(0xFF6B9E78)
    val Warning = Color(0xFFD4A24E)
    val Error = Color(0xFFC45B5B)
    val Info = Color(0xFF6B8EAE)

    // 关系类型专用（对应 PRD §3.1 关系图谱图例）
    val RelationFamily = Primary         // 家人 - 暖棕
    val RelationFriend = Info            // 朋友 - 蓝
    val RelationColleague = Success      // 同事 - 绿
    val RelationClassmate = Warning      // 同学 - 金
}

/**
 * 深色色板（与 design/colors_and_type.css .dark 对应）
 */
object WarmJournalColorsDark {
    val Primary = Color(0xFFD99B82)
    val PrimaryLight = Color(0xFFEBC8B8)
    val PrimaryLighter = Color(0xFFF5E6DD)
    val PrimaryLightest = Color(0xFF3D2B1F)
    val PrimaryDark = Color(0xFFC4785B)
    val PrimaryDarker = Color(0xFFA65D42)

    val Bg = Color(0xFF1A1410)
    val BgWarm = Color(0xFF231C15)
    val BgCard = Color(0xFF2A221A)
    val BgCardAlt = Color(0xFF241E16)
    val BgSurface = Color(0xFF1F1912)

    val TextPrimary = Color(0xFFF0E8DF)
    val TextSecondary = Color(0xFFB8A99A)
    val TextTertiary = Color(0xFF8A7B6C)
    val TextInverse = Color(0xFF3D2B1F)

    val Border = Color(0xFF3D3228)
    val BorderLight = Color(0xFF332A22)
    val Divider = Color(0xFF382E25)

    val Success = Color(0xFF6B9E78)
    val Warning = Color(0xFFD4A24E)
    val Error = Color(0xFFC45B5B)
    val Info = Color(0xFF6B8EAE)

    val RelationFamily = PrimaryLight
    val RelationFriend = Info
    val RelationColleague = Success
    val RelationClassmate = Warning
}
