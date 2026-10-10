package com.self.profile.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.BuildConfig
import com.self.profile.R
import com.self.profile.data.backup.DataExporter
import com.self.profile.domain.model.DarkModePref
import com.self.profile.domain.model.FontSizePref
import com.self.profile.domain.model.Reminder
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.PageHeader
import com.self.profile.ui.components.SectionCard
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.reminder.ReminderEditorDialog
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.DateFormats
import com.self.profile.util.LunarHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 设置页 / 提醒中心（US-012 / US-013 / US-014 / US-015 / US-018）。
 *
 * Phase 3.1 整合后：
 * - 上半 = 提醒中心：统计 + 即将到来 + 5 Tab 过滤 + 完整列表（toggle/edit/delete）+ FAB 添加
 * - 下半 = 设置：快捷设置 / 应用设置 / 关于
 *
 * 数据管理：导出 zip（含照片）到 SAF 自选位置 / 从备份导入 / 清除 / 回收站。
 */
@Composable
fun SettingsScreen(
    onNavigateToRecycleBin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val pendingImportUri by viewModel.pendingImportUri.collectAsStateWithLifecycle()
    val clearState by viewModel.clearState.collectAsStateWithLifecycle()

    var showDarkModePicker by remember { mutableStateOf(false) }
    var showFontScalePicker by remember { mutableStateOf(false) }
    var showDataManageSheet by remember { mutableStateOf(false) }
    var showClearStep1 by remember { mutableStateOf(false) }
    var showClearStep2 by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }

    val listState = rememberLazyListState()

    // 切 Tab 会让 itemsIndexed 的数据源从「上百条」骤减到「几条」。改之前这里是
    // Column + verticalScroll，ScrollState 会把 offset 自动夹到 maxValue，所以看不出问题；
    // 换成 LazyColumn 后 firstVisibleItemIndex 可能停在超出新 itemCount 的位置，列表直接空白。
    //
    // 刻意「只在越界时夹回边界」而不是每次切 Tab 都滚回顶部：前者等价于 ScrollState 的夹取语义
    // （新列表够长就保持原位置，不够长才落到末尾），无条件归位则是每次切 Tab 都丢位置的行为变更。
    // 观察 totalItemsCount 而不是读一次 layoutInfo，是因为新 itemCount 要等 measure 之后才反映到
    // layoutInfo 上，在 effect 体里直接读拿到的是上一帧的旧值；持续观察还能顺带兜住
    // 「停留在列表深处时删掉提醒」这类同类的骤减。
    LaunchedEffect(state.currentTab) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .collect { total ->
                val lastIndex = total - 1
                if (lastIndex >= 0 && listState.firstVisibleItemIndex > lastIndex) {
                    listState.scrollToItem(lastIndex)
                }
            }
    }

    // Android 13+ 通知运行时权限：不申请的话提醒到点全部静默
    val context = LocalContext.current
    var notifPermissionGranted by remember { mutableStateOf(checkNotifPermission(context)) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifPermissionGranted = granted }
    // 用户可能去系统设置里改，回前台时刷新
    DisposableEffect(context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifPermissionGranted = checkNotifPermission(context)
            }
        }
        (context as? LifecycleOwner)?.lifecycle?.addObserver(observer)
        onDispose { (context as? LifecycleOwner)?.lifecycle?.removeObserver(observer) }
    }

    // SAF：导出 = CreateDocument 自选保存位置；导入 = OpenDocument 选备份文件
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(viewModel::exportData) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onImportFilePicked) }

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
        // 整页压成单层 LazyColumn。原先这里是 Column + verticalScroll：滚动容器会给子节点
        // 无限高约束，提醒列表再套 LazyColumn 会直接崩（infinity maximum height constraints），
        // 不滚动又无法只组化可见项，所以各 section 一律摊平成 LazyListScope 的 item。
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            // 等价于原先滚动列末尾的 Spacer(80.dp)，给 FAB 让位
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 顶部 Header（统一页面头部；本页上半为提醒中心）
            item {
                PageHeader(
                    title = "设置",
                    subtitle = "提醒中心 · 数据 · 偏好"
                )
            }

            // 通知权限引导（Android 13+ 未授权时）
            if (!notifPermissionGranted) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionCard(title = null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = WarmJournalColors.Warning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "通知权限未开启",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "开启后提醒才能在通知栏弹出",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) { Text("去开启") }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // 统计概览
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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
            }
            item { Spacer(Modifier.height(8.dp)) }

            // 即将到来：被 take(5) 硬性截断，长度有界，仍作单个 item 组合
            if (state.upcoming.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionCard(title = "即将到来") {
                            Column {
                                state.upcoming.take(5).forEach { item ->
                                    UpcomingRow(item, state.today)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // 「全部提醒」卡片：本页唯一会随数据量无限增长的一段，拆成 item 列表。
            // 卡片拆开后由 CardSegment 让每段各自延续同一块 surface 背景，
            // 首段给上内边距、末段给下内边距，拼起来与原 SectionCard 视觉一致。
            item(key = "all_reminders_header") {
                CardSegment(topPadding = 20.dp) {
                    Text(
                        text = "全部提醒",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    SecondaryTabRow(
                        selectedTabIndex = state.currentTab.ordinal,
                        containerColor = Color.Transparent
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
            }
            if (state.filtered.isEmpty()) {
                item(key = "all_reminders_empty") {
                    CardSegment(bottomPadding = 20.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
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
                }
            } else {
                // key 用提醒业务主键 reminder.id，不用 index：开关 / 删除会让列表重排，
                // 按 index 复用会让状态挂到别的行上
                itemsIndexed(
                    items = state.filtered,
                    key = { _, item -> "all_reminder_${item.reminder.id}" }
                ) { index, item ->
                    CardSegment(
                        bottomPadding = if (index == state.filtered.lastIndex) 20.dp else 0.dp
                    ) {
                        AllReminderRow(
                            item = item,
                            today = state.today,
                            onClick = { editing = item.reminder },
                            onToggle = { enabled ->
                                viewModel.toggleEnabled(item.reminder.id, enabled)
                            },
                            onDelete = { pendingDelete = item.reminder }
                        )
                        if (index < state.filtered.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }

            // 快捷设置（固定 3 行，item 数不随数据增长）
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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
            }
            item { Spacer(Modifier.height(8.dp)) }

            // 应用设置（固定 5 行）
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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
                                value = when {
                                    clearState is ClearState.Running -> "清除中…"
                                    exportState is ExportState.Running -> "导出中…"
                                    importState is ImportState.Running -> "导入中…"
                                    else -> "备份/导入"
                                },
                                onClick = { showDataManageSheet = true }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingRow(
                                label = "回收站",
                                value = "已删除的人物与提醒",
                                onClick = {
                                    showDataManageSheet = false
                                    onNavigateToRecycleBin()
                                }
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
            }
            item { Spacer(Modifier.height(8.dp)) }

            // 关于（固定 2 行）
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionCard(title = "关于") {
                        Column {
                            SettingRow("应用名称", "自我档案与人物图鉴")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingRow("版本号", "v${BuildConfig.VERSION_NAME}")
                        }
                    }
                }
            }
        }
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

    // 数据管理 BottomSheet
    if (showDataManageSheet) {
        DataManageBottomSheet(
            lastBackupTime = state.settings.lastBackupTime,
            onExport = {
                showDataManageSheet = false
                exportLauncher.launch(DataExporter.suggestedFileName())
            },
            onImport = {
                showDataManageSheet = false
                importLauncher.launch(
                    arrayOf(
                        "application/zip",
                        "application/x-zip-compressed",
                        "application/octet-stream",
                        "application/json"
                    )
                )
            },
            onClear = {
                showDataManageSheet = false
                showClearStep1 = true
            },
            onDismiss = { showDataManageSheet = false }
        )
    }

    // 导入前确认：覆盖当前全部数据
    pendingImportUri?.let { importUri ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImportConfirm() },
            title = { Text("导入备份？") },
            text = {
                Text(
                    "导入将覆盖当前所有人物、提醒和照片。\n\n" +
                        "覆盖前会自动在应用缓存中留一份当前数据的安全备份。"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(importUri) }) {
                    Text("覆盖导入", color = WarmJournalColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImportConfirm() }) { Text("取消") }
            }
        )
    }

    // 清除确认：第一步（带统计）
    if (showClearStep1) {
        AlertDialog(
            onDismissRequest = { showClearStep1 = false },
            title = { Text("清除所有数据？") },
            text = {
                Text(
                    "将永久删除：\n" +
                        "· ${state.peopleById.size} 位人物（含你自己）\n" +
                        "· ${state.all.size} 条提醒\n" +
                        "· 所有已上传的照片\n\n" +
                        "设置项（深色模式、字体、应用锁等）不会被清除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearStep1 = false
                        showClearStep2 = true
                    }
                ) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { showClearStep1 = false }) { Text("取消") }
            }
        )
    }

    // 清除确认：第二步（最终警告）
    if (showClearStep2) {
        AlertDialog(
            onDismissRequest = { showClearStep2 = false },
            title = { Text("此操作不可撤销") },
            text = {
                Text(
                    "再次确认：所有人物、提醒和照片都将被永久删除。\n\n" +
                        "建议先在「导出」中保存一份备份。",
                    color = WarmJournalColors.Primary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearStep2 = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text(
                        "永久删除",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStep2 = false }) { Text("取消") }
            }
        )
    }

    // 清除结果
    when (val cs = clearState) {
        ClearState.Idle, ClearState.Running -> Unit
        is ClearState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearClearState() },
                title = { Text("已清除") },
                text = {
                    Text(
                        "已删除：\n" +
                            "· ${cs.summary.peopleDeleted} 位人物\n" +
                            "· ${cs.summary.remindersDeleted} 条提醒\n" +
                            "· ${cs.summary.photosDeleted} 张照片\n\n" +
                            "应用将回到初始状态。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearClearState() }) { Text("好的") }
                }
            )
        }
        is ClearState.Failure -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearClearState() },
                title = { Text("清除失败") },
                text = { Text("错误：${cs.error}") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearClearState() }) { Text("好的") }
                }
            )
        }
    }

    // 导出结果
    when (val es = exportState) {
        ExportState.Idle, ExportState.Running -> Unit
        is ExportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportState() },
                title = { Text("导出成功") },
                text = {
                    Text(
                        "已保存 ${es.summary.peopleCount} 位人物、${es.summary.remindersCount} 条提醒、" +
                            "${es.summary.photosCount} 张照片。\n\n可在文件管理器中查看 zip 文件。"
                    )
                },
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

    // 导入结果
    when (val im = importState) {
        ImportState.Idle, ImportState.Running -> Unit
        is ImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportState() },
                title = { Text("导入成功") },
                text = {
                    Text(
                        "已恢复 ${im.result.peopleCount} 位人物、${im.result.remindersCount} 条提醒、" +
                            "${im.result.photosRestored} 张照片。\n\n" +
                            (im.result.safetyBackupPath?.let {
                                "导入前的数据已备份在应用缓存：\n$it"
                            } ?: "注意：导入前的安全备份未能生成。")
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportState() }) { Text("好的") }
                }
            )
        }
        is ImportState.Failure -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportState() },
                title = { Text("导入失败") },
                text = { Text("错误：${im.error}\n\n当前数据未被改动。") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportState() }) { Text("好的") }
                }
            )
        }
    }

    // 添加提醒
    if (showAddDialog) {
        ReminderEditorDialog(
            existing = null,
            people = state.peopleById.values.toList(),
            onDismiss = { showAddDialog = false },
            onSave = { personId, type, title, date, adv, time, repeat, notes, isLunar ->
                viewModel.saveReminder(
                    existingId = null,
                    personId = personId,
                    type = type,
                    title = title,
                    date = date,
                    advanceDays = adv,
                    reminderTime = time,
                    repeat = repeat,
                    notes = notes,
                    isLunar = isLunar
                )
            }
        )
    }

    // 编辑提醒
    editing?.let { target ->
        ReminderEditorDialog(
            existing = target,
            people = state.peopleById.values.toList(),
            onDismiss = { editing = null },
            onSave = { personId, type, title, date, adv, time, repeat, notes, isLunar ->
                viewModel.saveReminder(
                    existingId = target.id,
                    personId = personId,
                    type = type,
                    title = title,
                    date = date,
                    advanceDays = adv,
                    reminderTime = time,
                    repeat = repeat,
                    notes = notes,
                    isLunar = isLunar
                )
            }
        )
    }

    // 删除确认
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除提醒") },
            text = { Text("确定删除「${target.title}」？\n\n删除后可在「设置 - 回收站」中恢复（保留 30 天）。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReminder(target)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
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

/**
 * 「全部提醒」卡片被 LazyColumn 拆成多段后，每一段延续同一块卡片背景。
 *
 * 与 [SectionCard] 视觉等价（那张卡片同样只有 surface 背景 + 20dp 内边距，无边框无圆角），
 * 区别只在于纵向内边距由各段自己给：首段补 20dp、末段补 20dp、中间段不给，
 * 这样多段拼起来既看不出接缝，也不必把长列表塞回一个 item 里。
 */
@Composable
private fun CardSegment(
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp)
            .padding(top = topPadding, bottom = bottomPadding)
    ) {
        content()
    }
}

@Composable
private fun StatCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$value", style = MaterialTheme.typography.displaySmall, color = WarmJournalColors.Primary)
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 即将到来行。倒计时基准的「今天」由 [SettingsUiState.today] 透传，
 * 不能在组合期自己读系统时间（跨零点不会触发重组）。
 */
@Composable
private fun UpcomingRow(item: UpcomingReminder, today: LocalDate) {
    val days = item.nextDate.toEpochDay() - today.toEpochDay()
    val countdown = when {
        days == 0L -> "今天"
        days > 0 -> "还有${days}天"
        else -> "已过${-days}天"
    }
    val dateText = formatDateText(item.reminder, item.nextDate)
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

/** 公历 M月d日；农历提醒显示农历月日 */
private fun formatDateText(reminder: Reminder, nextDate: LocalDate): String =
    if (reminder.isLunar && reminder.repeat == com.self.profile.domain.model.RepeatRule.YEARLY) {
        LunarHelper.lunarLabel(nextDate)
    } else {
        nextDate.format(DateFormats.SHORT)
    }

/** 全部提醒行。倒计时基准的「今天」由 [SettingsUiState.today] 透传，不能在组合期读系统时间。 */
@Composable
private fun AllReminderRow(
    item: AllReminder,
    today: LocalDate,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val days = item.nextDate.toEpochDay() - today.toEpochDay()
    val countdown = when {
        days == 0L -> "今天"
        days > 0 -> "还有${days}天"
        else -> "已过${-days}天"
    }
    val dateText = formatDateText(item.reminder, item.nextDate)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Avatar(initial = item.personInitial, colorHex = item.personColor, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.personName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.outline
            )
        }
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

/**
 * 备份时间展示格式：ISO 式日期 + 24 小时制时分。
 *
 * 刻意不复用 util/DateFormats：那里的 FULL 是「yyyy年M月d日」中文式、不带时分，
 * 而备份时间要能区分「今天上午导的」和「今天下午导的」，所以格式不同、也不合并。
 * 放在本文件是因为该工具类不在本任务改动范围内。
 */
private val BackupTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 把 [SettingsDataStore.setLastBackupTime] 写入的墙上时钟毫秒值格式成本地时区时间。
 * 调用方负责处理 null —— 没有备份时间时不该显示成一个像模像样的日期。
 */
private fun formatBackupTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(BackupTimeFormatter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataManageBottomSheet(
    lastBackupTime: Long?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    // null = 从没导出过，别退化成 1970 年那种「看起来有备份」的假象
    val lastBackupText = lastBackupTime?.let { formatBackupTime(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 必须可滚动：底部三个 action + 安全提示在字号放大档位下会超出 sheet 高度，
                // ModalBottomSheet 不会自动给内容加滚动，超出即裁切且用户无法触达底部的 action。
                // 顺序放在 padding 之前：滚动视口由此包含全部内容与内边距，能滚到真正的末尾；
                // 若放到 padding 之后，24dp 底距会变成永久固定占用、反而压矮可视区。
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "选择如何处理你的数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 上次备份时间：allowBackup=false 后系统不再兜底，用户自己知不知道上次导过全靠这一行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.data_last_backup_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = lastBackupText
                        ?: stringResource(R.string.data_last_backup_never),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (lastBackupText == null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (lastBackupText == null) {
                        WarmJournalColors.Primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // 数据安全提示：系统自动备份已关闭（AndroidManifest allowBackup=false），
            // 换机前只能靠这里手动导出的 zip，必须把这件事明说
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.data_security_notice_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.data_security_notice_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 导出数据
            DataManageAction(
                icon = Icons.Filled.FileDownload,
                tint = WarmJournalColors.Primary,
                title = "导出备份",
                subtitle = "人物、提醒和照片打包成 zip，保存到你选的位置",
                onClick = {
                    scope.launch { sheetState.hide() }
                    onExport()
                }
            )

            Spacer(Modifier.height(8.dp))

            // 导入数据
            DataManageAction(
                icon = Icons.Filled.RestoreFromTrash,
                tint = WarmJournalColors.Primary,
                title = "导入备份",
                subtitle = "从 zip 备份恢复（会覆盖当前数据）",
                onClick = {
                    scope.launch { sheetState.hide() }
                    onImport()
                }
            )

            Spacer(Modifier.height(8.dp))

            // 清除数据
            DataManageAction(
                icon = Icons.Filled.DeleteSweep,
                tint = MaterialTheme.colorScheme.error,
                title = "清除所有数据",
                subtitle = "永久删除人物、提醒和照片（不可撤销）",
                onClick = {
                    scope.launch { sheetState.hide() }
                    onClear()
                }
            )
        }
    }
}

@Composable
private fun DataManageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Android 13 以下无需运行时权限；13+ 检查 POST_NOTIFICATIONS */
private fun checkNotifPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
