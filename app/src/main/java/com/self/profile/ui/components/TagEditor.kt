package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 标签 chip + 删除按钮。
 */
@Composable
fun TagChip(
    text: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(WarmJournalColors.PrimaryLightest)
            .padding(start = 10.dp, end = if (onRemove != null) 4.dp else 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = WarmJournalColors.PrimaryDarker,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onRemove != null) {
            Spacer(Modifier.width(2.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "删除",
                    tint = WarmJournalColors.PrimaryDarker,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 标签组：渲染 chip 列表 + "添加"按钮。
 *
 * - [tags]：现有 tag
 * - [onAddClick]：点击 + 时弹出输入
 * - [onRemoveTag]：删除某个 tag
 */
@Composable
fun TagGroup(
    tags: List<String>,
    onAddClick: () -> Unit,
    onRemoveTag: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        tags.forEach { tag ->
            TagChip(
                text = tag,
                onRemove = onRemoveTag?.let { { -> it(tag) } }
            )
        }
        AddChip(onClick = onAddClick)
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(WarmJournalColors.BgSurface)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
