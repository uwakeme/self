package com.self.profile.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.self.profile.util.DateFormats
import java.time.LocalDate

/**
 * 添加纪念日对话框（我的档案 / 人物详情共用）。
 *
 * 校验语义：空标题/未选日期点保存显示错误提示并保留输入，不静默关闭。
 */
@Composable
fun AddAnniversaryDialog(
    onConfirm: (title: String, date: LocalDate, type: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var type by remember { mutableStateOf("纪念日") }
    var showDate by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    if (showDate) {
        DatePickerSheet(
            initialDate = date ?: LocalDate.now(),
            onConfirm = { d -> date = d; showDate = false; invalid = false },
            onDismiss = { showDate = false }
        )
    }
    if (showType) {
        ChoicePickerDialog(
            title = "类型",
            options = listOf("纪念日", "生日", "其他"),
            selected = type,
            onConfirm = { picked -> type = picked; showType = false },
            onDismiss = { showType = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加纪念日") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; invalid = false },
                    label = { Text("标题") },
                    singleLine = true,
                    isError = invalid && title.isBlank(),
                    supportingText = if (invalid && title.isBlank()) {
                        { Text("请填写标题") }
                    } else null
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showDate = true }
                ) {
                    Text(text = "日期：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = date?.format(DateFormats.FULL) ?: "选择",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (date == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (invalid && date == null) {
                    Text(
                        text = "请选择日期",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showType = true }
                ) {
                    Text(text = "类型：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(text = type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pickedDate = date
                    if (title.isNotBlank() && pickedDate != null) onConfirm(title, pickedDate, type)
                    else invalid = true
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
