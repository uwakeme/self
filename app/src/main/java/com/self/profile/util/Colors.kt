package com.self.profile.util

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 把 "#RRGGBB" 字符串解析为 Compose Color。
 *
 * 解析失败回退到主色（暖赤陶色）。
 */
fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val trimmed = hex.removePrefix("#")
    val long = runCatching { AndroidColor.parseColor("#$trimmed") }.getOrNull() ?: return null
    return Color(long)
}

/**
 * Compose Color → "#RRGGBB" 字符串。
 */
fun Color.toHex(): String {
    val argb = this.toArgb()
    val rgb = argb and 0xFFFFFF
    return String.format("#%06X", rgb)
}
