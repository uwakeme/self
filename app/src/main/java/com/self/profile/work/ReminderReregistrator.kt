package com.self.profile.work

import android.content.Context
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.util.ReminderCalc
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 冷启动重注册器：app 启动时遍历所有 enabled reminder，重新按 next trigger 调度。
 *
 * 原因：WorkManager 的任务在系统重启 / app 卸载重装后会被清掉，
 * 但 Room 里的 reminder 数据还在。需要重新挂上 Worker。
 */
@Singleton
class ReminderReregistrator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val reminderRepo: ReminderRepository
) {

    /**
     * 同步快照所有 enabled reminder，按 next trigger 重新调度。
     *
     * - next trigger == null（如 NONE 重复且已过）→ 自动 disable
     * - next trigger != null → enqueue Worker
     */
    suspend fun reregisterAll() {
        val now = System.currentTimeMillis()
        val enabled = reminderRepo.observeEnabled().first()
        enabled.forEach { reminder ->
            val next = ReminderCalc.nextTriggerMillis(reminder)
            if (next == null) {
                // 不重复且已过 → 关闭
                reminderRepo.setEnabled(reminder.id, false, now)
            } else {
                ReminderScheduler.scheduleReminder(appContext, reminder.id, next)
            }
        }
    }
}
