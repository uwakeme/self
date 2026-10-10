package com.self.profile.ui.people

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.RelationshipType
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EditIcon
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.PhotosSection
import com.self.profile.ui.components.SectionWrap
import com.self.profile.ui.components.TagGroup
import com.self.profile.ui.components.dialogs.AddAnniversaryDialog
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.ColorPickerDialog
import com.self.profile.ui.components.dialogs.EditTextDialog
import com.self.profile.ui.reminder.ReminderEditorDialog
import com.self.profile.ui.components.sections.AnniversariesSection
import com.self.profile.ui.components.sections.AspirationsSection
import com.self.profile.ui.components.sections.BackgroundSection
import com.self.profile.ui.components.sections.BodySection
import com.self.profile.ui.components.sections.CategorizedSection
import com.self.profile.ui.components.sections.IdealTypeSection
import com.self.profile.ui.components.sections.LifeJourneySection
import com.self.profile.ui.components.sections.NotesListSection
import com.self.profile.ui.components.sections.PersonalitySection
import com.self.profile.ui.components.sections.SizeSection
import com.self.profile.ui.components.sections.WishListSection
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.DateFormats
import com.self.profile.util.LunarHelper
import com.self.profile.util.ReminderCalc
import java.time.LocalDate
import java.util.UUID

/**
 * 人物详情（US-010）：14 模块 + 互动记录 / 共同点，全部可编辑。
 *
 * 模块渲染本体在 ui/components/sections（与我的档案共用），
 * 本文件只保留：PersonHeader、相关提醒列表、删除/提醒弹窗编排。
 */
@Composable
fun PersonDetailScreen(
    personId: String,
    onBack: () -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入页面时记录 last_viewed_at（用于"最近查看"排序）
    LaunchedEffect(personId) { viewModel.markViewed() }

    when (val s = state) {
        PersonDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", style = MaterialTheme.typography.bodyLarge)
            }
        }
        PersonDetailUiState.NotFound -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("人物不存在", style = MaterialTheme.typography.bodyLarge)
            }
        }
        is PersonDetailUiState.Loaded -> PersonContent(
            person = s.person,
            reminders = s.reminders,
            today = s.today,
            vm = viewModel,
            onBack = onBack
        )
    }
}

@Composable
private fun PersonContent(
    person: Person,
    reminders: List<Reminder>,
    today: LocalDate,
    vm: PersonDetailViewModel,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReminderEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var pendingDeleteReminder by remember { mutableStateOf<Reminder?>(null) }
    val lazyListState = rememberLazyListState()
    val showStickyHeader by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除人物") },
            text = { Text("确定删除「${person.name}」吗？\n\n人物和关联提醒将移入回收站（保留 30 天），可在「设置 - 回收站」中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete(onBack)
                }) { Text("删除", color = WarmJournalColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    // 添加相关提醒（绑定当前人物）
    if (showReminderEditor) {
        ReminderEditorDialog(
            existing = null,
            people = listOf(person),
            onDismiss = { showReminderEditor = false },
            onSave = { _, type, title, date, adv, time, repeat, notes, isLunar ->
                vm.saveReminder(null, type, title, date, adv, time, repeat, notes, isLunar)
            }
        )
    }
    // 编辑相关提醒
    editingReminder?.let { target ->
        ReminderEditorDialog(
            existing = target,
            people = listOf(person),
            onDismiss = { editingReminder = null },
            onSave = { _, type, title, date, adv, time, repeat, notes, isLunar ->
                vm.saveReminder(
                    target.id, type, title, date, adv, time, repeat, notes, isLunar
                )
            }
        )
    }
    // 删除提醒确认
    pendingDeleteReminder?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteReminder = null },
            title = { Text("删除提醒") },
            text = { Text("确定删除「${target.title}」？\n\n删除后可在「设置 - 回收站」中恢复（保留 30 天）。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteReminder(target)
                    pendingDeleteReminder = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteReminder = null }) { Text("取消") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 顶部条
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (!person.isSelf) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "person-header") { PersonHeader(person, vm) }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-background") {
                    SectionWrap("个人背景") {
                        BackgroundSection(person.personalBackground, isSelf = false) { savor ->
                            vm.updatePerson { p -> p.copy(personalBackground = savor(p.personalBackground)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-body") {
                    SectionWrap("身体数据") {
                        BodySection(person.bodyData) { savor ->
                            vm.updatePerson { p -> p.copy(bodyData = savor(p.bodyData)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-size") {
                    SectionWrap("尺码信息") {
                        SizeSection(person.sizeInfo) { savor ->
                            vm.updatePerson { p -> p.copy(sizeInfo = savor(p.sizeInfo)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-liked") {
                    SectionWrap("喜欢的事物") {
                        CategorizedSection(
                            items = person.thingsLiked,
                            onAddItem = { cat, item -> vm.addCategoryItem(true, cat, item) },
                            onRemoveItem = { cat, item -> vm.removeCategoryItem(true, cat, item) },
                            onAddCategory = { vm.addCategory(true, it) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-disliked") {
                    SectionWrap("不喜欢的事物") {
                        CategorizedSection(
                            items = person.thingsDisliked,
                            onAddItem = { cat, item -> vm.addCategoryItem(false, cat, item) },
                            onRemoveItem = { cat, item -> vm.removeCategoryItem(false, cat, item) },
                            onAddCategory = { vm.addCategory(false, it) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-personality") {
                    SectionWrap("性格分析") {
                        PersonalitySection(person.personalityAnalysis) { v ->
                            vm.updatePerson { it.copy(personalityAnalysis = v) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-ideal") {
                    SectionWrap("理想型") {
                        IdealTypeSection(person.idealType) { v ->
                            vm.updatePerson { it.copy(idealType = v) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-aspirations") {
                    SectionWrap("憧憬与目标") {
                        AspirationsSection(
                            list = person.aspirations,
                            onAdd = vm::addAspiration,
                            onRemove = vm::removeAspiration
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-wishes") {
                    SectionWrap("愿望清单") {
                        WishListSection(
                            list = person.wishList,
                            onAdd = vm::addWishItem,
                            onToggle = vm::toggleWishItem,
                            onRemove = vm::removeWishItem
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-journey") {
                    SectionWrap("人生轨迹") {
                        LifeJourneySection(
                            list = person.lifeJourney,
                            onAdd = { y, t, ty, d -> vm.addLifeJourney(y, t, ty, d) },
                            onRemove = vm::removeLifeJourney
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-photos") {
                    SectionWrap("照片墙") {
                        PhotosSection(
                            photos = person.photos,
                            onAddPhoto = { uri -> vm.addPhoto(uri) },
                            onRemovePhoto = { id -> vm.removePhoto(id) },
                            onUpdatePhotoDescription = { id, desc -> vm.updatePhotoDescription(id, desc) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-interactions") {
                    SectionWrap("互动记录") {
                        NotesListSection(
                            list = person.interactionNotes,
                            dialogTitle = "新增互动记录",
                            placeholder = "今天和他/她…",
                            addLabel = "新增互动",
                            areaHeight = 120.dp,
                            onAdd = vm::addInteractionNote,
                            onRemove = vm::removeInteractionNote
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-common") {
                    SectionWrap("共同点") {
                        CommonGroundsSection(person.commonGrounds, vm)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-anniversaries") {
                    SectionWrap("纪念日", action = { AddAnniversaryTrigger(vm) }) {
                        AnniversariesSection(
                            list = person.anniversaries,
                            onAddReminder = { annId, adv, time, repeat, notes ->
                                vm.addReminderForAnniversary(annId, adv, time, repeat, notes)
                            },
                            onDelete = { id ->
                                vm.updatePerson { p ->
                                    p.copy(anniversaries = p.anniversaries.filter { it.id != id })
                                }
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-reminders") {
                    SectionWrap("相关提醒", action = { showReminderEditor = true }) {
                        RemindersSection(
                            reminders = reminders,
                            today = today,
                            onEdit = { editingReminder = it },
                            onToggle = { id, enabled -> vm.toggleReminder(id, enabled) },
                            onDelete = { pendingDeleteReminder = it }
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }

            // Sticky overlay：PersonHeader 是 item 0，滚出后浮起一份
            if (showStickyHeader) {
                PersonHeader(
                    person, vm,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
private fun PersonHeader(person: Person, vm: PersonDetailViewModel, modifier: Modifier = Modifier) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showMbtiDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }

    if (showNameDialog) {
        EditTextDialog(
            title = "编辑姓名",
            initialValue = person.name,
            onConfirm = { newName -> vm.updatePerson { it.copy(name = newName) } },
            onDismiss = { showNameDialog = false }
        )
    }
    if (showMbtiDialog) {
        ChoicePickerDialog(
            title = "选择 MBTI",
            options = listOf("") + LunarHelper.allMbti,
            selected = person.mbti,
            onConfirm = { picked -> vm.updatePerson { it.copy(mbti = picked.takeIf { p -> p.isNotBlank() }) } },
            onDismiss = { showMbtiDialog = false }
        )
    }
    if (showTypeDialog) {
        ChoicePickerDialog(
            title = "选择关系",
            options = LunarHelper.allRelationshipTypes,
            selected = person.relationshipType.label,
            onConfirm = { picked -> vm.updatePerson { it.copy(relationshipType = RelationshipType.fromLabel(picked)) } },
            onDismiss = { showTypeDialog = false }
        )
    }
    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = person.avatarColor,
            onConfirm = { newColor -> vm.updatePerson { it.copy(avatarColor = newColor) } },
            onDismiss = { showColorDialog = false }
        )
    }
    if (showTagInput) {
        EditTextDialog(
            title = "添加标签",
            initialValue = "",
            placeholder = "如：球友、书友",
            onConfirm = { tag ->
                val t = tag.trim()
                if (t.isNotBlank() && t !in person.tags) {
                    vm.updatePerson { it.copy(tags = it.tags + t) }
                }
                showTagInput = false
            },
            onDismiss = { showTagInput = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.clickable { showColorDialog = true }) {
            Avatar(initial = person.avatarInitial, colorHex = person.avatarColor, size = 72.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            EditIcon(onClick = { showNameDialog = true })
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!person.mbti.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(WarmJournalColors.PrimaryLightest)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = person.mbti.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmJournalColors.PrimaryDarker
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(WarmJournalColors.BgSurface)
                    .clickable { showTypeDialog = true }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = person.relationshipType.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            person.bloodType?.takeIf { it.isNotBlank() }?.let { bt ->
                Text(
                    text = "血型 $bt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            EditIcon(onClick = { showMbtiDialog = true })
        }
        if (person.tags.isNotEmpty() || !person.isSelf) {
            Spacer(Modifier.height(8.dp))
            TagGroup(
                tags = person.tags,
                onAddClick = { showTagInput = true },
                onRemoveTag = { tag -> vm.updatePerson { it.copy(tags = it.tags - tag) } }
            )
        }
    }
}

/** 共同点（仅人物详情有，随想笔记是我的档案专属） */
@Composable
private fun CommonGroundsSection(
    list: List<com.self.profile.domain.model.CommonGround>,
    vm: PersonDetailViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加共同点",
            initialValue = "",
            placeholder = "我们都喜欢…",
            onConfirm = { newVal -> vm.addCommonGround(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "· ${c.content}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.removeCommonGround(c.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = WarmJournalColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = "添加共同点", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun AddAnniversaryTrigger(vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AddAnniversaryDialog(
            onConfirm = { title, date, type ->
                vm.updatePerson { p ->
                    p.copy(anniversaries = p.anniversaries + com.self.profile.domain.model.Anniversary(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        date = date.toEpochDay(),
                        type = type
                    ))
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
    IconButton(onClick = { showDialog = true }) {
        Icon(Icons.Filled.Add, contentDescription = "添加纪念日", tint = WarmJournalColors.Primary)
    }
}

/**
 * 相关提醒：展示该人物关联的提醒，可编辑 / 开关 / 删除（Phase 2 补完，
 * 此前 ViewModel 里 combine 了 reminders 但 UI 从未渲染）。
 *
 * @param today 倒计时基准的「今天」，由 VM 透传；不在组合期调 `LocalDate.now()`
 *   ——组合期读到的值不会因跨零点变化，state 结构相等时也不会重组，倒计时会停在昨天。
 */
@Composable
private fun RemindersSection(
    reminders: List<Reminder>,
    today: LocalDate,
    onEdit: (Reminder) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    if (reminders.isEmpty()) {
        EmptyState(title = "暂无相关提醒", description = "点击右上角 + 为他/她添加生日或纪念日提醒")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { r ->
            val nextDate = ReminderCalc.nextOccurrenceDate(r)
            val days = nextDate.toEpochDay() - today.toEpochDay()
            val countdown = when {
                days == 0L -> "今天"
                days > 0 -> "还有${days}天"
                else -> "已过${-days}天"
            }
            val dateText = if (r.isLunar && r.repeat == com.self.profile.domain.model.RepeatRule.YEARLY) {
                LunarHelper.lunarLabel(nextDate)
            } else {
                nextDate.format(DateFormats.SHORT)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable { onEdit(r) }) {
                    Text(
                        text = r.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${r.type.label} · $dateText · $countdown · 提前${r.advanceDays}天 ${r.reminderTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = r.enabled,
                    onCheckedChange = { onToggle(r.id, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WarmJournalColors.Primary,
                        checkedTrackColor = WarmJournalColors.PrimaryLightest
                    )
                )
                IconButton(onClick = { onDelete(r) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除提醒",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
