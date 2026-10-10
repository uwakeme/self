package com.self.profile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.self.profile.data.repository.RecycleBinJanitor
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
 * - 种子数据（首次启动写入示例），必须先于提醒重注册完成
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

    @Inject
    lateinit var recycleBinJanitor: RecycleBinJanitor

    @Inject
    lateinit var personRepo: com.self.profile.data.repository.PersonRepository

    @Inject
    lateinit var reminderRepo: com.self.profile.data.repository.ReminderRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val TAG = "SelfApp"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // 播种与重注册必须串行：reregisterAll() 读的是 DB 里的 enabled 列表，
        // 若它先于 seedIfEmpty() 跑到空列表，种子写入的 3 条提醒这次冷启动就不会被排上 Worker，
        // 得再等一次冷启动才响。
        //
        // 播种单独包 runCatching：SupervisorJob 只隔离兄弟协程，协程体内前一句抛异常会直接中止
        // 后面的语句。不兜住的话，播种一失败重注册就跟着不跑，库里已有的提醒会白白失去调度。
        appScope.launch {
            runCatching { seedData.seedIfEmpty() }
                .onFailure { Log.w(TAG, "写入种子数据失败", it) }
            reminderReregistrator.reregisterAll()
        }
        appScope.launch { recycleBinJanitor.purgeExpired() }
        // 冷启动刷新桌面小组件（系统另有 30 分钟周期刷新兜底）
        appScope.launch {
            com.self.profile.widget.RecentWidgetUpdater.update(applicationContext, personRepo, reminderRepo)
        }
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
