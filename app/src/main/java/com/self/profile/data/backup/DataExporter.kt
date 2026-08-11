package com.self.profile.data.backup

import android.content.Context
import android.os.Environment
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.local.AppJson
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.util.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全量数据导出器。
 *
 * 格式：JSON（kotlinx.serialization 序列化）。
 * 写入位置：app 私有 Downloads 目录（`getExternalFilesDir(DIRECTORY_DOWNLOADS)`）
 *          在 Android 10+ 用户可通过文件管理器看到，路径 `/Android/data/com.self.profile/files/Downloads/`
 *
 * 导出后更新 [AppSettings.lastBackupTime]。
 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val settingsStore: SettingsDataStore,
    private val photoStorage: PhotoStorage
) {

    /**
     * 收集所有数据并写入 Downloads。
     *
     * @return 写入文件的绝对路径
     */
    suspend fun exportToDownloads(): String {
        val data = collect()
        val json = AppJson.encodeToString(BackupData.serializer(), data)

        val timestamp = System.currentTimeMillis()
        val fileName = "self-profile-backup-${formatTimestamp(timestamp)}.json"

        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, "Downloads").also { it.mkdirs() }
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val target = File(downloadsDir, fileName)
        target.writeText(json, Charsets.UTF_8)

        settingsStore.setLastBackupTime(timestamp)
        return target.absolutePath
    }

    private suspend fun collect(): BackupData {
        val allPeople = personRepo.observeAll().first()
        val reminders = reminderRepo.observeAll().first()
        val settings = settingsStore.settings.first()
        val self = allPeople.firstOrNull { it.isSelf }
        val others = allPeople.filter { !it.isSelf }

        return BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            self = self,
            people = others,
            reminders = reminders,
            settings = settings
        )
    }

    /**
     * 清空所有用户数据（人物 + 提醒 + 照片）。
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

    private fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(ts))
}

/** 清除结果摘要，用于 UI 反馈。 */
data class ClearSummary(
    val peopleDeleted: Int,
    val remindersDeleted: Int,
    val photosDeleted: Int
)

/**
 * 备份数据 DTO。version 用于未来 schema 演进时做兼容判断。
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
