package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.self.profile.domain.model.Person

/**
 * 头像堆叠（首页关系概览用，最多 6 个）。
 *
 * 第 1 个画"我"自己（白色背景 + 主色边），后续画其他人，负 margin 重叠。
 */
@Composable
fun AvatarStack(
    people: List<Person>,
    max: Int = 6,
    avatarSize: Dp = 32.dp,
    overlap: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val display = if (people.size > max) people.take(max - 1) + people.last().copy(name = "+${people.size - max + 1}") else people
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        display.forEachIndexed { index, person ->
            val isSelf = person.isSelf
            Box(
                modifier = Modifier
                    .padding(start = if (index == 0) 0.dp else (avatarSize - overlap) * index)
            ) {
                Avatar(
                    initial = person.avatarInitial,
                    colorHex = person.avatarColor,
                    size = avatarSize,
                    borderColor = com.self.profile.ui.theme.WarmJournalColors.Bg,
                    borderWidth = 2.dp
                )
                if (isSelf) {
                    // 自我头像加主色边
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(2.dp, com.self.profile.ui.theme.WarmJournalColors.Primary, CircleShape)
                    )
                }
            }
        }
    }
}
