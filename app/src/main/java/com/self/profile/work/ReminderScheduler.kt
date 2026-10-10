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
            // 同时打一个通用 tag，供 cancelAll() 一次性撤掉全部提醒。
            // WorkManager 的 tag 是精确匹配、不做前缀匹配，只有上面的 per-id tag
            // （"reminder-<id>"）的话 cancelAllWorkByTag(TAG_REMINDER) 一条都撤不掉。
            // 两个 tag 并存不影响按 id 取消：cancel() 走的是唯一工作名，不是 tag。
            .addTag(TAG_REMINDER)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_PREFIX + reminderId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * 取消一条已调度的 reminder。
     *
     * 走唯一工作名（[WORK_NAME_PREFIX] + id），与 tag 无关，因此
     * [scheduleReminder] 额外打的通用 tag 不影响这里的按 id 取消。
     */
    fun cancel(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + reminderId)
    }

    /**
     * 取消全部 reminder 调度。
     *
     * 依赖 [scheduleReminder] 给每条任务都打了通用 tag [TAG_REMINDER]：WorkManager 的
     * tag 是精确匹配，`TAG_PREFIX + id` 这种 per-id tag 匹配不上 [TAG_REMINDER]。
     */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER)
    }

    private const val TAG_PREFIX = "reminder-"
    private const val WORK_NAME_PREFIX = "reminder-work-"
    private const val TAG_REMINDER = "reminder"
}
