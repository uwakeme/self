package com.self.profile.data.backup

import android.content.Context
import android.net.Uri
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.local.AppJson
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.util.PhotoStorage
import com.self.profile.work.ReminderReregistrator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全量数据备份 / 恢复。
 *
 * v2 备份格式：zip 包
 * - `backup.json` —— [BackupData]（Person.photo.url 改写为 zip 内相对路径 `photos/<文件名>`）
 * - `photos/...`  —— 照片文件本体
 *
 * 兼容 v1：纯 JSON、不含照片、photo.url 为导出设备上的绝对路径——导入后这些 url
 * 会成为悬空引用（显示为空），其余数据正常恢复。
 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val settingsStore: SettingsDataStore,
    private val photoStorage: PhotoStorage,
    private val reregistrator: ReminderReregistrator
) {

    /**
     * 收集全部数据并写出 zip 到 [uri]（SAF CreateDocument 返回的位置）。
     */
    suspend fun exportToUri(uri: Uri): ExportSummary = withContext(Dispatchers.IO) {
        val (data, photoFiles) = collectForExport()
        writeZip(uri, data, photoFiles)
        settingsStore.setLastBackupTime(System.currentTimeMillis())
        ExportSummary(
            peopleCount = data.people.size + if (data.self != null) 1 else 0,
            remindersCount = data.reminders.size,
            photosCount = photoFiles.size
        )
    }

    /**
     * 从 [uri]（v2 zip 或 v1 纯 JSON）恢复全部数据。
     *
     * 流程：解析 → 当前数据先落一份安全备份（cache，防误导入）→ 清空现有 →
     * 恢复照片并重写路径 → 恢复人物/提醒 → 恢复设置 → 重挂提醒调度。
     */
    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取所选文件")
        val parsed = parseBackup(bytes)
        if (parsed.backup.version > BACKUP_VERSION) {
            throw IllegalStateException("备份版本过新（v${parsed.backup.version}），请先升级应用")
        }

        // 1. 导入前把当前数据备份到 cache，导入错了还能救回来
        val safety = createSafetyBackup()

        // 2. 清空现有（人物 + 提醒 + 照片文件）
        clearAllData()

        // 3. 照片落盘，建 相对名 → 新绝对路径 的重写表
        val pathMap = HashMap<String, String>()
        parsed.photoBytes.forEach { (name, content) ->
            photoStorage.saveFromBytes(name, content)?.let { pathMap["$PHOTO_DIR/$name"] = it }
        }

        // 4. 恢复人物（照片 url 重写到本机私有目录）
        val self = parsed.backup.self?.let { rewritePhotoUrls(it, pathMap) }
        val people = parsed.backup.people.map { rewritePhotoUrls(it, pathMap) }
        personRepo.upsertAll(listOfNotNull(self) + people)

        // 5. 恢复提醒 + 设置，重挂调度
        reminderRepo.upsertAll(parsed.backup.reminders)
        settingsStore.restoreFrom(parsed.backup.settings)
        reregistrator.reregisterAll()

        ImportResult(
            peopleCount = people.size + if (self != null) 1 else 0,
            remindersCount = parsed.backup.reminders.size,
            photosRestored = pathMap.size,
            safetyBackupPath = safety?.absolutePath,
            fromVersion = parsed.backup.version
        )
    }

    /**
     * 清空所有用户数据（人物 + 提醒 + 照片，含回收站软删行）。
     *
     * 设计原则：
     * - **不**触碰设置项（深色模式、字体、应用锁、提醒默认值、模块顺序等都是 app 偏好，不属于"数据"）。
     * - 先删提醒再删人物，避免顺序问题（虽然当前 schema 没外键，但顺序上更安全）。
     * - 照片文件在最后清（即便 DB 引用已断，文件不再被访问）。
     *
     * @return 实际删除的数量，方便 UI 展示
     */
    suspend fun clearAllData(): ClearSummary {
        val peopleBefore = personRepo.observeAll().first().size
        val remindersBefore = reminderRepo.observeAll().first().size
        val photosBefore = photoStorage.photosDir().listFiles()?.size ?: 0

        reminderRepo.deleteAll()
        personRepo.deleteAll()
        photoStorage.photosDir().listFiles()?.forEach { it.delete() }

        return ClearSummary(
            peopleDeleted = peopleBefore,
            remindersDeleted = remindersBefore,
            photosDeleted = photosBefore
        )
    }

    // ====== 内部 ======

    /** 当前数据快照：photo.url 改写为 zip 内相对路径，同时收集要打包的照片文件 */
    private suspend fun collectForExport(): Pair<BackupData, List<File>> {
        val allPeople = personRepo.observeAll().first()
        val reminders = reminderRepo.observeAll().first()
        val settings = settingsStore.settings.first()
        val self = allPeople.firstOrNull { it.isSelf }
        val others = allPeople.filter { !it.isSelf }

        val filesByName = LinkedHashMap<String, File>()
        fun rewrite(p: Person): Person = p.copy(
            photos = p.photos.map { photo ->
                val f = File(photo.url)
                if (f.exists() && f.isFile) {
                    filesByName[f.name] = f
                    photo.copy(url = "$PHOTO_DIR/${f.name}")
                } else photo
            }
        )

        val data = BackupData(
            version = BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            self = self?.let(::rewrite),
            people = others.map(::rewrite),
            reminders = reminders,
            settings = settings
        )
        return data to filesByName.values.toList()
    }

    private fun writeZip(uri: Uri, data: BackupData, photoFiles: List<File>) {
        val out = appContext.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("无法打开导出位置")
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(AppJson.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            photoFiles.forEach { file ->
                zip.putNextEntry(ZipEntry("$PHOTO_DIR/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * 导入前的安全备份：写当前数据到 cache 目录的 zip。
     * 失败不阻断导入（返回 null），但结果里会提示用户没拿到安全备份。
     */
    private suspend fun createSafetyBackup(): File? = runCatching {
        val (data, photoFiles) = collectForExport()
        val dir = File(appContext.cacheDir, "safety-backup").apply { mkdirs() }
        // 只留最近 3 份，防止 cache 无限膨胀
        dir.listFiles()?.sortedByDescending { it.name }?.drop(2)?.forEach { it.delete() }
        val target = File(dir, "safety-${formatTimestamp(System.currentTimeMillis())}.zip")
        val fos = FileOutputStream(target)
        ZipOutputStream(BufferedOutputStream(fos)).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(AppJson.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            photoFiles.forEach { file ->
                zip.putNextEntry(ZipEntry("$PHOTO_DIR/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        target
    }.getOrNull()

    /** 先按 v1 纯 JSON 解，失败再按 zip 解（zip 里的 backup.json 与 photos 目录） */
    private fun parseBackup(bytes: ByteArray): ParsedBackup {
        val asPlainJson = runCatching {
            AppJson.decodeFromString(BackupData.serializer(), bytes.decodeToString())
        }.getOrNull()
        if (asPlainJson != null) return ParsedBackup(asPlainJson, emptyMap())

        var backup: BackupData? = null
        val photoBytes = LinkedHashMap<String, ByteArray>()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == JSON_ENTRY ->
                            backup = AppJson.decodeFromString(BackupData.serializer(), zip.readBytes().decodeToString())
                        entry.name.startsWith("$PHOTO_DIR/") && !entry.isDirectory ->
                            photoBytes[entry.name.removePrefix("$PHOTO_DIR/")] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val b = backup ?: throw IllegalStateException("不是有效的备份文件（需要本应用导出的 zip 或 JSON）")
        return ParsedBackup(b, photoBytes)
    }

    private fun rewritePhotoUrls(p: Person, pathMap: Map<String, String>): Person = p.copy(
        photos = p.photos.map { photo -> pathMap[photo.url]?.let { photo.copy(url = it) } ?: photo }
    )

    private fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(ts))

    companion object {
        const val BACKUP_VERSION = 2
        const val JSON_ENTRY = "backup.json"
        const val PHOTO_DIR = "photos"

        /** SAF 建议文件名 */
        fun suggestedFileName(): String =
            "self-profile-backup-${formatTimestampStatic(System.currentTimeMillis())}.zip"

        private fun formatTimestampStatic(ts: Long): String =
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(ts))
    }
}

/** 导出结果摘要，用于 UI 反馈。 */
data class ExportSummary(
    val peopleCount: Int,
    val remindersCount: Int,
    val photosCount: Int
)

/** 导入结果摘要。 */
data class ImportResult(
    val peopleCount: Int,
    val remindersCount: Int,
    val photosRestored: Int,
    val safetyBackupPath: String?,
    val fromVersion: Int
)

/** 清除结果摘要，用于 UI 反馈。 */
data class ClearSummary(
    val peopleDeleted: Int,
    val remindersDeleted: Int,
    val photosDeleted: Int
)

/**
 * 备份数据 DTO。version 用于兼容判断；v2 起照片路径为 zip 内相对路径。
 */
@Serializable
data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val self: Person? = null,
    val people: List<Person> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val settings: AppSettings = AppSettings()
)

private data class ParsedBackup(
    val backup: BackupData,
    val photoBytes: Map<String, ByteArray>
)
