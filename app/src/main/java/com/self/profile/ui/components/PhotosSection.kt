package com.self.profile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.self.profile.domain.model.Photo
import com.self.profile.ui.theme.WarmJournalColors
import java.io.File

/**
 * 照片墙。3 列方形缩略图网格，最后一格是 "+" 添加按钮。
 *
 * - 点击缩略图 → [PhotoViewerDialog]（可编辑描述 / 删除）
 * - 点击 "+" → 系统照片选择器（ActivityResultContracts.PickMultipleVisualMedia，
 *   Android 13+ 不需权限，旧版本回退到 SAF）
 *
 * @param photos 照片列表
 * @param onAddPhoto 收到一个 content:// URI（已复制到 app 私有目录的事由调用方负责）
 * @param onRemovePhoto 删除某张照片
 * @param onUpdatePhotoDescription 更新某张照片的描述
 */
@Composable
fun PhotosSection(
    photos: List<Photo>,
    onAddPhoto: (Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onUpdatePhotoDescription: (String, String) -> Unit
) {
    var viewingPhoto by remember { mutableStateOf<Photo?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris: List<Uri> ->
        uris.forEach { onAddPhoto(it) }
    }

    viewingPhoto?.let { photo ->
        PhotoViewerDialog(
            photo = photo,
            onDismiss = { viewingPhoto = null },
            onUpdateDescription = { desc -> onUpdatePhotoDescription(photo.id, desc) },
            onDelete = {
                onRemovePhoto(photo.id)
                viewingPhoto = null
            }
        )
    }

    if (photos.isEmpty()) {
        // 空态：占大按钮，提示去添加
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(WarmJournalColors.BgSurface)
                .clickable {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = WarmJournalColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "点击上传照片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmJournalColors.Primary
                )
                Text(
                    text = "记录生活的瞬间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 3 列网格（用 Row 嵌套避免引入 LazyVerticalGrid 嵌套 LazyColumn 的复杂度）
            photos.chunked(3).forEach { rowPhotos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPhotos.forEach { photo ->
                        PhotoTile(
                            photo = photo,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { viewingPhoto = photo }
                        )
                    }
                }
            }
            // 独立的全宽添加按钮（在网格下方，统一入口）
            AddPhotoRow(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
    }
}

@Composable
private fun PhotoTile(
    photo: Photo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = File(photo.url),
            contentDescription = photo.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )
    }
}

@Composable
private fun AddPhotoRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WarmJournalColors.BgSurface)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = WarmJournalColors.Primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "继续添加照片",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmJournalColors.Primary
        )
    }
}
