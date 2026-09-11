package com.self.profile.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.data.repository.RecycleBinJanitor
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 回收站：查看 / 恢复 / 彻底删除软删的人物与提醒。
 * 软删数据保留 [RecycleBinJanitor.RETENTION_DAYS] 天，到期启动时自动清除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var pendingPurgePerson by remember { mutableStateOf<Person?>(null) }
    var pendingPurgeReminder by remember { mutableStateOf<Reminder?>(null) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (!state.isEmpty) {
                        TextButton(onClick = { showEmptyConfirm = true }) {
                            Text("清空", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                EmptyState(
                    title = "回收站是空的",
                    description = "删除的人物和提醒会在这里保留 ${RecycleBinJanitor.RETENTION_DAYS} 天"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                if (state.people.isNotEmpty()) {
                    item {
                        SectionLabel("人物（${state.people.size}）")
                    }
                    item {
                        SectionCard(title = null) {
                            Column {
                                state.people.forEachIndexed { index, person ->
                                    DeletedPersonRow(
                                        person = person,
                                        onRestore = { viewModel.restorePerson(person) },
                                        onPurge = { pendingPurgePerson = person }
                                    )
                                    if (index < state.people.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.reminders.isNotEmpty()) {
                    item {
                        SectionLabel("提醒（${state.reminders.size}）")
                    }
                    item {
                        SectionCard(title = null) {
                            Column {
                                state.reminders.forEachIndexed { index, reminder ->
                                    DeletedReminderRow(
                                        reminder = reminder,
                                        onRestore = { viewModel.restoreReminder(reminder) },
                                        onPurge = { pendingPurgeReminder = reminder }
                                    )
                                    if (index < state.reminders.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "回收站内容保留 ${RecycleBinJanitor.RETENTION_DAYS} 天，之后自动彻底删除。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }
    }

    // 彻底删除人物确认
    pendingPurgePerson?.let { person ->
        AlertDialog(
            onDismissRequest = { pendingPurgePerson = null },
            title = { Text("彻底删除「${person.name}」？") },
            text = {
                Text(
                    "人物、关联提醒和 ${person.photos.size} 张照片将被永久删除，无法恢复。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.purgePerson(person)
                    pendingPurgePerson = null
                }) {
                    Text(
                        "彻底删除",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurgePerson = null }) { Text("取消") }
            }
        )
    }

    // 彻底删除提醒确认
    pendingPurgeReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { pendingPurgeReminder = null },
            title = { Text("彻底删除提醒？") },
            text = { Text("「${reminder.title}」将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.purgeReminder(reminder)
                    pendingPurgeReminder = null
                }) {
                    Text(
                        "彻底删除",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurgeReminder = null }) { Text("取消") }
            }
        )
    }

    // 清空回收站确认
    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("清空回收站？") },
            text = { Text("回收站里的所有人物、提醒和照片将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyBin()
                    showEmptyConfirm = false
                }) {
                    Text(
                        "清空",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun DeletedPersonRow(
    person: Person,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Avatar(initial = person.avatarInitial, colorHex = person.avatarColor, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "删除于 ${formatDate(person.deletedAt)} · ${daysLeftText(person.deletedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Filled.Restore, contentDescription = "恢复", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onPurge) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = "彻底删除",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DeletedReminderRow(
    reminder: Reminder,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${reminder.type.label} · 删除于 ${formatDate(reminder.deletedAt)} · ${daysLeftText(reminder.deletedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Filled.Restore, contentDescription = "恢复", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onPurge) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = "彻底删除",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null) return "未知"
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("M月d日"))
}

private fun daysLeftText(deletedAt: Long?): String {
    if (deletedAt == null) return ""
    val purgeDay = java.time.Instant.ofEpochMilli(RecycleBinJanitor.purgeAt(deletedAt))
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val days = purgeDay.toEpochDay() - LocalDate.now().toEpochDay()
    return if (days > 0) "${days}天后自动清除" else "即将自动清除"
}
