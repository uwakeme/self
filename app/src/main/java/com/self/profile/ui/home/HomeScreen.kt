package com.self.profile.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.RepeatRule
import com.self.profile.ui.components.AvatarStack
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.GreetingHeader
import com.self.profile.ui.components.QuickActionRow
import com.self.profile.ui.components.QuoteCard
import com.self.profile.ui.components.ReminderItem
import com.self.profile.ui.components.SearchField
import com.self.profile.ui.components.SectionCard
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.LunarHelper
import com.self.profile.util.QuoteProvider
import java.time.LocalDate

/**
 * 首页（US-001 ~ US-005）
 *
 * 5 个区块：
 * 1. GreetingHeader：问候语 + 寄语 + 头像
 * 2. 4 快捷入口
 * 3. 近期提醒（前 3 条）
 * 4. 关系概览（头像堆叠 + 类别计数）
 * 5. 今日寄语
 */
@Composable
fun HomeScreen(
    /**
     * 跳到自我档案 tab。`scrollTo` 可选：
     * - "notes"          滚到"随想笔记"section
     * - "anniversaries"  滚到"纪念日"section
     * - null             跳过去但不滚
     */
    onNavigateToSelf: (scrollTo: String?) -> Unit,
    onNavigateToPeople: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quote = remember { QuoteProvider.today() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        GreetingHeader(
            nickname = state.nickname,
            avatarUrl = state.avatarUrl,
            fallbackColor = state.fallbackColor,
            fallbackInitial = state.fallbackInitial,
            onAvatarClick = { onNavigateToSelf(null) }
        )

        // 全局搜索入口（统一搜索框，入口模式）
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            SearchField(
                value = "",
                onValueChange = {},
                placeholder = "搜索人物、笔记、共同点…",
                entryOnly = true,
                onEntryClick = onNavigateToSearch
            )
        }

        // 快捷操作
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SectionCard(title = "快捷操作") {
                QuickActionRow(
                    onWriteNote = { onNavigateToSelf("notes") },
                    onAddPerson = onNavigateToPeople,
                    onAnniversary = { onNavigateToSelf("anniversaries") },
                    onReminder = onNavigateToSettings
                )
            }
        }

        // 近期提醒
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            SectionCard(
                title = "近期提醒",
                action = {
                    Text(
                        text = "查看全部 >",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToSettings() }
                    )
                }
            ) {
                if (state.upcomingReminders.isEmpty()) {
                    EmptyState(
                        title = "暂无近期提醒",
                        description = "去设置里添加生日或纪念日吧"
                    )
                } else {
                    state.upcomingReminders.forEachIndexed { index, reminder ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val person = state.others.firstOrNull { it.id == reminder.personId }
                        ReminderItem(
                            reminder = reminder,
                            personInitial = person?.avatarInitial,
                            personColor = person?.avatarColor,
                            today = state.today
                        )
                    }
                }
            }
        }

        // 关系概览
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            SectionCard(
                title = "我的人际关系",
                action = {
                    Text(
                        text = "查看图谱 >",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToMap() }
                    )
                }
            ) {
                if (state.others.isEmpty()) {
                    EmptyState(
                        title = "还没有添加人物",
                        description = "去人物图鉴添加吧"
                    )
                } else {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarStack(
                                people = state.others.take(6),
                                max = 6,
                                avatarSize = 32.dp,
                                overlap = 8.dp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "共 ${state.relationshipCounts.total} 位重要的人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        // 关系分布环形图 + 图例（Phase 3.3，纯 Canvas 自绘）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RelationshipDonut(
                                counts = state.relationshipCounts,
                                modifier = Modifier.size(84.dp)
                            )
                            Spacer(Modifier.width(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RelationshipLabel("家人", state.relationshipCounts.family, ColorFamily)
                                RelationshipLabel("朋友", state.relationshipCounts.friend, ColorFriend)
                                RelationshipLabel("同事", state.relationshipCounts.colleague, ColorColleague)
                                RelationshipLabel("同学", state.relationshipCounts.classmate, ColorClassmate)
                            }
                        }
                    }
                }
            }
        }

        // 本月重要日子（事件列表；不再展示整月日历条）
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            SectionCard(title = "本月重要日子") {
                if (state.monthEvents.isEmpty()) {
                    Text(
                        text = "本月暂无生日或纪念日",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.monthEvents.forEach { event ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(ColorFamily, CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${event.date.monthValue}月${event.date.dayOfMonth}日" +
                                        if (event.reminder.isLunar && event.reminder.repeat == RepeatRule.YEARLY) {
                                            "（${LunarHelper.lunarLabel(event.date)}）"
                                        } else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarmJournalColors.PrimaryDarker
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = event.reminder.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 今日寄语
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp)) {
            QuoteCard(
                quote = quote.text,
                author = quote.author,
                subline = quote.subline
            )
        }
    }
}

@Composable
private fun RelationshipLabel(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 关系图例颜色（与 design prototype 一致）
private val ColorFamily = Color(0xFFC4785B)
private val ColorFriend = Color(0xFF6B8EAE)
private val ColorColleague = Color(0xFF6B9E78)
private val ColorClassmate = Color(0xFFD4A24E)

/**
 * 关系分布环形图（Phase 3.3，纯 Canvas 自绘，与关系图谱同风格）。
 */
@Composable
private fun RelationshipDonut(counts: RelationshipCounts, modifier: Modifier = Modifier) {
    val segments = listOf(
        ColorFamily to counts.family,
        ColorFriend to counts.friend,
        ColorColleague to counts.colleague,
        ColorClassmate to counts.classmate
    )
    Canvas(modifier) {
        val stroke = 13.dp.toPx()
        val dia = size.minDimension - stroke
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(dia, dia)
        if (counts.total == 0) {
            drawArc(
                color = WarmJournalColors.BgSurface,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Butt),
                topLeft = topLeft, size = arcSize
            )
            return@Canvas
        }
        var start = -90f
        segments.forEach { (color, count) ->
            if (count > 0) {
                val sweep = count.toFloat() / counts.total * 360f
                drawArc(
                    color = color,
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                    topLeft = topLeft, size = arcSize
                )
                start += sweep
            }
        }
    }
}
