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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.Anniversary
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.BodyData
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.CommonGround
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.PersonalBackground
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.SizeInfo
import com.self.profile.domain.model.WishItem
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.PhotosSection
import com.self.profile.ui.components.SectionCard
import com.self.profile.ui.components.TagGroup
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.DatePickerSheet
import com.self.profile.ui.components.dialogs.EditTextAreaDialog
import com.self.profile.ui.components.dialogs.EditTextDialog
import com.self.profile.ui.reminder.ReminderEditorDialog
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.DateFormats
import com.self.profile.util.LunarHelper
import com.self.profile.util.ReminderCalc
import com.self.profile.util.parseHexColor
import java.time.LocalDate
import java.util.UUID

/**
 * 人物详情（US-010）：14 模块 + 互动记录 / 共同点，全部可编辑。
 *
 * 阶段 3：编辑（基础信息 + 14 模块 + 互动 + 共同点）+ 删除
 */
@Composable
fun PersonDetailScreen(
    personId: String,
    onBack: () -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入页面时记录 lastViewed_at（用于"最近查看"排序）
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
            vm = viewModel,
            onBack = onBack
        )
    }
}

@Composable
private fun PersonContent(
    person: Person,
    reminders: List<Reminder>,
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
                item(key = "sec-background") { SectionWrap("个人背景") { BackgroundSection(person.personalBackground, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-body") { SectionWrap("身体数据") { BodySection(person.bodyData, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-size") { SectionWrap("尺码信息") { SizeSection(person.sizeInfo, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-liked") { SectionWrap("喜欢的事物") { CategorizedSection(person.thingsLiked, isLiked = true, vm = vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-disliked") { SectionWrap("不喜欢的事物") { CategorizedSection(person.thingsDisliked, isLiked = false, vm = vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-personality") { SectionWrap("性格分析") { PersonalitySection(person.personalityAnalysis, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-ideal") { SectionWrap("理想型") { IdealTypeSection(person.idealType, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-aspirations") { SectionWrap("憧憬与目标") { AspirationsSection(person.aspirations, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-wishes") { SectionWrap("愿望清单") { WishListSection(person.wishList, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-journey") { SectionWrap("人生轨迹") { LifeJourneySection(person.lifeJourney, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-photos") { SectionWrap("照片墙") {
                    PhotosSection(
                        photos = person.photos,
                        onAddPhoto = { uri -> vm.addPhoto(uri) },
                        onRemovePhoto = { id -> vm.removePhoto(id) },
                        onUpdatePhotoDescription = { id, desc -> vm.updatePhotoDescription(id, desc) }
                    )
                } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-interactions") { SectionWrap("互动记录") { InteractionsSection(person.interactionNotes, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-common") { SectionWrap("共同点") { CommonGroundsSection(person.commonGrounds, vm) } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-anniversaries") { SectionWrap("纪念日", action = { AddAnniversaryTrigger(vm) }) {
                    AnniversariesSection(person.anniversaries, vm)
                } }
                item { Spacer(Modifier.height(8.dp)) }
                item(key = "sec-reminders") {
                    SectionWrap("相关提醒", action = { showReminderEditor = true }) {
                        RemindersSection(
                            reminders = reminders,
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
private fun SectionWrap(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionCard(title = title, action = action, content = content)
    }
}

@Composable
private fun EditIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "编辑",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            onConfirm = { picked -> vm.updatePerson { it.copy(relationshipType = com.self.profile.domain.model.RelationshipType.fromLabel(picked)) } },
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

@Composable
private fun BackgroundSection(bg: PersonalBackground, vm: PersonDetailViewModel) {
    val birthday = bg.birthday?.let { LocalDate.ofEpochDay(it) }
    val constellation = birthday?.let { LunarHelper.dateToConstellation(it) }
    val lunar = birthday?.let { LunarHelper.solarToLunarShort(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditableKV("家乡", bg.hometown, vm) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(hometown = newVal)) } }
        EditableKV("现居地", bg.currentCity, vm, leadingIcon = Icons.Filled.LocationOn) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(currentCity = newVal)) } }

        var showDatePicker by remember { mutableStateOf(false) }
        if (showDatePicker) {
            DatePickerSheet(
                initialDate = birthday ?: LocalDate.now(),
                onConfirm = { date ->
                    vm.updatePerson { p ->
                        p.copy(
                            personalBackground = p.personalBackground.copy(
                                birthday = date.toEpochDay(),
                                lunarBirthday = LunarHelper.solarToLunarShort(date)
                            )
                        )
                    }
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
        ) {
            Text(
                text = "生日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birthday?.format(DateFormats.FULL) ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (lunar != null) {
                    val text = "$lunar · ${constellation ?: ""}"
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val constellationText = constellation ?: "—"
        Text(
            text = "星座：$constellationText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EditableKV("大学", bg.university, vm, leadingIcon = Icons.Filled.School) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(university = newVal)) } }
        EditableKV("专业", bg.major, vm) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(major = newVal)) } }
        EditableKV("工作", bg.job, vm, leadingIcon = Icons.Filled.Work) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(job = newVal)) } }
    }
}

@Composable
private fun EditableKV(
    label: String,
    value: String?,
    vm: PersonDetailViewModel,
    leadingIcon: ImageVector? = null,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "点击编辑",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodySection(b: BodyData, vm: PersonDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberKV("血型", b.bloodType, vm) { newVal -> vm.updatePerson { it.copy(bodyData = it.bodyData.copy(bloodType = newVal)) } }
        NumberKV("身高 (cm)", b.height?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(height = n?.toFloat())) }
        }
        NumberKV("当前体重 (kg)", b.weight?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(weight = n?.toFloat())) }
        }
        NumberKV("肩宽 (cm)", b.shoulderWidth?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(shoulderWidth = n?.toFloat())) }
        }
        NumberKV("臂展 (cm)", b.armSpan?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(armSpan = n?.toFloat())) }
        }
        NumberKV("胸围 (cm)", b.chest?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(chest = n?.toFloat())) }
        }
        NumberKV("腰围 (cm)", b.waist?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(waist = n?.toFloat())) }
        }
        NumberKV("臀围 (cm)", b.hip?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(hip = n?.toFloat())) }
        }
        NumberKV("臂围 (cm)", b.armGirth?.toInt()?.toString(), vm) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(armGirth = n?.toFloat())) }
        }
    }
}

@Composable
private fun NumberKV(
    label: String,
    value: String?,
    vm: PersonDetailViewModel,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "点击编辑",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SizeSection(s: SizeInfo, vm: PersonDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceKV("鞋码", s.shoeSize, LunarHelper.allShoeSizes, vm) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(shoeSize = newVal)) } }
        ChoiceKV("上衣尺码", s.topSize, LunarHelper.allTopSizes, vm) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(topSize = newVal)) } }
        ChoiceKV("裤子尺码", s.pantsSize, LunarHelper.allPantsSizes, vm) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(pantsSize = newVal)) } }
        ChoiceKV("裙子尺码", s.skirtSize, LunarHelper.allSkirtSizes, vm) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(skirtSize = newVal)) } }
    }
}

@Composable
private fun ChoiceKV(
    label: String,
    value: String?,
    options: List<String>,
    vm: PersonDetailViewModel,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "点击选择",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategorizedSection(items: List<CategorizedItems>, isLiked: Boolean, vm: PersonDetailViewModel) {
    if (items.isEmpty()) {
        Text(text = "点击 + 添加分类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        AddCategoryRow(isLiked, vm)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { c ->
            CategoryBlock(c, isLiked, vm)
        }
        AddCategoryRow(isLiked, vm)
    }
}

@Composable
private fun CategoryBlock(c: CategorizedItems, isLiked: Boolean, vm: PersonDetailViewModel) {
    var showAddItem by remember { mutableStateOf(false) }
    if (showAddItem) {
        EditTextDialog(
            title = "添加到「${c.category}」",
            initialValue = "",
            placeholder = "输入项目",
            onConfirm = { item -> vm.addCategoryItem(isLiked, c.category, item); showAddItem = false },
            onDismiss = { showAddItem = false }
        )
    }
    Column {
        Text(
            text = c.category,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        TagGroup(
            tags = c.items,
            onAddClick = { showAddItem = true },
            onRemoveTag = { item -> vm.removeCategoryItem(isLiked, c.category, item) }
        )
    }
}

@Composable
private fun AddCategoryRow(isLiked: Boolean, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加新分类",
            initialValue = "",
            placeholder = "分类名",
            onConfirm = { cat -> vm.addCategory(isLiked, cat); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = WarmJournalColors.Primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = "添加分类", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
    }
}

@Composable
private fun PersonalitySection(text: String?, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "性格分析",
            initialValue = text.orEmpty(),
            placeholder = "描述其性格…",
            onConfirm = { newVal ->
                vm.updatePerson { it.copy(personalityAnalysis = newVal.takeIf { v -> v.isNotBlank() }) }
            },
            onDismiss = { showDialog = false }
        )
    }
    TextBlock(
        text = text,
        bodyLarge = true,
        onClick = { showDialog = true }
    )
}

@Composable
private fun IdealTypeSection(text: String?, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "理想型",
            initialValue = text.orEmpty(),
            placeholder = "他/她理想型是…",
            onConfirm = { newVal ->
                vm.updatePerson { it.copy(idealType = newVal.takeIf { v -> v.isNotBlank() }) }
            },
            onDismiss = { showDialog = false }
        )
    }
    TextBlock(
        text = text,
        bodyLarge = true,
        onClick = { showDialog = true }
    )
}

/**
 * 长段落文本块。
 *
 * @param bodyLarge 长描述（性格分析 / 理想型）传 true，用 bodyLarge (15sp) 读着更舒服；
 *                  其它短描述默认 bodyMedium (13sp) 跟周围 KV 行对齐。
 */
@Composable
private fun TextBlock(text: String?, bodyLarge: Boolean = false, onClick: () -> Unit) {
    if (text.isNullOrBlank()) {
        Text(
            text = "点击编辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.clickable { onClick() }
        )
    } else {
        Text(
            text = text,
            style = if (bodyLarge) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
private fun AspirationsSection(list: List<Aspiration>, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加目标",
            initialValue = "",
            placeholder = "他/她的目标…",
            onConfirm = { newVal -> vm.addAspiration(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "· ${a.content}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.removeAspiration(a.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = WarmJournalColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = "添加目标", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun WishListSection(list: List<WishItem>, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加愿望",
            initialValue = "",
            placeholder = "他/她想要…",
            onConfirm = { newVal -> vm.addWishItem(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckboxCircle(checked = item.completed, onClick = {
                    vm.toggleWishItem(item.id)
                })
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.removeWishItem(item.id) }, modifier = Modifier.size(28.dp)) {
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
            Text(text = "添加愿望", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun CheckboxCircle(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (checked) WarmJournalColors.Success else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun LifeJourneySection(list: List<LifeJourneyEntry>, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AddLifeJourneyDialog(
            onConfirm = { year, title, type, desc ->
                vm.addLifeJourney(year, title, type, desc)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        list.forEach { e ->
            Row {
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = e.year, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (e.type == "教育") WarmJournalColors.Info else WarmJournalColors.Success)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = e.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = e.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.removeLifeJourney(e.id) }, modifier = Modifier.size(28.dp)) {
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
            Text(text = "添加节点", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun AddLifeJourneyDialog(
    onConfirm: (year: String, title: String, type: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("教育") }
    var description by remember { mutableStateOf("") }
    var showTypePicker by remember { mutableStateOf(false) }

    if (showTypePicker) {
        ChoicePickerDialog(
            title = "类别",
            options = listOf("教育", "工作"),
            selected = type,
            onConfirm = { picked -> type = picked; showTypePicker = false },
            onDismiss = { showTypePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加人生节点") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("年份") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showTypePicker = true }) {
                    Text(text = "类别：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(text = type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onConfirm(year.ifBlank { "2024" }, title, type, description)
                else onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun InteractionsSection(list: List<Note>, vm: PersonDetailViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "新增互动记录",
            initialValue = "",
            placeholder = "今天和他/她…",
            height = 120.dp,
            onConfirm = { newVal -> vm.addInteractionNote(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { n ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(WarmJournalColors.BgWarm)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = n.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = LocalDate.ofEpochDay(n.date).format(DateFormats.FULL),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.removeInteractionNote(n.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = WarmJournalColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = "新增互动", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun CommonGroundsSection(list: List<CommonGround>, vm: PersonDetailViewModel) {
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
                    p.copy(anniversaries = p.anniversaries + Anniversary(
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

@Composable
private fun AddAnniversaryDialog(
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showDate = true }) {
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showType = true }) {
                    Text(text = "类型：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(text = type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pickedDate = date
                if (title.isNotBlank() && pickedDate != null) onConfirm(title, pickedDate, type)
                else invalid = true
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AnniversariesSection(list: List<Anniversary>, vm: PersonDetailViewModel) {
    if (list.isEmpty()) {
        EmptyState(title = "还没有纪念日", description = "点击 + 添加重要日期")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { a ->
            var showReminderForm by remember { mutableStateOf(false) }
            if (showReminderForm) {
                com.self.profile.ui.components.dialogs.ReminderFormDialog(
                    onConfirm = { advanceDays, time, repeat, notes ->
                        vm.addReminderForAnniversary(a.id, advanceDays, time, repeat, notes)
                        showReminderForm = false
                    },
                    onDismiss = { showReminderForm = false }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = a.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = LocalDate.ofEpochDay(a.date).format(DateFormats.FULL) + " · ${a.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showReminderForm = true }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "提醒", tint = WarmJournalColors.Primary)
                }
                IconButton(
                    onClick = {
                        vm.updatePerson { p ->
                            p.copy(anniversaries = p.anniversaries.filter { it.id != a.id })
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除纪念日",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 相关提醒：展示该人物关联的提醒，可编辑 / 开关 / 删除（Phase 2 补完，
 * 此前 ViewModel 里 combine 了 reminders 但 UI 从未渲染）。
 */
@Composable
private fun RemindersSection(
    reminders: List<Reminder>,
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
            val days = nextDate.toEpochDay() - LocalDate.now().toEpochDay()
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

/**
 * 颜色选择器（与 SelfDetailScreen 内部相同）。
 */
@Composable
private fun ColorPickerDialog(
    currentColor: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = listOf(
        "#C4785B", "#D99B82", "#EBC8B8", "#F5E6DD",
        "#6B8EAE", "#6B9E78", "#D4A24E", "#A65D42"
    )
    var selected by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择头像颜色") },
        text = {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                palette.forEach { hex ->
                    val isCurrent = hex.equals(selected, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex) ?: Color.Gray)
                            .clickable { selected = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
