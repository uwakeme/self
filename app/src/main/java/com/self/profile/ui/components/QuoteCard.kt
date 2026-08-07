package com.self.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 引用样式卡片（左侧竖线装饰）。用于今日寄语 / 鼓励语。
 */
@Composable
fun QuoteCard(
    quote: String,
    author: String? = null,
    subline: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WarmJournalColors.PrimaryLightest)
            .border(
                width = 0.dp,
                color = WarmJournalColors.PrimaryLighter,
                shape = MaterialTheme.shapes.large
            )
            .padding(20.dp)
    ) {
        // 左侧竖线
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .background(WarmJournalColors.PrimaryLighter)
                .padding(0.dp)
        )
        Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
            Text(
                text = "“$quote”",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = com.self.profile.ui.theme.WarmJournalColors.TextSecondary
            )
            if (author != null) {
                Text(
                    text = "— $author",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).align(androidx.compose.ui.Alignment.End)
                )
            }
            if (subline != null) {
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodySmall,
                    color = com.self.profile.ui.theme.WarmJournalColors.TextTertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
