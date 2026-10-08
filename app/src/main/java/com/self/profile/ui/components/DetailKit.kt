package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 详情页小件（我的档案 / 人物详情共用）。
 */

/** Section 外壳：水平边距 + 统一 SectionCard */
@Composable
fun SectionWrap(
    title: String?,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionCard(title = title, action = action, content = content)
    }
}

/** 28dp 编辑小图标按钮 */
@Composable
fun EditIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "编辑",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 长段落文本块。
 *
 * @param bodyLarge 长描述（性格分析 / 理想型）传 true，用 bodyLarge (15sp) 读着更舒服；
 *                  其它短描述默认 bodyMedium (13sp) 跟周围 KV 行对齐。
 */
@Composable
fun TextBlock(text: String?, bodyLarge: Boolean = false, onClick: () -> Unit) {
    if (text.isNullOrBlank()) {
        Text(
            text = "点击编辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.clickable { onClick() }
        )
    } else {
        Text(
            text = text,
            style = if (bodyLarge) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onClick() }
        )
    }
}

/** 愿望清单的圆形勾选框 */
@Composable
fun CheckboxCircle(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (checked) WarmJournalColors.Success else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
