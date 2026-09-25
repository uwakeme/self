package com.self.profile.ui.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.DatePickerSheet
import com.self.profile.ui.components.dialogs.ReminderFormDialog
import com.self.profile.util.DateFormats
import com.self.profile.util.LunarHelper
import java.time.LocalDate

/**
 * 添加/编辑提醒对话框。
 *
 * - existing == null：添加模式
 * - existing != null：编辑模式（预填所有字段）
 *
 * UI：
 * 1. 关联人物（可选，chip 横滑列表）
 * 2. 类型（3 chip：生日/纪念日/自定义）
 * 3. 标题
 * 4. 日期（公历 DatePicker / 农历月+日选择，随农历开关切换）
 * 5. 高级设置（点击展开，调用 ReminderFormDialog）
 *
 * 农历开关只在 YEARLY 重复下有意义；保存时把农历月日换算成"下一个公历日"存入 date，
 * 调度时 ReminderCalc 再按农历逐年重算。
 */
@Composable
fun ReminderEditorDialog(
    existing: Reminder?,
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (
        personId: String?,
        type: ReminderType,
        title: String,
        date: LocalDate,
        advanceDays: Int,
        reminderTime: String,
        repeat: RepeatRule,
        notes: String?,
        isLunar: Boolean
    ) -> Unit
) {
    var personId by remember { mutableStateOf(existing?.personId) }
    var type by remember { mutableStateOf(existing?.type ?: ReminderType.CUSTOM) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(
        existing?.date?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().plusDays(7)
    ) }
    var isLunar by remember {
        mutableStateOf(existing?.isLunar ?: false)
    }
    var lunarMonth by remember {
        mutableStateOf(
            existing?.date?.let { LunarHelper.solarToLunarMonthDay(LocalDate.ofEpochDay(it)).first } ?: 1
        )
    }
    var lunarDay by remember {
        mutableStateOf(
            existing?.date?.let { LunarHelper.solarToLunarMonthDay(LocalDate.ofEpochDay(it)).second } ?: 1
        )
    }
    var advanceDays by remember { mutableStateOf(existing?.advanceDays ?: 3) }
    var reminderTime by remember { mutableStateOf(existing?.reminderTime ?: "08:00") }
    var repeat by remember { mutableStateOf(existing?.repeat ?: RepeatRule.YEARLY) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showLunarMonthPicker by remember { mutableStateOf(false) }
    var showLunarDayPicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    val dateFormatter = DateFormats.FULL
    val canSave = title.isNotBlank()

    if (showDatePicker) {
        DatePickerSheet(
            initialDate = date,
            onConfirm = { d ->
                date = d
                // 从公历切农历时，以所选公历日的农历月日为起点
                val (m, d2) = LunarHelper.solarToLunarMonthDay(d)
                lunarMonth = m
                lunarDay = d2
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showLunarMonthPicker) {
        ChoicePickerDialog(
            title = "农历月份",
            options = LunarHelper.lunarMonths.toList(),
            selected = LunarHelper.lunarMonths[lunarMonth - 1],
            onConfirm = { picked ->
                lunarMonth = LunarHelper.lunarMonths.indexOf(picked) + 1
                showLunarMonthPicker = false
                showLunarDayPicker = true
            },
            onDismiss = { showLunarMonthPicker = false }
        )
    }
    if (showLunarDayPicker) {
        ChoicePickerDialog(
            title = "农历日期",
            options = LunarHelper.lunarDays.toList(),
            selected = LunarHelper.lunarDays[lunarDay - 1],
            onConfirm = { picked ->
                lunarDay = LunarHelper.lunarDays.indexOf(picked) + 1
                showLunarDayPicker = false
            },
            onDismiss = { showLunarDayPicker = false }
        )
    }
    if (showAdvanced) {
        ReminderFormDialog(
            initialAdvanceDays = advanceDays,
            initialReminderTime = reminderTime,
            initialRepeat = repeat,
            initialNotes = notes,
            onConfirm = { days, time, rep, n ->
                advanceDays = days
                reminderTime = time
                repeat = rep
                notes = n
                showAdvanced = false
            },
            onDismiss = { showAdvanced = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "添加提醒" else "编辑提醒") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 关联人物
                FieldLabel("关联人物（可选）")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PersonChip(
                        label = "不绑定",
                        selected = personId == null,
                        onClick = { personId = null }
                    )
                    people.forEach { p ->
                        PersonChip(
                            label = p.name,
                            selected = personId == p.id,
                            onClick = { personId = p.id }
                        )
                    }
                }

                // 类型
                FieldLabel("类型")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) },
                            leadingIcon = if (type == t) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.height(16.dp)) }
                            } else null
                        )
                    }
                }

                // 标题
                FieldLabel("标题")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("如：妈妈生日 / 结婚纪念日") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 日期（农历开关：仅每年重复有意义）
                FieldLabel("日期")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = isLunar,
                        onClick = { isLunar = !isLunar },
                        label = { Text("农历") },
                        leadingIcon = if (isLunar) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.height(16.dp)) }
                        } else null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (repeat == RepeatRule.YEARLY) "农历生日按每年农历换算公历提醒"
                        else "农历仅在「每年」重复时生效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (isLunar) showLunarMonthPicker = true else showDatePicker = true
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isLunar) {
                            "农历${LunarHelper.lunarMonths[lunarMonth - 1]}${LunarHelper.lunarDays[lunarDay - 1]}"
                        } else {
                            date.format(dateFormatter)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 高级设置入口
                FieldLabel("高级设置（提前/时间/重复/备注）")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showAdvanced = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "提前 $advanceDays 天 · $reminderTime · ${repeat.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (notes.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "备注：$notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 农历：存"下一个农历对应公历日"，调度时逐年重算
                    val storedDate = if (isLunar) {
                        LunarHelper.nextLunarDateOnOrAfter(lunarMonth, lunarDay, LocalDate.now())
                    } else date
                    onSave(
                        personId, type, title, storedDate,
                        advanceDays, reminderTime, repeat, notes, isLunar
                    )
                    onDismiss()
                },
                enabled = canSave
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PersonChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
