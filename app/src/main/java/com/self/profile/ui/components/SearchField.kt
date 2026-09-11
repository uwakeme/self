package com.self.profile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 统一的搜索框：白底、14dp 圆角、聚焦时主色描边，首页与图鉴共用。
 *
 * - 普通模式：真输入（图鉴筛选）
 * - 入口模式：[entryOnly] = true，禁用输入、整块可点，仅作为跳转入口（首页）
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    entryOnly: Boolean = false,
    onEntryClick: (() -> Unit)? = null
) {
    val field: @Composable (Boolean) -> Unit = { enabled ->
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = if (value.isNotEmpty() && !entryOnly) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "清空")
                    }
                }
            } else null,
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = WarmJournalColors.Primary,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (entryOnly) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onEntryClick?.invoke() }
        ) {
            field(false)
        }
    } else {
        Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            field(true)
        }
    }
}
