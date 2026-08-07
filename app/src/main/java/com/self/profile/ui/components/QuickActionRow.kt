package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 首页 4 个快捷入口（写笔记 / 加人物 / 纪念日 / 提醒）。
 *
 * 视觉规范（与 prototype 一致）：
 * - 等宽 4 列
 * - 每个独立主题色（暖赤陶 / 信息蓝 / 成功绿 / 警告金）
 * - 圆形图标 + 标签
 */
@Composable
fun QuickActionRow(
    onWriteNote: () -> Unit,
    onAddPerson: () -> Unit,
    onAnniversary: () -> Unit,
    onReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionItem(
            label = "写笔记",
            icon = Icons.Filled.Edit,
            tint = Color(0xFFC4785B),
            bg = Color(0xFFF5E6DD),
            border = Color(0xFFEBC8B8),
            onClick = onWriteNote,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            label = "加人物",
            icon = Icons.Filled.PersonAdd,
            tint = Color(0xFF6B8EAE),
            bg = Color(0xFFEBF0F5),
            border = Color(0xFFE0E8F0),
            onClick = onAddPerson,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            label = "纪念日",
            icon = Icons.Filled.Cake,
            tint = Color(0xFF6B9E78),
            bg = Color(0xFFE8F2EB),
            border = Color(0xFFD8E8DD),
            onClick = onAnniversary,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            label = "提醒",
            icon = Icons.Filled.Notifications,
            tint = Color(0xFFD4A24E),
            bg = Color(0xFFFDF3E0),
            border = Color(0xFFF5E6CC),
            onClick = onReminder,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    border: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .border(1.dp, border, MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
