package com.self.profile.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 多行文本编辑对话框。
 *
 * 适合性格分析、理想型等长文本。
 */
@Composable
fun EditTextAreaDialog(
    title: String,
    initialValue: String,
    placeholder: String = "",
    height: androidx.compose.ui.unit.Dp = 160.dp,
    confirmText: String = "保存",
    cancelText: String = "取消",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = if (placeholder.isNotEmpty()) {
                    { Text(text = placeholder) }
                } else null,
                modifier = Modifier.fillMaxWidth().height(height)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (value != initialValue) onConfirm(value)
                else onDismiss()
            }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = cancelText)
            }
        }
    )
}
