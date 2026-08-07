package com.self.profile.ui.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import com.self.profile.ui.components.EmptyState
import com.self.profile.ui.theme.WarmJournalColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 关系图谱（US-011）：Canvas 绘制星形网络图。
 *
 * 视觉规范（与 prototype 一致）：
 * - 中心节点"我"（pulse 动画）
 * - 家人：内圈 r=85，实线粗 2.5
 * - 朋友：中圈 r=130，虚线 1.5
 * - 同事/同学：外圈 r=160，点线 1
 * - 2x2 统计网格
 */
@Composable
fun RelationshipMapScreen(
    onOpenPerson: (String) -> Unit,
    viewModel: RelationshipMapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(WarmJournalColors.PrimaryLightest, WarmJournalColors.Bg)
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "关系图谱",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "看见人与人之间的联结",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Legend
        Legend()
        Spacer(Modifier.height(8.dp))

        // Canvas 图谱
        if (state.total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "还没有人物",
                    description = "去人物图鉴添加人，这里就能看到关系星图"
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(340f / 400f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                RelationshipCanvas(
                    family = state.family,
                    friend = state.friend,
                    colleague = state.colleague,
                    classmate = state.classmate,
                    onClickNode = { onOpenPerson(it.id) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2x2 统计
        StatsGrid(
            total = state.total,
            family = state.family.size,
            friend = state.friend.size,
            other = state.colleague.size + state.classmate.size
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendDot(WarmJournalColors.Primary, "家人")
        LegendDot(WarmJournalColors.Info, "朋友")
        LegendDot(WarmJournalColors.Success, "同事")
        LegendDot(WarmJournalColors.Warning, "同学")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RelationshipCanvas(
    family: List<Person>,
    friend: List<Person>,
    colleague: List<Person>,
    classmate: List<Person>,
    onClickNode: (Person) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()

    // 节点位置（Composed 阶段算好，Canvas 绘制 + 点击检测共享）
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }
    val allPeople = remember(family, friend, colleague, classmate) {
        family + friend + colleague + classmate
    }

    // 尺寸/数据变化时重算节点位置（极坐标：家人内圈 / 朋友中圈 / 同事同学外圈）
    LaunchedEffect(canvasSize, family, friend, colleague, classmate) {
        if (canvasSize.width == 0 || canvasSize.height == 0) {
            nodePositions.clear()
            return@LaunchedEffect
        }
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val familyR = w * 0.25f
        val friendR = w * 0.38f
        val otherR = w * 0.48f

        val groups = listOf(
            family to familyR,
            friend to friendR,
            colleague to otherR,
            classmate to otherR
        )
        val newPositions = mutableMapOf<String, Offset>()
        // 每环加 PI/4 偏移，避免少人时三环节点全堆在垂直轴上
        groups.forEachIndexed { ringIndex, (people, r) ->
            val ringOffset = ringIndex * (PI / 4)
            people.forEachIndexed { i, person ->
                val theta = ringOffset + (2 * PI * i / people.size.coerceAtLeast(1)) - PI / 2
                val x = cx + r * cos(theta).toFloat()
                val y = cy + r * sin(theta).toFloat()
                newPositions[person.id] = Offset(x, y)
            }
        }
        nodePositions.clear()
        nodePositions.putAll(newPositions)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(allPeople) {
                detectTapGestures { tap ->
                    // 找 36px 内最近的节点
                    val hit = nodePositions.entries
                        .filter { (tap - it.value).getDistance() < 36f }
                        .minByOrNull { (tap - it.value).getDistance() }
                        ?.key
                    if (hit != null) {
                        allPeople.firstOrNull { it.id == hit }?.let(onClickNode)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val familyR = w * 0.25f
            val friendR = w * 0.38f
            val otherR = w * 0.48f

            // 背景渐变
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WarmJournalColors.PrimaryLightest.copy(alpha = 0.5f),
                        WarmJournalColors.Bg.copy(alpha = 0f)
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.6f
                )
            )

            // 三圈轨道
            drawCircle(
                color = WarmJournalColors.Border.copy(alpha = 0.25f),
                radius = familyR,
                center = Offset(cx, cy),
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 8f)))
            )
            drawCircle(
                color = WarmJournalColors.Border.copy(alpha = 0.30f),
                radius = friendR,
                center = Offset(cx, cy),
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            )
            drawCircle(
                color = WarmJournalColors.Border.copy(alpha = 0.20f),
                radius = otherR,
                center = Offset(cx, cy),
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 7f)))
            )

            // 中心节点（pulse）
            val pulseAlpha = 0.3f + 0.4f * pulse
            val pulseRadius = 32f + 6f * pulse
            drawCircle(
                color = WarmJournalColors.Primary.copy(alpha = pulseAlpha * 0.5f),
                radius = pulseRadius + 10,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = WarmJournalColors.Primary,
                radius = 32f,
                center = Offset(cx, cy)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString("我"),
                topLeft = Offset(cx - 8f, cy - 12f),
                style = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
            )

            // 节点位置 + 连线（用 Composed 阶段算好的位置）
            drawNodesAndConnections(
                cx = cx, cy = cy,
                familyR = familyR, friendR = friendR, otherR = otherR,
                family = family, friend = friend, colleague = colleague, classmate = classmate,
                textMeasurer = textMeasurer,
                nodePositions = nodePositions
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodesAndConnections(
    cx: Float, cy: Float,
    familyR: Float, friendR: Float, otherR: Float,
    family: List<Person>, friend: List<Person>, colleague: List<Person>, classmate: List<Person>,
    textMeasurer: TextMeasurer,
    nodePositions: Map<String, Offset>
) {
    val allByKind = listOf(
        // 家人：实线 → pathEffect 传 null（不能用 dashPathEffect(floatArrayOf(0f))，API 要求偶数长度会崩）
        Triple(family, familyR, WarmJournalColors.Primary to (null to 2.5f)),
        Triple(friend, friendR, WarmJournalColors.Info to (PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) to 2f)),
        Triple(colleague, otherR, WarmJournalColors.Success to (PathEffect.dashPathEffect(floatArrayOf(3f, 5f)) to 1.5f)),
        Triple(classmate, otherR, WarmJournalColors.Warning to (PathEffect.dashPathEffect(floatArrayOf(3f, 5f)) to 1.5f))
    )

    // 连线
    allByKind.forEach { (people, _, styleInfo) ->
        val (color, dashWidth) = styleInfo
        val (pathEffect, width) = dashWidth
        people.forEach { person ->
            val pos = nodePositions[person.id] ?: return@forEach
            drawLine(
                color = color.copy(alpha = if (person.relationshipType == RelationshipType.FAMILY) 0.5f else 0.3f),
                start = Offset(cx, cy),
                end = pos,
                strokeWidth = width * 2f,
                pathEffect = pathEffect
            )
        }
    }

    // 节点
    allByKind.forEach { (people, _, styleInfo) ->
        val (color, _) = styleInfo
        people.forEach { person ->
            val pos = nodePositions[person.id] ?: return@forEach
            drawCircle(
                color = color,
                radius = 24f,
                center = pos
            )
            // 文字
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString(person.avatarInitial.take(1)),
                topLeft = Offset(pos.x - 8f, pos.y - 8f),
                style = TextStyle(color = Color.White, fontSize = 13.sp)
            )
        }
    }
}

@Composable
private fun StatsGrid(total: Int, family: Int, friend: Int, other: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox("总人数", total, modifier = Modifier.weight(1f))
        StatBox("家人", family, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox("朋友", friend, modifier = Modifier.weight(1f))
        StatBox("其他", other, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$value", style = MaterialTheme.typography.displaySmall, color = WarmJournalColors.Primary)
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
