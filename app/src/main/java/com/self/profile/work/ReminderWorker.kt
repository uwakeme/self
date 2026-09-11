package com.self.profile.work

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.self.profile.MainActivity
import com.self.profile.R
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Reminder
import com.self.profile.util.ReminderCalc
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 触发提醒的 Worker。
 *
 * - 读 reminder（by id）→ 检查 enabled → 发通知
 * - 按 repeat 规则计算下次触发时间，重新调度（如不重复，触发后 disable）
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepo: ReminderRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val reminder = reminderRepo.getById(reminderId) ?: return Result.success()
        // 已删除（回收站）或已关闭：不通知，也不再续排
        if (!reminder.enabled || reminder.deletedAt != null) return Result.success()

        // 1. 弹通知
        showNotification(reminder)

        // 2. 记录触发时间（reminderRepo.upsert 会做 entity ↔ domain 转换）
        val now = System.currentTimeMillis()
        reminderRepo.upsert(reminder.copy(triggeredAt = now, updatedAt = now))

        // 3. 按 repeat 决定下一步
        val nextTrigger = ReminderCalc.nextTriggerMillis(reminder)
        if (nextTrigger == null) {
            // 不重复 → 关闭
            reminderRepo.setEnabled(reminder.id, false, now)
        } else {
            // 重新调度
            ReminderScheduler.scheduleReminder(
                applicationContext,
                reminder.id,
                nextTrigger
            )
        }

        return Result.success()
    }

    private fun showNotification(reminder: Reminder) {
        val context = applicationContext
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 13+ 通知权限
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(reminder.notes ?: "今天是个重要日子")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(reminder.id.hashCode(), notification)
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
