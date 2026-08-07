package com.self.profile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.ui.components.AvatarStack
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.components.GreetingHeader
import com.self.profile.ui.components.QuickActionRow
import com.self.profile.ui.components.QuoteCard
import com.self.profile.ui.components.ReminderItem
import com.self.profile.ui.components.SectionCard
import com.self.profile.util.QuoteProvider
import com.self.profile.util.parseHexColor

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
    onNavigateToSelf: () -> Unit,
    onNavigateToPeople: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReminders: () -> Unit,
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
            onAvatarClick = onNavigateToSelf
        )

        // 快捷操作
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SectionCard(title = "快捷操作") {
                QuickActionRow(
                    onWriteNote = onNavigateToSelf,
                    onAddPerson = onNavigateToPeople,
                    onAnniversary = onNavigateToSelf,
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
                        modifier = Modifier.clickable { onNavigateToReminders() }
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
                            personColor = person?.avatarColor
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
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            RelationshipLabel("家人", state.relationshipCounts.family, ColorFamily)
                            RelationshipLabel("朋友", state.relationshipCounts.friend, ColorFriend)
                            RelationshipLabel("同事", state.relationshipCounts.colleague, ColorColleague)
                            RelationshipLabel("同学", state.relationshipCounts.classmate, ColorClassmate)
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
private val ColorFamily = parseHexColor("#C4785B")!!
private val ColorFriend = parseHexColor("#6B8EAE")!!
private val ColorColleague = parseHexColor("#6B9E78")!!
private val ColorClassmate = parseHexColor("#D4A24E")!!
