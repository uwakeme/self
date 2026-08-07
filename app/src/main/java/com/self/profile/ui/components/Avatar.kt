package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.self.profile.util.parseHexColor

/**
 * 人物头像圆圈。
 *
 * 视觉规范（与 design/colors_and_type.css 一致）：
 * - 圆圈 + 居中首字
 * - 文字色按背景色明度自动选白/深棕
 * - 边框：实色 1-2dp（用于叠加头像区分时）
 */
@Composable
fun Avatar(
    initial: String,
    colorHex: String,
    size: Dp = 40.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    val bg = parseHexColor(colorHex) ?: Color(0xFFC4785B)
    val onBg = if (bg.isLight()) Color(0xFF3D2B1F) else Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (borderColor != null) Modifier.border(borderWidth, borderColor, CircleShape)
                else Modifier
            )
    ) {
        Text(
            text = initial.take(2),
            color = onBg,
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = (size.value * 0.4f).sp
            )
        )
    }
}

/**
 * 简单判断颜色明度。luminance > 0.6 视为浅色。
 */
fun Color.isLight(): Boolean {
    val r = red
    val g = green
    val b = blue
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return luminance > 0.6f
}
