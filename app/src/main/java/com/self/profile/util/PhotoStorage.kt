package com.self.profile.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 照片本地存储工具。
 *
 * 把 MediaStore / 相册选择器返回的 content:// URI 复制到 app 私有目录
 * （`/data/data/com.self.profile/files/photos/`），得到一个稳定可读的绝对路径。
 *
 * 为什么不直接存 content:// URI：
 * - 用户卸载重装后，MediaStore 的 content URI 仍可读，但本 app 不能保证；
 * - 私有路径不需 READ_MEDIA_IMAGES 权限，备份导出也能直接读；
 * - Photo 序列化只需存 String 路径，跨进程 / 跨设备一致。
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun photosDir(): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    /**
     * 把 [uri] 指向的图片复制到私有目录，返回绝对路径。
     * 失败返回 null（不抛异常，调用方按 null 跳过）。
     */
    suspend fun saveFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val mime = context.contentResolver.getType(uri)
            val ext = when (mime) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }
            val fileName = "photo_${System.currentTimeMillis()}_${(0..9999).random()}.$ext"
            val dest = File(photosDir(), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            dest.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 把备份 zip 里的照片字节写入私有目录（文件名重新生成避免冲突），返回绝对路径。
     * 失败返回 null（调用方按 null 跳过，url 保持原值成悬空引用，不影响导入其余数据）。
     */
    suspend fun saveFromBytes(fileName: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val rawExt = fileName.substringAfterLast('.', "").lowercase()
            val ext = if (rawExt in setOf("jpg", "jpeg", "png", "webp", "gif")) rawExt else "jpg"
            val dest = File(photosDir(), "photo_${System.currentTimeMillis()}_${(0..9999).random()}.$ext")
            dest.writeBytes(bytes)
            dest.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 删除指定路径上的文件。失败静默（文件可能本来就不存在）。
     */
    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
