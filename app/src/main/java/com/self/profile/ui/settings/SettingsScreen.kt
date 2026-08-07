package com.self.profile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.BuildConfig
import com.self.profile.domain.model.DarkModePref
import com.self.profile.domain.model.FontSizePref
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.SectionCard
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.theme.WarmJournalColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 设置页 / 提醒中心（US-012 / US-013 / US-014 / US-015 / US-018）。
 *
 * 阶段 6 接线：
 * - 深色模式（点开 ChoicePickerDialog 选 关/开/跟随系统）
 * - 字体大小（点开 ChoicePickerDialog 选 小/标准/大/特大）
 * - 应用锁（ToggleRow 开关）
 * - 农历生日显示（ToggleRow 开关）
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var showDarkModePicker by remember { mutableStateOf(false) }
    var showFontScalePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部 Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(WarmJournalColors.PrimaryLightest, WarmJournalColors.Bg)
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(WarmJournalColors.PrimaryLightest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = WarmJournalColors.Primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "提醒中心",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "管理所有生日、纪念日和自定义提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 统计概览
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionCard(title = null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCell("已设置", state.total)
                    StatCell("已开启", state.enabledCount)
                    StatCell("7 天内", state.upcomingCount)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 即将到来
        if (state.upcoming.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "即将到来") {
                    Column {
                        state.upcoming.take(5).forEach { item ->
                            UpcomingRow(item)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 全部提醒
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionCard(title = "全部提醒") {
                if (state.all.isEmpty()) {
                    Text(
                        text = "还没有设置任何提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column {
                        state.all.forEach { item ->
                            AllReminderRow(item, onToggle = { enabled ->
                                viewModel.toggleEnabled(item.reminder.id, enabled)
                            })
                            if (item != state.all.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 快捷设置
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionCard(title = "快捷设置") {
                Column {
                    SettingRow("默认提前天数", "${state.settings.defaultAdvanceDays} 天")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow("每日提醒时间", state.settings.defaultReminderTime)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ToggleRow(
                        label = "农历生日显示",
                        checked = state.settings.showLunarBirthday,
                        onToggle = { viewModel.setShowLunarBirthday(it) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 应用设置
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionCard(title = "应用设置") {
                Column {
                    SettingRow(
                        label = "深色模式",
                        value = darkModeLabel(state.settings.darkMode),
                        onClick = { showDarkModePicker = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        label = "字体大小",
                        value = fontScaleLabel(state.settings.fontScale),
                        onClick = { showFontScalePicker = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        label = "数据管理",
                        value = if (exportState is ExportState.Running) "导出中…" else "导出/清除",
                        onClick = { viewModel.exportData() }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ToggleRow(
                        label = "应用锁",
                        checked = state.settings.appLockEnabled,
                        onToggle = { viewModel.setAppLockEnabled(it) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 关于
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionCard(title = "关于") {
                Column {
                    SettingRow("应用名称", "自我档案与人物图鉴")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow("版本号", "v${BuildConfig.VERSION_NAME}")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    // 深色模式选择
    if (showDarkModePicker) {
        ChoicePickerDialog(
            title = "深色模式",
            options = DarkModePref.values().map { darkModeLabel(it) },
            selected = darkModeLabel(state.settings.darkMode),
            onConfirm = { pickedLabel ->
                val picked = DarkModePref.values()
                    .firstOrNull { darkModeLabel(it) == pickedLabel } ?: DarkModePref.SYSTEM
                viewModel.setDarkMode(picked)
                showDarkModePicker = false
            },
            onDismiss = { showDarkModePicker = false }
        )
    }

    // 字体大小选择
    if (showFontScalePicker) {
        ChoicePickerDialog(
            title = "字体大小",
            options = FontSizePref.values().map { fontScaleLabel(it) },
            selected = fontScaleLabel(state.settings.fontScale),
            onConfirm = { pickedLabel ->
                val picked = FontSizePref.values()
                    .firstOrNull { fontScaleLabel(it) == pickedLabel } ?: FontSizePref.STANDARD
                viewModel.setFontScale(picked)
                showFontScalePicker = false
            },
            onDismiss = { showFontScalePicker = false }
        )
    }

    // 导出结果
    when (val es = exportState) {
        ExportState.Idle, ExportState.Running -> Unit
        is ExportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportState() },
                title = { Text("导出成功") },
                text = { Text("文件已保存到：\n${es.path}") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportState() }) { Text("好的") }
                }
            )
        }
        is ExportState.Failure -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportState() },
                title = { Text("导出失败") },
                text = { Text("错误：${es.error}") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportState() }) { Text("好的") }
                }
            )
        }
    }
}

private fun darkModeLabel(pref: DarkModePref): String = when (pref) {
    DarkModePref.OFF -> "关闭"
    DarkModePref.ON -> "开启"
    DarkModePref.SYSTEM -> "跟随系统"
}

private fun fontScaleLabel(pref: FontSizePref): String = when (pref) {
    FontSizePref.SMALL -> "小号"
    FontSizePref.STANDARD -> "标准"
    FontSizePref.LARGE -> "大号"
    FontSizePref.XLARGE -> "特大"
}

@Composable
private fun StatCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$value", style = MaterialTheme.typography.displaySmall, color = WarmJournalColors.Primary)
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UpcomingRow(item: UpcomingReminder) {
    val days = item.reminder.date - LocalDate.now().toEpochDay()
    val countdown = when {
        days == 0L -> "今天"
        days > 0 -> "还有${days}天"
        else -> "已过${-days}天"
    }
    val dateText = LocalDate.ofEpochDay(item.reminder.date).format(DateTimeFormatter.ofPattern("M月d日"))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Avatar(initial = item.personInitial, colorHex = item.personColor, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.personName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${item.reminder.type.label} · $dateText · $countdown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AllReminderRow(item: AllReminder, onToggle: (Boolean) -> Unit) {
    val days = item.reminder.date - LocalDate.now().toEpochDay()
    val countdown = when {
        days == 0L -> "今天"
        days > 0 -> "还有${days}天"
        else -> "已过${-days}天"
    }
    val dateText = LocalDate.ofEpochDay(item.reminder.date).format(DateTimeFormatter.ofPattern("M月d日"))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Avatar(initial = item.personInitial, colorHex = item.personColor, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.personName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${item.reminder.type.label} · $dateText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "提前 ${item.reminder.advanceDays} 天 · ${item.reminder.reminderTime} · $countdown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(
            checked = item.reminder.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmJournalColors.Primary,
                checkedTrackColor = WarmJournalColors.PrimaryLightest
            )
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmJournalColors.Primary,
                checkedTrackColor = WarmJournalColors.PrimaryLightest
            )
        )
    }
}
