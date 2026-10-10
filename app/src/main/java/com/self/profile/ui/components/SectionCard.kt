package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 通用卡片容器：surface 纯色背景 + 20dp 内边距，**没有边框、没有圆角**。
 *
 * 「无边框无圆角」是刻意为之，不是漏实现：设置页的「全部提醒」卡片和回收站的
 * 人物/提醒列表都被 LazyColumn 拆成了多个 item，靠 `SettingsScreen.kt` 的
 * `CardSegment` / `RecycleBinScreen.kt` 的 `BinCardSegment` 各自延续同一块背景
 * 拼回去（见两处注释）。只要这张卡是带圆角的，分段处就会露 N 道圆角接缝。
 * 要改形状请连同那两处分段逻辑一起改。
 *
 * 可选 [title]：左上角标题 + 右侧 [action] slot
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        if (title != null || action != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                action?.invoke()
            }
        }
        content()
    }
}
