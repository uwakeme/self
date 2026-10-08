package com.self.profile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.EditTextDialog

/**
 * 详情页通用 KV 编辑行（我的档案 / 人物详情共用）。
 *
 * 统一语义：可清空、清空存 null（两屏历史上曾漂移为空串/不可清空，已对齐）。
 */

/** 固定 20dp 图标占位：有图标就画，没有就留空，确保标签列永远对齐 */
@Composable
fun LeadingIcon(icon: ImageVector?) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** 单行文本字段（家乡/大学/工作…），弹窗编辑，清空存 null */
@Composable
fun EditableKV(
    label: String,
    value: String?,
    leadingIcon: ImageVector? = null,
    labelWidth: androidx.compose.ui.unit.Dp = 72.dp,
    onSave: (String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "编辑$label",
            initialValue = value.orEmpty(),
            placeholder = "点击输入",
            onConfirm = { newVal ->
                onSave(newVal.trim().takeIf { it.isNotEmpty() })
            },
            onDismiss = { showDialog = false }
        )
    }
    KVRowScaffold(label, value, "点击编辑", leadingIcon, labelWidth) { showDialog = true }
}

/** 数值字段（身高/体重…），数字键盘，清空存 null */
@Composable
fun NumberKV(
    label: String,
    value: String?,
    leadingIcon: ImageVector? = null,
    onSave: (String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "编辑$label",
            initialValue = value.orEmpty(),
            placeholder = "数字",
            keyboardType = KeyboardType.Number,
            onConfirm = { newVal ->
                onSave(newVal.takeIf { it.isNotBlank() })
            },
            onDismiss = { showDialog = false }
        )
    }
    KVRowScaffold(label, value, "点击编辑", leadingIcon, 120.dp) { showDialog = true }
}

/** 单选字段（鞋码/尺码…），底部选择器，清空存 null */
@Composable
fun ChoiceKV(
    label: String,
    value: String?,
    options: List<String>,
    leadingIcon: ImageVector? = null,
    onSave: (String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ChoicePickerDialog(
            title = "选择$label",
            options = listOf("") + options,
            selected = value,
            onConfirm = { picked -> onSave(picked.takeIf { it.isNotBlank() }) },
            onDismiss = { showDialog = false }
        )
    }
    KVRowScaffold(label, value, "点击选择", leadingIcon, 120.dp) { showDialog = true }
}

@Composable
private fun KVRowScaffold(
    label: String,
    value: String?,
    emptyHint: String,
    leadingIcon: ImageVector?,
    labelWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        LeadingIcon(leadingIcon)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: emptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
