package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
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
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.util.parseHexColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 提醒列表项（首页近期提醒 / 提醒中心复用）。
 *
 * 视觉规范（与 prototype self-profile.html 一致）：
 * - 左侧 40dp 头像（首字+背景色）
 * - 中间：标题 + 副标题（日期 + 倒计时）
 * - 右侧 28dp 类型图标
 */
@Composable
fun ReminderItem(
    reminder: Reminder,
    personInitial: String?,
    personColor: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg = parseHexColor(personColor) ?: Color(0xFFC4785B)
    val typeIcon: ImageVector = when (reminder.type) {
        ReminderType.BIRTHDAY -> Icons.Filled.Cake
        ReminderType.ANNIVERSARY -> Icons.Filled.Favorite
        ReminderType.CUSTOM -> Icons.Filled.Notifications
    }
    val typeTint = when (reminder.type) {
        ReminderType.BIRTHDAY -> Color(0xFFC4785B)
        ReminderType.ANNIVERSARY -> Color(0xFF6B8EAE)
        ReminderType.CUSTOM -> Color(0xFFD4A24E)
    }
    val dateText = LocalDate.ofEpochDay(reminder.date).format(DateTimeFormatter.ofPattern("M月d日"))
    val daysToGo = daysUntil(reminder.date)
    val countdownText = when {
        daysToGo == 0L -> "今天"
        daysToGo > 0 -> "还有${daysToGo}天"
        else -> "已过${-daysToGo}天"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Avatar(
            initial = personInitial ?: "·",
            colorHex = personColor ?: "#C4785B",
            size = 40.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$dateText · $countdownText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(typeTint.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = reminder.type.label,
                tint = typeTint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun daysUntil(epochDay: Long): Long {
    val today = LocalDate.now().toEpochDay()
    return epochDay - today
}
