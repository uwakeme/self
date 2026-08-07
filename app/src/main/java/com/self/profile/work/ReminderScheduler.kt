package com.self.profile.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 提醒调度服务。把 reminder 一次性 + 重复任务交给 WorkManager。
 *
 * WorkManager 比 AlarmManager 在 Doze 模式下更可靠，且能处理约束条件。
 */
object ReminderScheduler {

    /**
     * 调度一条 reminder 在 [triggerAtMillis] 时刻触发。
     * 如果已存在同名任务（reminderId）会被 REPLACE 覆盖。
     */
    fun scheduleReminder(
        context: Context,
        reminderId: String,
        triggerAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val delay = (triggerAtMillis - now).coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_REMINDER_ID, reminderId)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_PREFIX + reminderId)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_PREFIX + reminderId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * 取消一条已调度的 reminder。
     */
    fun cancel(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + reminderId)
    }

    /**
     * 取消全部 reminder 调度。
     */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER)
    }

    private const val TAG_PREFIX = "reminder-"
    private const val WORK_NAME_PREFIX = "reminder-work-"
    private const val TAG_REMINDER = "reminder"
}
