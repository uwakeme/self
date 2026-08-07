package com.self.profile.ui.reminder

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.Reminder
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.ReminderItem
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 提醒中心（US-007 完整版）。
 *
 * 布局：
 * 1. 顶栏（标题 + 返回）
 * 2. 统计概览（已设置 / 已开启 / 7 天内即将）
 * 3. 即将到来（前 5 条）
 * 4. 5 Tab 筛选（全部 / 生日 / 纪念日 / 自定义 / 已关闭）
 * 5. 完整列表（带 Switch + 删除）
 * 6. FAB（添加提醒，Step 2 接）
 */
@Composable
fun ReminderCenterScreen(
    onBack: () -> Unit,
    viewModel: ReminderCenterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = WarmJournalColors.Primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加提醒") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 顶栏
            item { HeaderBar(onBack = onBack) }

            // 统计概览
            item {
                StatsRow(
                    total = state.totalCount,
                    enabled = state.enabledCount,
                    upcoming = state.upcomingCount
                )
            }

            // 即将到来
            item {
                SectionHeader(title = "即将到来", subtitle = "未来 7 天内 · 最多 5 条")
            }
            if (state.upcoming.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "近期没有要触发的提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(state.upcoming, key = { "upcoming-${it.id}" }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        personInitial = state.peopleById[reminder.personId]?.avatarInitial,
                        personColor = state.peopleById[reminder.personId]?.avatarColor,
                        onClick = { editing = reminder },
                        onToggle = { viewModel.toggleEnabled(reminder) },
                        onDelete = { pendingDelete = reminder }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // 5 Tab
            item {
                Spacer(Modifier.height(8.dp))
                SecondaryTabRow(
                    selectedTabIndex = state.currentTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    ReminderTab.values().forEach { tab ->
                        Tab(
                            selected = state.currentTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            // 完整列表
            if (state.filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = when (state.currentTab) {
                                ReminderTab.DISABLED -> "没有已关闭的提醒"
                                ReminderTab.BIRTHDAY -> "还没有生日提醒"
                                ReminderTab.ANNIVERSARY -> "还没有纪念日提醒"
                                ReminderTab.CUSTOM -> "还没有自定义提醒"
                                ReminderTab.ALL -> "还没有任何提醒"
                            },
                            description = "点击右下角 + 添加"
                        )
                    }
                }
            } else {
                items(state.filtered, key = { "filtered-${it.id}" }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        personInitial = state.peopleById[reminder.personId]?.avatarInitial,
                        personColor = state.peopleById[reminder.personId]?.avatarColor,
                        onClick = { editing = reminder },
                        onToggle = { viewModel.toggleEnabled(reminder) },
                        onDelete = { pendingDelete = reminder }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // FAB 占位
        }
    }

    // 删除确认
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除提醒") },
            text = { Text("确定删除「${target.title}」？删除后无法恢复") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    // 添加
    if (showAddDialog) {
        ReminderEditorDialog(
            existing = null,
            people = state.peopleById.values.toList(),
            onDismiss = { showAddDialog = false },
            onSave = { personId, type, title, date, adv, time, repeat, notes ->
                viewModel.saveReminder(
                    existingId = null,
                    personId = personId,
                    type = type,
                    title = title,
                    date = date,
                    advanceDays = adv,
                    reminderTime = time,
                    repeat = repeat,
                    notes = notes
                )
            }
        )
    }

    // 编辑
    editing?.let { target ->
        ReminderEditorDialog(
            existing = target,
            people = state.peopleById.values.toList(),
            onDismiss = { editing = null },
            onSave = { personId, type, title, date, adv, time, repeat, notes ->
                viewModel.saveReminder(
                    existingId = target.id,
                    personId = personId,
                    type = type,
                    title = title,
                    date = date,
                    advanceDays = adv,
                    reminderTime = time,
                    repeat = repeat,
                    notes = notes
                )
            }
        )
    }
}

@Composable
private fun HeaderBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(WarmJournalColors.PrimaryLightest, WarmJournalColors.Bg)
                )
            )
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "提醒中心",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "管理你设的所有提醒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(total: Int, enabled: Int, upcoming: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox("已设置", total, WarmJournalColors.Primary, modifier = Modifier.weight(1f))
        StatBox("已开启", enabled, WarmJournalColors.Info, modifier = Modifier.weight(1f))
        StatBox("7天内", upcoming, WarmJournalColors.Success, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    personInitial: String?,
    personColor: String?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ReminderItem(
                reminder = reminder,
                personInitial = personInitial,
                personColor = personColor
            )
        }
        Switch(
            checked = reminder.enabled,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
