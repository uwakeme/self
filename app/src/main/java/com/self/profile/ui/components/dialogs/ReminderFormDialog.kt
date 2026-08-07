package com.self.profile.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
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
import com.self.profile.domain.model.RepeatRule

/**
 * 提醒设置弹窗（US-007）。
 *
 * - 提前天数（数字输入，0-30）
 * - 提醒时间（HH:mm）
 * - 重复（不重复 / 每年 / 每月 / 每周）
 * - 备注
 */
@Composable
fun ReminderFormDialog(
    initialAdvanceDays: Int = 3,
    initialReminderTime: String = "08:00",
    initialRepeat: RepeatRule = RepeatRule.YEARLY,
    initialNotes: String = "",
    onConfirm: (advanceDays: Int, reminderTime: String, repeat: RepeatRule, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var advanceDays by remember { mutableStateOf(initialAdvanceDays.toString()) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var repeat by remember { mutableStateOf(initialRepeat) }
    var notes by remember { mutableStateOf(initialNotes) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRepeatPicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialog(
            initial = reminderTime,
            onConfirm = { t -> reminderTime = t; showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
    if (showRepeatPicker) {
        RepeatPickerDialog(
            selected = repeat,
            onConfirm = { r -> repeat = r; showRepeatPicker = false },
            onDismiss = { showRepeatPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "提前",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(64.dp)
                    )
                    OutlinedTextField(
                        value = advanceDays,
                        onValueChange = { advanceDays = it.filter(Char::isDigit).take(2) },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("天", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "时间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(64.dp)
                    )
                    TextButton(onClick = { showTimePicker = true }) {
                        Text(text = reminderTime, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "重复",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(64.dp)
                    )
                    TextButton(onClick = { showRepeatPicker = true }) {
                        Text(text = repeat.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    placeholder = { Text("可选") },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = advanceDays.toIntOrNull()?.coerceIn(0, 30) ?: 3
                onConfirm(days, reminderTime, repeat, notes)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun TimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initH, initM) = initial.split(":").let {
        (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    var hour by remember { mutableStateOf(initH.toString()) }
    var minute by remember { mutableStateOf(initM.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                    label = { Text("时") },
                    singleLine = true,
                    modifier = Modifier.width(80.dp)
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                    label = { Text("分") },
                    singleLine = true,
                    modifier = Modifier.width(80.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 8
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                onConfirm("%02d:%02d".format(h, m))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RepeatPickerDialog(
    selected: RepeatRule,
    onConfirm: (RepeatRule) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复频率") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                RepeatRule.values().forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(opt) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = opt.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (opt == selected) {
                            Text(text = "✓", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
