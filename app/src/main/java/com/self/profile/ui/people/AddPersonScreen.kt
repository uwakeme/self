package com.self.profile.ui.people

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.profile.domain.model.RelationshipType
import com.self.profile.ui.components.Avatar
import com.self.profile.ui.components.dialogs.ChoicePickerDialog
import com.self.profile.ui.theme.WarmJournalColors
import com.self.profile.util.LunarHelper
import com.self.profile.util.parseHexColor
import kotlinx.coroutines.launch

/**
 * 添加新人物（US-009）。
 *
 * - 必填：姓名、关系
 * - 可选：MBTI、头像颜色、亲密程度
 * - 姓名为空时"保存"按钮置灰 + 提示
 * - 保存成功后返回人物图鉴，列表首位
 */
@Composable
fun AddPersonScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPersonViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部条
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Spacer(Modifier.width(4.dp))
                Text("加人物", style = MaterialTheme.typography.titleLarge)
            }

            // 头像预览
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    initial = form.name.take(1).ifBlank { "·" },
                    colorHex = form.avatarColor,
                    size = 80.dp
                )
            }

            // 头像颜色
            AvatarColorRow(
                current = form.avatarColor,
                onChange = viewModel::setAvatarColor
            )

            Spacer(Modifier.height(20.dp))

            // 姓名（必填）
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::setName,
                label = { Text("姓名 *") },
                placeholder = { Text("请输入") },
                singleLine = true,
                isError = form.name.isEmpty(),
                supportingText = if (form.name.isEmpty()) {
                    { Text("请输入姓名", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 关系类型（必填）
            RelationshipRow(
                selected = form.relationshipType,
                onChange = viewModel::setRelationship
            )

            Spacer(Modifier.height(12.dp))

            // MBTI（可选）
            MbtiRow(
                selected = form.mbti,
                onChange = viewModel::setMbti
            )

            Spacer(Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = {
                    scope.launch {
                        viewModel.save { person ->
                            scope.launch {
                                snackbarHostState.showSnackbar("已添加「${person.name}」")
                            }
                            onSaved()
                        }
                    }
                },
                enabled = viewModel.isValid() && !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmJournalColors.Primary,
                    contentColor = WarmJournalColors.TextInverse,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = if (saving) "保存中…" else "保存",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AvatarColorRow(
    current: String,
    onChange: (String) -> Unit
) {
    val palette = listOf(
        "#C4785B", "#D99B82", "#EBC8B8", "#F5E6DD",
        "#6B8EAE", "#6B9E78", "#D4A24E", "#A65D42"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("头像颜色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            palette.forEach { hex ->
                val isCurrent = hex.equals(current, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex) ?: Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        IconButton(
                            onClick = { onChange(hex) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onChange(hex) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Spacer(Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipRow(
    selected: RelationshipType,
    onChange: (RelationshipType) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        ChoicePickerDialog(
            title = "选择关系",
            options = LunarHelper.allRelationshipTypes,
            selected = selected.label,
            onConfirm = { picked ->
                onChange(RelationshipType.fromLabel(picked))
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("关系 *", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MbtiRow(
    selected: String?,
    onChange: (String?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        ChoicePickerDialog(
            title = "选择 MBTI（可选）",
            options = listOf("") + LunarHelper.allMbti,
            selected = selected,
            onConfirm = { picked ->
                onChange(picked.takeIf { it.isNotBlank() })
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("MBTI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selected ?: "未选择",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
