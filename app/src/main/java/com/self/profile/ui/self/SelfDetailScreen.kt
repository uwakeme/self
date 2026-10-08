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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.SelfModule
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EditIcon
import com.self.profile.ui.components.PhotosSection
import com.self.profile.ui.components.SectionWrap
import com.self.profile.ui.components.dialogs.AddAnniversaryDialog
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.components.dialogs.ColorPickerDialog
import com.self.profile.ui.components.dialogs.EditTextDialog
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
import com.self.profile.util.LunarHelper
import kotlinx.coroutines.delay

/**
 * 我的档案（US-006）：13 个模块的查看 + 编辑。
 *
 * 模块渲染本体在 ui/components/sections（与人物详情共用），
 * 本文件只保留：模块排序（US-016）、scrollTo、IdentityHeader。
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
        SelfModule.orderedFrom(moduleOrder)
    }
    var editOrderMode by rememberSaveable { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    // 当 IdentityHeader 滚出屏幕外（firstVisibleItemIndex > 0）时显示浮起副本
    val showStickyHeader by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    // scrollTo 滚动：只对同一个 scrollTo 值执行一次。
    // 用"已处理"标志位而不是把 loaded 放进 key——loaded 每次编辑都会产生新实例，
    // 那样用户此后每次保存都会被拽回目标 section。
    val scrollHandled = remember(scrollTo) { mutableStateOf(false) }
    LaunchedEffect(scrollTo, loaded) {
        if (scrollTo == null || loaded == null || scrollHandled.value) return@LaunchedEffect
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
            scrollHandled.value = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "identity-header") {
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
        SelfModule.BACKGROUND -> BackgroundSection(person.personalBackground, isSelf = true) { savor ->
            vm.updatePerson { p -> p.copy(personalBackground = savor(p.personalBackground)) }
        }
        SelfModule.BODY -> BodySection(person.bodyData) { savor ->
            vm.updatePerson { p -> p.copy(bodyData = savor(p.bodyData)) }
        }
        SelfModule.SIZE -> SizeSection(person.sizeInfo) { savor ->
            vm.updatePerson { p -> p.copy(sizeInfo = savor(p.sizeInfo)) }
        }
        SelfModule.LIKED -> CategorizedSection(
            items = person.thingsLiked,
            onAddItem = { cat, item -> vm.addCategoryItem(true, cat, item) },
            onRemoveItem = { cat, item -> vm.removeCategoryItem(true, cat, item) },
            onAddCategory = { vm.addCategory(true, it) }
        )
        SelfModule.DISLIKED -> CategorizedSection(
            items = person.thingsDisliked,
            onAddItem = { cat, item -> vm.addCategoryItem(false, cat, item) },
            onRemoveItem = { cat, item -> vm.removeCategoryItem(false, cat, item) },
            onAddCategory = { vm.addCategory(false, it) }
        )
        SelfModule.PERSONALITY -> PersonalitySection(person.personalityAnalysis) { v ->
            vm.updatePerson { it.copy(personalityAnalysis = v) }
        }
        SelfModule.IDEAL -> IdealTypeSection(person.idealType) { v ->
            vm.updatePerson { it.copy(idealType = v) }
        }
        SelfModule.ASPIRATIONS -> AspirationsSection(
            list = person.aspirations,
            onAdd = vm::addAspiration,
            onRemove = vm::removeAspiration
        )
        SelfModule.WISHES -> WishListSection(
            list = person.wishList,
            onAdd = vm::addWishItem,
            onToggle = vm::toggleWishItem,
            onRemove = vm::removeWishItem
        )
        SelfModule.LIFE_JOURNEY -> LifeJourneySection(
            list = person.lifeJourney,
            onAdd = { y, t, ty, d -> vm.addLifeJourney(y, t, ty, d) },
            onRemove = vm::removeLifeJourney
        )
        SelfModule.NOTES -> NotesListSection(
            list = person.thoughtNotes,
            dialogTitle = "新增笔记",
            placeholder = "此刻的想法…",
            addLabel = "新增笔记",
            areaHeight = 140.dp,
            onAdd = vm::addNote,
            onRemove = vm::removeNote
        )
        SelfModule.PHOTOS -> PhotosSection(
            photos = person.photos,
            onAddPhoto = { uri -> vm.addPhoto(uri) },
            onRemovePhoto = { id -> vm.removePhoto(id) },
            onUpdatePhotoDescription = { id, desc -> vm.updatePhotoDescription(id, desc) }
        )
        SelfModule.ANNIVERSARIES -> AnniversariesSection(
            list = person.anniversaries,
            onAddReminder = { annId, adv, time, repeat, notes ->
                vm.addReminderForAnniversary(annId, adv, time, repeat, notes)
            },
            onDelete = vm::removeAnniversary
        )
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
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(WarmJournalColors.PrimaryLightest, WarmJournalColors.Bg)
                )
            )
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
                            text = person.mbti.orEmpty(),
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
