package com.self.profile.ui.self

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
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
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.PersonalBackground
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.SelfModule
import com.self.profile.domain.model.SizeInfo
import com.self.profile.domain.model.WishItem
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.NumberStepper
import com.self.profile.ui.components.PhotosSection
import com.self.profile.ui.components.SectionCard
import com.self.profile.ui.components.TagGroup
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.DatePickerSheet
import com.self.profile.ui.components.dialogs.EditTextAreaDialog
import com.self.profile.ui.components.dialogs.EditTextDialog
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.LunarHelper
import com.self.profile.util.parseHexColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * 我的档案（US-006）：14 个模块的查看 + 编辑。
 *
 * @param scrollTo 可选，跳转到此 screen 时自动滚到指定 section：
 *  - "notes"          随想笔记
 *  - "anniversaries"  纪念日
 *  - null             不滚动
 */
@Composable
fun SelfDetailScreen(
    scrollTo: String? = null,
    viewModel: SelfEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SelfDetailUiState.Loading, SelfDetailUiState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "加载中…", style = MaterialTheme.typography.bodyLarge)
            }
        }
        is SelfDetailUiState.Loaded -> SelfContent(
            person = s.self,
            viewModel = viewModel,
            scrollTo = scrollTo
        )
    }
}

@Composable
private fun SelfContent(
    person: Person,
    viewModel: SelfEditViewModel,
    scrollTo: String?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state as? SelfDetailUiState.Loaded
    val moduleOrder = loaded?.moduleOrder ?: SelfModule.defaultOrder
    val orderedModules = remember(moduleOrder) {
        moduleOrder.mapNotNull { idx -> SelfModule.values().getOrNull(idx - 1) }
    }
    var editOrderMode by rememberSaveable { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    // 当 IdentityHeader 滚出屏幕外（firstVisibleItemIndex > 0）时显示浮起副本
    val showStickyHeader by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    // scrollTo 滚动：等首帧渲染完成再 animateScrollToItem
    LaunchedEffect(scrollTo, loaded) {
        if (scrollTo == null || loaded == null) return@LaunchedEffect
        val targetModule = when (scrollTo) {
            "notes" -> SelfModule.NOTES
            "anniversaries" -> SelfModule.ANNIVERSARIES
            else -> null
        } ?: return@LaunchedEffect
        val moduleIdx = orderedModules.indexOf(targetModule)
        if (moduleIdx >= 0) {
            // LazyColumn 里：item 0 = IdentityHeader, item 1 = Spacer, items() 从 2 开始
            // 所以 module i 的 item index = i + 2
            delay(120)  // 等首帧布局完成
            lazyListState.animateScrollToItem(index = moduleIdx + 2)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                IdentityHeader(
                    person = person,
                    vm = viewModel,
                    editOrderMode = editOrderMode,
                    onToggleEditOrder = { editOrderMode = !editOrderMode },
                    onResetOrder = { viewModel.resetModuleOrder() }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }

        items(
            count = orderedModules.size,
            key = { index -> orderedModules[index].name }
        ) { index ->
            val module = orderedModules[index]
            SelfModuleSlot(
                module = module,
                person = person,
                vm = viewModel,
                editOrderMode = editOrderMode,
                isFirst = index == 0,
                isLast = index == orderedModules.lastIndex,
                onMoveUp = { viewModel.moveModule(index, index - 1) },
                onMoveDown = { viewModel.moveModule(index, index + 1) }
            )
            if (index < orderedModules.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
        }

        // Sticky overlay：IdentityHeader 是 item 0，滚出后浮起一份
        if (showStickyHeader) {
            IdentityHeader(
                person = person,
                vm = viewModel,
                editOrderMode = editOrderMode,
                onToggleEditOrder = { editOrderMode = !editOrderMode },
                onResetOrder = { viewModel.resetModuleOrder() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
private fun SelfModuleSlot(
    module: SelfModule,
    person: Person,
    vm: SelfEditViewModel,
    editOrderMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        if (editOrderMode) {
            ReorderHandle(
                isFirst = isFirst,
                isLast = isLast,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SectionWrap(module.title, moduleAction(module, vm)) {
                renderSelfModule(module, person, vm)
            }
        }
    }
}

@Composable
private fun moduleAction(module: SelfModule, vm: SelfEditViewModel): (@Composable () -> Unit)? {
    return if (module == SelfModule.ANNIVERSARIES) {
        { AddAnniversaryTrigger(vm) }
    } else null
}

@Composable
private fun renderSelfModule(module: SelfModule, person: Person, vm: SelfEditViewModel) {
    when (module) {
        SelfModule.BACKGROUND -> BackgroundSection(person.personalBackground, vm)
        SelfModule.BODY -> BodySection(person.bodyData, vm)
        SelfModule.SIZE -> SizeSection(person.sizeInfo, vm)
        SelfModule.LIKED -> CategorizedSection(person.thingsLiked, isLiked = true, vm = vm)
        SelfModule.DISLIKED -> CategorizedSection(person.thingsDisliked, isLiked = false, vm = vm)
        SelfModule.PERSONALITY -> PersonalitySection(person.personalityAnalysis, vm)
        SelfModule.IDEAL -> IdealTypeSection(person.idealType, vm)
        SelfModule.ASPIRATIONS -> AspirationsSection(person.aspirations, vm)
        SelfModule.WISHES -> WishListSection(person.wishList, vm)
        SelfModule.LIFE_JOURNEY -> LifeJourneySection(person.lifeJourney, vm)
        SelfModule.NOTES -> NotesSection(person.thoughtNotes, vm)
        SelfModule.PHOTOS -> PhotosSection(
            photos = person.photos,
            onAddPhoto = { uri -> vm.addPhoto(uri) },
            onRemovePhoto = { id -> vm.removePhoto(id) },
            onUpdatePhotoDescription = { id, desc -> vm.updatePhotoDescription(id, desc) }
        )
        SelfModule.ANNIVERSARIES -> AnniversariesSection(person.anniversaries, vm)
    }
}

@Composable
private fun ReorderHandle(
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移", tint = if (isFirst) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移", tint = if (isLast) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionWrap(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
private fun IdentityHeader(
    person: Person,
    vm: SelfEditViewModel,
    editOrderMode: Boolean,
    onToggleEditOrder: () -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showMbtiDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

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
    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = person.avatarColor,
            onConfirm = { newColor -> vm.updatePerson { it.copy(avatarColor = newColor) } },
            onDismiss = { showColorDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        // 右上角排序按钮
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editOrderMode) {
                TextButton(onClick = onResetOrder) {
                    Text("重置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onToggleEditOrder) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = if (editOrderMode) "完成排序" else "调整模块顺序",
                    tint = if (editOrderMode) WarmJournalColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.clickable { showColorDialog = true }) {
                Avatar(
                    initial = person.avatarInitial,
                    colorHex = person.avatarColor,
                    size = 72.dp
                )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!person.mbti.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(WarmJournalColors.PrimaryLightest)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = person.mbti!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmJournalColors.PrimaryDarker
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                EditIcon(onClick = { showMbtiDialog = true })
            }
            Spacer(Modifier.height(8.dp))
            val basic = listOfNotNull(
                person.bloodType?.let { "血型 $it" },
                person.bodyData.height?.let { "${it.toInt()} cm" },
                person.bodyData.weight?.let { "${it.toInt()} kg" }
            ).joinToString(" | ")
            if (basic.isNotBlank()) {
                Text(
                    text = basic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BackgroundSection(bg: PersonalBackground, vm: SelfEditViewModel) {
    val birthday = bg.birthday?.let { LocalDate.ofEpochDay(it) }
    val constellation = birthday?.let { LunarHelper.dateToConstellation(it) }
    val lunar = birthday?.let { LunarHelper.solarToLunarShort(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditableKV("家乡", bg.hometown, vm, leadingIcon = Icons.Filled.Home) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(hometown = newVal)) } }
        EditableKV("现居地", bg.currentCity, vm, leadingIcon = Icons.Filled.LocationOn) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(currentCity = newVal)) } }

        // 生日（含农历 + 星座）
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
            LeadingIcon(Icons.Filled.Cake)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "生日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birthday?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: "—",
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LeadingIcon(Icons.Filled.AutoAwesome)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "星座：$constellationText（自动计算）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EditableKV("幸运数字", bg.luckyNumbers.joinToString(", "), vm, leadingIcon = Icons.Filled.Star) { newVal: String ->
            val list = newVal.split(",").map { it.trim() }.filter { it.isNotBlank() }
            vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(luckyNumbers = list)) }
        }
        EditableKV("大学", bg.university, vm, leadingIcon = Icons.Filled.School) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(university = newVal)) } }
        EditableKV("专业", bg.major, vm, leadingIcon = Icons.Filled.MenuBook) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(major = newVal)) } }
        EditableKV("工作", bg.job, vm, leadingIcon = Icons.Filled.Work) { newVal -> vm.updatePerson { it.copy(personalBackground = it.personalBackground.copy(job = newVal)) } }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector?) {
    // 固定 20dp 占位：有图标就画，没有就留空，确保标签列永远对齐
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun EditableKV(
    label: String,
    value: String?,
    vm: SelfEditViewModel,
    leadingIcon: ImageVector? = null,
    onSave: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "编辑$label",
            initialValue = value.orEmpty(),
            placeholder = "点击输入",
            onConfirm = { newVal ->
                val nonEmpty = newVal.trim()
                if (nonEmpty.isNotEmpty()) onSave(nonEmpty)
            },
            onDismiss = { showDialog = false }
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        LeadingIcon(leadingIcon)
        Spacer(Modifier.width(6.dp))
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
private fun BodySection(b: BodyData, vm: SelfEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberKV("血型", b.bloodType, vm, leadingIcon = Icons.Filled.ColorLens) { newVal -> vm.updatePerson { it.copy(bodyData = it.bodyData.copy(bloodType = newVal)) } }
        NumberKV("身高 (cm)", b.height?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(height = n?.toFloat())) }
        }
        NumberKV("当前体重 (kg)", b.weight?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.MonitorWeight) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(weight = n?.toFloat())) }
        }
        NumberKV("肩宽 (cm)", b.shoulderWidth?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(shoulderWidth = n?.toFloat())) }
        }
        NumberKV("臂展 (cm)", b.armSpan?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(armSpan = n?.toFloat())) }
        }
        NumberKV("胸围 (cm)", b.chest?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(chest = n?.toFloat())) }
        }
        NumberKV("腰围 (cm)", b.waist?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(waist = n?.toFloat())) }
        }
        NumberKV("臀围 (cm)", b.hip?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(hip = n?.toFloat())) }
        }
        NumberKV("臂围 (cm)", b.armGirth?.toInt()?.toString(), vm, leadingIcon = Icons.Filled.Straighten) { newVal ->
            val n = newVal?.toIntOrNull()
            vm.updatePerson { it.copy(bodyData = it.bodyData.copy(armGirth = n?.toFloat())) }
        }
    }
}

@Composable
private fun NumberKV(
    label: String,
    value: String?,
    vm: SelfEditViewModel,
    leadingIcon: ImageVector? = null,
    onSave: (String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "编辑$label",
            initialValue = value.orEmpty(),
            placeholder = "数字",
            keyboardType = KeyboardType.Number,
            onConfirm = { newVal -> onSave(newVal.takeIf { it.isNotBlank() }) },
            onDismiss = { showDialog = false }
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        LeadingIcon(leadingIcon)
        Spacer(Modifier.width(6.dp))
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
private fun SizeSection(s: SizeInfo, vm: SelfEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceKV("鞋码", s.shoeSize, LunarHelper.allShoeSizes, vm, leadingIcon = Icons.Filled.DirectionsWalk) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(shoeSize = newVal)) } }
        ChoiceKV("上衣尺码", s.topSize, LunarHelper.allTopSizes, vm, leadingIcon = Icons.Filled.Checkroom) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(topSize = newVal)) } }
        ChoiceKV("裤子尺码", s.pantsSize, LunarHelper.allPantsSizes, vm, leadingIcon = Icons.Filled.Checkroom) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(pantsSize = newVal)) } }
        ChoiceKV("裙子尺码", s.skirtSize, LunarHelper.allSkirtSizes, vm, leadingIcon = Icons.Filled.Checkroom) { newVal -> vm.updatePerson { it.copy(sizeInfo = it.sizeInfo.copy(skirtSize = newVal)) } }
    }
}

@Composable
private fun ChoiceKV(
    label: String,
    value: String?,
    options: List<String>,
    vm: SelfEditViewModel,
    leadingIcon: ImageVector? = null,
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
        LeadingIcon(leadingIcon)
        Spacer(Modifier.width(6.dp))
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
private fun CategorizedSection(items: List<CategorizedItems>, isLiked: Boolean, vm: SelfEditViewModel) {
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
private fun CategoryBlock(c: CategorizedItems, isLiked: Boolean, vm: SelfEditViewModel) {
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
private fun AddCategoryRow(isLiked: Boolean, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加新分类",
            initialValue = "",
            placeholder = "分类名（如 食物）",
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
        Text(
            text = "添加分类",
            style = MaterialTheme.typography.bodySmall,
            color = WarmJournalColors.Primary
        )
    }
}

@Composable
private fun PersonalitySection(text: String?, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "性格分析",
            initialValue = text.orEmpty(),
            placeholder = "用几段话说说你是一个什么样的人…",
            onConfirm = { newVal -> vm.updatePerson { it.copy(personalityAnalysis = newVal.takeIf { v -> v.isNotBlank() }) } },
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
private fun IdealTypeSection(text: String?, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "理想型画像",
            initialValue = text.orEmpty(),
            placeholder = "描述你心目中理想的人…",
            onConfirm = { newVal -> vm.updatePerson { it.copy(idealType = newVal.takeIf { v -> v.isNotBlank() }) } },
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
private fun AspirationsSection(list: List<Aspiration>, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加目标",
            initialValue = "",
            placeholder = "我想…",
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
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = WarmJournalColors.Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = "添加目标", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun WishListSection(list: List<WishItem>, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加愿望",
            initialValue = "",
            placeholder = "我想…",
            onConfirm = { newVal -> vm.addWishItem(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckboxCircle(checked = item.completed, onClick = { vm.toggleWishItem(item.id) })
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
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = WarmJournalColors.Primary,
                modifier = Modifier.size(16.dp)
            )
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
private fun LifeJourneySection(list: List<LifeJourneyEntry>, vm: SelfEditViewModel) {
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
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = WarmJournalColors.Primary,
                modifier = Modifier.size(16.dp)
            )
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
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("年份") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showTypePicker = true }
                ) {
                    Text(text = "类别：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(text = type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onConfirm(year.ifBlank { "2024" }, title, type, description)
                    else onDismiss()
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NotesSection(list: List<Note>, vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "新增笔记",
            initialValue = "",
            placeholder = "此刻的想法…",
            height = 140.dp,
            onConfirm = { newVal -> vm.addNote(newVal); showDialog = false },
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
                        text = LocalDate.ofEpochDay(n.date).format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.removeNote(n.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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
            Text(text = "新增笔记", style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
        }
    }
}

@Composable
private fun AddAnniversaryTrigger(vm: SelfEditViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AddAnniversaryDialog(
            onConfirm = { title, date, type ->
                vm.addAnniversary(title, date.toEpochDay(), type)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
    IconButton(onClick = { showDialog = true }) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "添加纪念日",
            tint = WarmJournalColors.Primary
        )
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

    if (showDate) {
        DatePickerSheet(
            initialDate = date ?: LocalDate.now(),
            onConfirm = { d -> date = d; showDate = false },
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
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showDate = true }
                ) {
                    Text(text = "日期：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = date?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: "选择",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (date == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
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
                    if (title.isNotBlank() && date != null) onConfirm(title, date!!, type)
                    else onDismiss()
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AnniversariesSection(list: List<Anniversary>, vm: SelfEditViewModel) {
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
                        text = LocalDate.ofEpochDay(a.date).format(DateTimeFormatter.ofPattern("yyyy年M月d日")) + " · ${a.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showReminderForm = true }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "提醒", tint = WarmJournalColors.Primary)
                }
            }
        }
    }
}

/**
 * 简单颜色选择器（8 个预设色，对应关系图谱 4 个 + 中性 4 个）。
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
            FlowRow(
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
                            .clickable { selected = hex }
                            .then(
                                if (isCurrent) Modifier
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex) ?: Color.Gray)
                                else Modifier
                            ),
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
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
