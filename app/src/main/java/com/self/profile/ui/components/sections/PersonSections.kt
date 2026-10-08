package com.self.profile.ui.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.self.profile.domain.model.Anniversary
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.BodyData
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.PersonalBackground
import com.self.profile.domain.model.RepeatRule
import com.self.profile.domain.model.SizeInfo
import com.self.profile.domain.model.WishItem
import com.self.profile.ui.components.CheckboxCircle
import com.self.profile.ui.components.ChoiceKV
import com.self.profile.ui.components.EditableKV
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.LeadingIcon
import com.self.profile.ui.components.NumberKV
import com.self.profile.ui.components.TagGroup
import com.self.profile.ui.components.TextBlock
import com.self.profile.ui.components.dialogs.AddLifeJourneyDialog
import com.self.profile.ui.components.dialogs.DatePickerSheet
import com.self.profile.ui.components.dialogs.EditTextAreaDialog
import com.self.profile.ui.components.dialogs.EditTextDialog
import com.self.profile.ui.components.dialogs.ReminderFormDialog
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.DateFormats
import com.self.profile.util.LunarHelper
import java.time.LocalDate

/**
 * 档案模块 Section（我的档案 / 人物详情共用）。
 *
 * 全部走回调（对齐 PhotosSection 的风格），不依赖具体 ViewModel；
 * 字段更新统一用 `onUpdate { it.copy(...) }` 形式的 savor 回调包住
 * 外层的 updatePerson。
 */

// ====== 个人背景 ======

/**
 * @param isSelf 我的档案多一行「幸运数字」
 * @param onUpdate 收到字段级 savor 函数，包进外层 Person 级 copy 后持久化
 */
@Composable
fun BackgroundSection(
    bg: PersonalBackground,
    isSelf: Boolean,
    onUpdate: (((PersonalBackground) -> PersonalBackground)) -> Unit
) {
    val birthday = bg.birthday?.let { LocalDate.ofEpochDay(it) }
    val constellation = birthday?.let { LunarHelper.dateToConstellation(it) }
    val lunar = birthday?.let { LunarHelper.solarToLunarShort(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditableKV("家乡", bg.hometown, leadingIcon = Icons.Filled.Home) { v ->
            onUpdate { it.copy(hometown = v) }
        }
        EditableKV("现居地", bg.currentCity, leadingIcon = Icons.Filled.LocationOn) { v ->
            onUpdate { it.copy(currentCity = v) }
        }

        // 生日（含农历 + 星座）
        var showDatePicker by remember { mutableStateOf(false) }
        if (showDatePicker) {
            DatePickerSheet(
                initialDate = birthday ?: LocalDate.now(),
                onConfirm = { date ->
                    onUpdate {
                        it.copy(
                            birthday = date.toEpochDay(),
                            lunarBirthday = LunarHelper.solarToLunarShort(date)
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
                    text = birthday?.format(DateFormats.FULL) ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (lunar != null) {
                    Text(
                        text = "$lunar · ${constellation ?: ""}".trimEnd('·', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LeadingIcon(Icons.Filled.AutoAwesome)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "星座：${constellation ?: "—"}（自动计算）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelf) {
            EditableKV("幸运数字", bg.luckyNumbers.joinToString(", "), leadingIcon = Icons.Filled.Star) { v: String? ->
                val list = v.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
                onUpdate { it.copy(luckyNumbers = list) }
            }
        }
        EditableKV("大学", bg.university, leadingIcon = Icons.Filled.School) { v ->
            onUpdate { it.copy(university = v) }
        }
        EditableKV("专业", bg.major, leadingIcon = Icons.Filled.MenuBook) { v ->
            onUpdate { it.copy(major = v) }
        }
        EditableKV("工作", bg.job, leadingIcon = Icons.Filled.Work) { v ->
            onUpdate { it.copy(job = v) }
        }
    }
}

// ====== 身体数据 / 尺码 ======

@Composable
fun BodySection(
    b: BodyData,
    onUpdate: (((BodyData) -> BodyData)) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberKV("血型", b.bloodType, leadingIcon = Icons.Filled.ColorLens) { v ->
            onUpdate { it.copy(bloodType = v) }
        }
        listOf(
            "身高 (cm)" to Icons.Filled.Straighten,
            "当前体重 (kg)" to Icons.Filled.MonitorWeight,
            "肩宽 (cm)" to Icons.Filled.Straighten,
            "臂展 (cm)" to Icons.Filled.Straighten,
            "胸围 (cm)" to Icons.Filled.Straighten,
            "腰围 (cm)" to Icons.Filled.Straighten,
            "臀围 (cm)" to Icons.Filled.Straighten,
            "臂围 (cm)" to Icons.Filled.Straighten
        ).forEach { (label, icon) ->
            val current = when (label) {
                "身高 (cm)" -> b.height
                "当前体重 (kg)" -> b.weight
                "肩宽 (cm)" -> b.shoulderWidth
                "臂展 (cm)" -> b.armSpan
                "胸围 (cm)" -> b.chest
                "腰围 (cm)" -> b.waist
                "臀围 (cm)" -> b.hip
                else -> b.armGirth
            }?.toInt()?.toString()
            NumberKV(label, current, leadingIcon = icon) { v ->
                val n = v?.toIntOrNull()
                val f = n?.toFloat()
                onUpdate {
                    when (label) {
                        "身高 (cm)" -> it.copy(height = f)
                        "当前体重 (kg)" -> it.copy(weight = f)
                        "肩宽 (cm)" -> it.copy(shoulderWidth = f)
                        "臂展 (cm)" -> it.copy(armSpan = f)
                        "胸围 (cm)" -> it.copy(chest = f)
                        "腰围 (cm)" -> it.copy(waist = f)
                        "臀围 (cm)" -> it.copy(hip = f)
                        else -> it.copy(armGirth = f)
                    }
                }
            }
        }
    }
}

@Composable
fun SizeSection(
    s: SizeInfo,
    onUpdate: (((SizeInfo) -> SizeInfo)) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceKV("鞋码", s.shoeSize, LunarHelper.allShoeSizes, leadingIcon = Icons.Filled.DirectionsWalk) { v ->
            onUpdate { it.copy(shoeSize = v) }
        }
        ChoiceKV("上衣尺码", s.topSize, LunarHelper.allTopSizes, leadingIcon = Icons.Filled.Checkroom) { v ->
            onUpdate { it.copy(topSize = v) }
        }
        ChoiceKV("裤子尺码", s.pantsSize, LunarHelper.allPantsSizes, leadingIcon = Icons.Filled.Checkroom) { v ->
            onUpdate { it.copy(pantsSize = v) }
        }
        ChoiceKV("裙子尺码", s.skirtSize, LunarHelper.allSkirtSizes, leadingIcon = Icons.Filled.Checkroom) { v ->
            onUpdate { it.copy(skirtSize = v) }
        }
    }
}

// ====== 喜欢 / 不喜欢的事物 ======

@Composable
fun CategorizedSection(
    items: List<CategorizedItems>,
    onAddItem: (category: String, item: String) -> Unit,
    onRemoveItem: (category: String, item: String) -> Unit,
    onAddCategory: (category: String) -> Unit
) {
    if (items.isEmpty()) {
        Text(text = "点击 + 添加分类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        AddCategoryRow(onAddCategory)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { c ->
            CategoryBlock(c, onAddItem, onRemoveItem)
        }
        AddCategoryRow(onAddCategory)
    }
}

@Composable
private fun CategoryBlock(
    c: CategorizedItems,
    onAddItem: (category: String, item: String) -> Unit,
    onRemoveItem: (category: String, item: String) -> Unit
) {
    var showAddItem by remember { mutableStateOf(false) }
    if (showAddItem) {
        EditTextDialog(
            title = "添加到「${c.category}」",
            initialValue = "",
            placeholder = "输入项目",
            onConfirm = { item -> onAddItem(c.category, item); showAddItem = false },
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
            onRemoveTag = { item -> onRemoveItem(c.category, item) }
        )
    }
}

@Composable
private fun AddCategoryRow(onAddCategory: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加新分类",
            initialValue = "",
            placeholder = "分类名（如 食物）",
            onConfirm = { cat -> onAddCategory(cat); showDialog = false },
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

// ====== 性格分析 / 理想型 ======

@Composable
fun PersonalitySection(text: String?, onSave: (String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "性格分析",
            initialValue = text.orEmpty(),
            placeholder = "用几段话描述…",
            onConfirm = { newVal -> onSave(newVal.takeIf { v -> v.isNotBlank() }) },
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
fun IdealTypeSection(text: String?, onSave: (String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = "理想型画像",
            initialValue = text.orEmpty(),
            placeholder = "描述心目中理想的人…",
            onConfirm = { newVal -> onSave(newVal.takeIf { v -> v.isNotBlank() }) },
            onDismiss = { showDialog = false }
        )
    }
    TextBlock(
        text = text,
        bodyLarge = true,
        onClick = { showDialog = true }
    )
}

// ====== 憧憬与目标 / 愿望清单 ======

@Composable
fun AspirationsSection(
    list: List<Aspiration>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加目标",
            initialValue = "",
            placeholder = "我想…",
            onConfirm = { newVal -> onAdd(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { a ->
            DeletableBulletRow(text = a.content, contentDescription = "删除目标") { onRemove(a.id) }
        }
        AddRow(label = "添加目标") { showDialog = true }
    }
}

@Composable
fun WishListSection(
    list: List<WishItem>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextDialog(
            title = "添加愿望",
            initialValue = "",
            placeholder = "想要…",
            onConfirm = { newVal -> onAdd(newVal); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
    Column {
        list.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckboxCircle(checked = item.completed, onClick = { onToggle(item.id) })
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(item.id) }, modifier = Modifier.size(28.dp)) {
                    SmallDeleteIcon(contentDescription = "删除愿望")
                }
            }
        }
        AddRow(label = "添加愿望") { showDialog = true }
    }
}

// ====== 人生轨迹 ======

@Composable
fun LifeJourneySection(
    list: List<LifeJourneyEntry>,
    onAdd: (year: String, title: String, type: String, description: String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AddLifeJourneyDialog(
            onConfirm = { year, title, type, desc ->
                onAdd(year, title, type, desc)
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
                IconButton(onClick = { onRemove(e.id) }, modifier = Modifier.size(28.dp)) {
                    SmallDeleteIcon(contentDescription = "删除节点")
                }
            }
        }
        AddRow(label = "添加节点") { showDialog = true }
    }
}

// ====== 笔记 / 互动记录（同一套 UI，文案参数化） ======

@Composable
fun NotesListSection(
    list: List<Note>,
    dialogTitle: String,
    placeholder: String,
    addLabel: String,
    areaHeight: Dp = 140.dp,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        EditTextAreaDialog(
            title = dialogTitle,
            initialValue = "",
            placeholder = placeholder,
            height = areaHeight,
            onConfirm = { newVal -> onAdd(newVal); showDialog = false },
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
                IconButton(onClick = { onRemove(n.id) }, modifier = Modifier.size(28.dp)) {
                    SmallDeleteIcon(contentDescription = "删除")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        AddRow(label = addLabel) { showDialog = true }
    }
}

// ====== 纪念日 ======

/**
 * @param onAddReminder 铃铛：为纪念日建提醒（advanceDays/time/repeat/notes 见 ReminderFormDialog）
 * @param onDelete      行内删除按钮
 */
@Composable
fun AnniversariesSection(
    list: List<Anniversary>,
    onAddReminder: (anniversaryId: String, advanceDays: Int, reminderTime: String, repeat: RepeatRule, notes: String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (list.isEmpty()) {
        EmptyState(title = "还没有纪念日", description = "点击 + 添加重要日期")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { a ->
            var showReminderForm by remember { mutableStateOf(false) }
            if (showReminderForm) {
                ReminderFormDialog(
                    onConfirm = { advanceDays, time, repeat, notes ->
                        onAddReminder(a.id, advanceDays, time, repeat, notes)
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
                IconButton(onClick = { onDelete(a.id) }, modifier = Modifier.size(28.dp)) {
                    SmallDeleteIcon(contentDescription = "删除纪念日")
                }
            }
        }
    }
}

// ====== 通用小件 ======

/** 「· 内容」+ 行尾删除按钮 */
@Composable
private fun DeletableBulletRow(text: String, contentDescription: String, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "· $text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            SmallDeleteIcon(contentDescription = contentDescription)
        }
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = WarmJournalColors.Primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = WarmJournalColors.Primary)
    }
}

@Composable
private fun SmallDeleteIcon(contentDescription: String) {
    Icon(
        imageVector = Icons.Filled.Delete,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(14.dp)
    )
}
