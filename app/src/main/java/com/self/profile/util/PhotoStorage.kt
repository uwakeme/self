package com.self.profile.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
 * - 私有路径不需任何媒体读取权限，备份导出也能直接读；
 * - Photo 序列化只需存 String 路径，跨进程 / 跨设备一致。
 *
 * 存储策略（PRD 4.2）：静态图压缩到最长边 ≤ 1080px、JPEG 质量 80，
 * 避免现代相机原图（3-8MB）吃满私有目录；GIF（可能带动画）原样保留。
 * 压缩结果比原图还大时直接用原图。
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun photosDir(): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    /**
     * 把 [uri] 指向的图片压缩后写入私有目录，返回绝对路径。
     * 失败返回 null（不抛异常，调用方按 null 跳过）。
     */
    suspend fun saveFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val mime = context.contentResolver.getType(uri)
            if (mime == "image/gif") {
                // GIF 可能带动画，Bitmap 重编码会丢帧，原样保留
                copyOriginal(uri, "gif")
            } else {
                val bytes = compressImage(uri)
                val ext = if (bytes.second) "png" else "jpg"
                val dest = File(photosDir(), "photo_${System.currentTimeMillis()}_${(0..9999).random()}.$ext")
                FileOutputStream(dest).use { it.write(bytes.first) }
                dest.absolutePath
            }
        } catch (_: Throwable) {
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
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 删除指定路径上的文件（IO 线程）。失败静默（文件可能本来就不存在）。
     */
    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
        Unit
    }

    // ====== 内部 ======

    private fun copyOriginal(uri: Uri, ext: String): String? {
        val dest = File(photosDir(), "photo_${System.currentTimeMillis()}_${(0..9999).random()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: return null
        return dest.absolutePath
    }

    /**
     * 解码 → 按需降采样/缩放到最长边 [MAX_DIMENSION] → 重编码。
     * 返回 (字节, 是否 PNG)。PNG 输入保 PNG（无损，避免透明度变黑底）。
     */
    private fun compressImage(uri: Uri): Pair<ByteArray, Boolean> {
        val sourceBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取图片")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("不是有效图片")
        }

        val isPng = bounds.outMimeType == "image/png"
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, opts)
            ?: throw IllegalStateException("图片解码失败")

        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        val out = ByteArrayOutputStream()
        val compressed = if (isPng) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray() to true
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray() to false
        }
        bitmap.recycle()

        // 压缩没赚到就存原图
        return if (compressed.first.size < sourceBytes.size) compressed else sourceBytes to isPng
    }

    companion object {
        /** 最长边上限（px），PRD 4.2 存储策略 */
        const val MAX_DIMENSION = 1080

        /** JPEG 压缩质量，PRD 4.2 存储策略 */
        const val JPEG_QUALITY = 80
    }
}
