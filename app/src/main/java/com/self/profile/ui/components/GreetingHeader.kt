package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.self.profile.util.parseHexColor
import java.time.LocalTime

/**
 * 首页问候头部：
 * - 渐变背景（主色 lightest → 页面 bg）
 * - 问候语：基于时间段（早上/下午/晚上）
 * - 右上角头像（点击进我的档案）
 *
 * 注：原"寄语"字段已移除——避免与底部 QuoteCard 重复显示同一句格言。
 *     今日寄语只在 HomeScreen 底部的 QuoteCard 渲染。
 */
@Composable
fun GreetingHeader(
    nickname: String,
    avatarUrl: String?,
    fallbackColor: String,
    fallbackInitial: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (greeting, _) = greetingForNow()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        com.self.profile.ui.theme.WarmJournalColors.PrimaryLightest,
                        com.self.profile.ui.theme.WarmJournalColors.Bg
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(end = 56.dp)) {
            Text(
                text = "$greeting，$nickname",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(parseHexColor(fallbackColor) ?: com.self.profile.ui.theme.WarmJournalColors.BgSurface)
                .clickable { onAvatarClick() }
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            } else {
                Avatar(
                    initial = fallbackInitial,
                    colorHex = fallbackColor,
                    size = 40.dp,
                    borderColor = com.self.profile.ui.theme.WarmJournalColors.Bg,
                    borderWidth = 2.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * 当前时段问候语。
 */
fun greetingForNow(now: LocalTime = LocalTime.now()): Pair<String, String> = when (now.hour) {
    in 5..11 -> "早上好" to "morning"
    in 12..17 -> "下午好" to "afternoon"
    else -> "晚上好" to "evening"
}
