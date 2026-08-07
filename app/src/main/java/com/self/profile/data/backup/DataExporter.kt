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
    private val settingsStore: SettingsDataStore
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

    private fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(ts))
}

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
