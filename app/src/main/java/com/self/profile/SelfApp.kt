package com.self.profile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.self.profile.data.seed.SeedData
import com.self.profile.work.NotificationChannels
import com.self.profile.work.ReminderReregistrator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用入口。
 *
 * 启动时初始化：
 * - 通知渠道（Android 8.0+ 要求）
 * - WorkManager 配置（注入 Hilt 工厂）
 * - 种子数据（首次启动写入示例）
 * - 提醒重注册（遍历所有 enabled reminder 重新挂上 Worker）
 */
@HiltAndroidApp
class SelfApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var seedData: SeedData

    @Inject
    lateinit var reminderReregistrator: ReminderReregistrator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScope.launch { seedData.seedIfEmpty() }
        appScope.launch { reminderReregistrator.reregisterAll() }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                NotificationChannels.REMINDER_ID,
                getString(R.string.notif_channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_reminder_desc)
                enableVibration(true)
            }
            nm.createNotificationChannel(reminderChannel)
        }
    }
}
