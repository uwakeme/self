package com.self.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 数字步进器：- [N] +。
 *
 * - [value]：当前值
 * - [onChange]：值改变时回调
 * - [min] / [max]：边界（默认 0-300）
 * - [step]：步长（默认 1）
 * - [unit]：单位（如 "cm"、"kg"）
 */
@Composable
fun NumberStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 300,
    step: Int = 1,
    unit: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        StepperButton(icon = Icons.Filled.Remove, enabled = value > min) {
            if (value > min) onChange((value - step).coerceAtLeast(min))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        StepperButton(icon = Icons.Filled.Add, enabled = value < max) {
            if (value < max) onChange((value + step).coerceAtMost(max))
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = WarmJournalColors.PrimaryLightest,
            contentColor = WarmJournalColors.PrimaryDarker,
            disabledContainerColor = WarmJournalColors.BgSurface,
            disabledContentColor = WarmJournalColors.TextTertiary
        )
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}
