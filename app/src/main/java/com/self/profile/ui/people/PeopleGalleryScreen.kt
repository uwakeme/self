package com.self.profile.ui.people

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.GallerySortRule
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.isLight
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.parseHexColor

/**
 * 人物图鉴（US-008）：横幅 + 搜索 + 筛选 + 2 列网格 + FAB。
 *
 * 阶段 1：只读，搜索筛选
 * 阶段 3：点击卡片进详情、FAB 添加、4 种排序
 */
@Composable
fun PeopleGalleryScreen(
    onOpenPerson: (String) -> Unit,
    onAddPerson: () -> Unit,
    viewModel: PeopleGalleryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showSortSheet by remember { mutableStateOf(false) }
    var customOrderEdit by rememberSaveable { mutableStateOf(false) }

    // 切到 CUSTOM 排序时自动 seed
    LaunchedEffect(state.sortRule) {
        if (state.sortRule == GallerySortRule.CUSTOM) {
            viewModel.seedCustomOrderIfEmpty()
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            current = state.sortRule,
            onSelect = {
                viewModel.setSortRule(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryBanner()
            SearchBar(
                query = state.query,
                onQueryChange = viewModel::setQuery
            )
            FilterRow(
                selected = state.filter,
                onSelect = viewModel::setFilter
            )
            SortRow(
                rule = state.sortRule,
                customOrderEdit = customOrderEdit,
                onSortClick = { showSortSheet = true },
                onEditOrderClick = {
                    if (state.sortRule == GallerySortRule.CUSTOM) customOrderEdit = !customOrderEdit
                }
            )
            if (state.people.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = if (state.total == 0) "还没有添加人物" else "该分类下暂无人物",
                        description = if (state.total == 0) "点击右下角加一个吧" else "试试其他分类或搜索"
                    )
                }
            } else if (customOrderEdit && state.sortRule == GallerySortRule.CUSTOM) {
                CustomOrderList(
                    people = state.people,
                    onMove = { from, to ->
                        viewModel.moveCustomOrder(state.people.map { it.id }, from, to)
                    },
                    onItemClick = onOpenPerson
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.people, key = { it.id }) { person ->
                        PersonCard(person = person, onClick = { onOpenPerson(person.id) })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddPerson,
            icon = { Icon(Icons.Filled.Add, contentDescription = "添加") },
            text = { Text("加人物") },
            containerColor = WarmJournalColors.Primary,
            contentColor = WarmJournalColors.TextInverse,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun CustomOrderList(
    people: List<Person>,
    onMove: (from: Int, to: Int) -> Unit,
    onItemClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(people.size) { index ->
            val person = people[index]
            ReorderablePersonRow(
                person = person,
                isFirst = index == 0,
                isLast = index == people.lastIndex,
                onMoveUp = { onMove(index, index - 1) },
                onMoveDown = { onMove(index, index + 1) },
                onClick = { onItemClick(person.id) }
            )
        }
    }
}

@Composable
private fun ReorderablePersonRow(
    person: Person,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (isFirst) MaterialTheme.colorScheme.outlineVariant else WarmJournalColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (isLast) MaterialTheme.colorScheme.outlineVariant else WarmJournalColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Avatar(initial = person.avatarInitial, colorHex = person.avatarColor, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "${person.relationshipType.label}${if (!person.mbti.isNullOrBlank()) " · ${person.mbti}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GalleryBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WarmJournalColors.PrimaryLight,
                        WarmJournalColors.PrimaryLightest
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = "人物图鉴",
                style = MaterialTheme.typography.displaySmall,
                color = WarmJournalColors.TextInverse
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每一段相遇，都值得被记住",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmJournalColors.TextInverse.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索姓名", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "清空")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = WarmJournalColors.BgCard,
            unfocusedContainerColor = WarmJournalColors.BgCard
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FilterRow(
    selected: RelationshipType?,
    onSelect: (RelationshipType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("全部") }
        )
        RelationshipType.values().forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarmJournalColors.PrimaryLightest,
                    selectedLabelColor = WarmJournalColors.PrimaryDarker
                )
            )
        }
    }
}

@Composable
private fun PersonCard(person: Person, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            initial = person.avatarInitial,
            colorHex = person.avatarColor,
            size = 56.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = person.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        if (!person.mbti.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(parseHexColor(person.avatarColor)?.copy(alpha = 0.15f) ?: WarmJournalColors.PrimaryLightest)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = person.mbti!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = parseHexColor(person.avatarColor)?.let {
                        if (it.isLight()) WarmJournalColors.TextPrimary else it
                    } ?: WarmJournalColors.PrimaryDarker
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(WarmJournalColors.BgSurface)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = person.relationshipType.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        person.bloodType?.takeIf { it.isNotBlank() }?.let { bt ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = "血型 $bt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SortRow(
    rule: GallerySortRule,
    customOrderEdit: Boolean,
    onSortClick: () -> Unit,
    onEditOrderClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onSortClick() }
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = rule.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (rule == GallerySortRule.CUSTOM) {
            TextButton(onClick = onEditOrderClick) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = null,
                    tint = if (customOrderEdit) WarmJournalColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (customOrderEdit) "完成" else "调整顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (customOrderEdit) WarmJournalColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SortBottomSheet(
    current: GallerySortRule,
    onSelect: (GallerySortRule) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        GallerySortRule.RECENTLY_VIEWED,
        GallerySortRule.RECENTLY_MODIFIED,
        GallerySortRule.CREATED_AT,
        GallerySortRule.CUSTOM
    )
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "选择排序方式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            options.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(opt) }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (opt == current) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = WarmJournalColors.Primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
