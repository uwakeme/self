package com.self.profile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.self.profile.MainActivity
import com.self.profile.R
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.util.ReminderCalc
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 「近期提醒」桌面小组件（Phase 3.4）。
 *
 * 内容：人物 / 提醒数量 + 最近 2 条提醒（倒计时按"下一次正日"计算，农历自动换算）。
 * 刷新时机：系统周期（30 分钟）+ 每次冷启动 [SelfApp] 主动刷一次。
 * 用经典 RemoteViews 而非 Glance：不引新依赖，规避 Compose 版本耦合。
 */
@AndroidEntryPoint
class RecentWidgetProvider : BroadcastReceiver() {

    @Inject
    lateinit var personRepo: PersonRepository

    @Inject
    lateinit var reminderRepo: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppWidgetManager.ACTION_APPWIDGET_UPDATE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecentWidgetUpdater.update(
                    context, personRepo, reminderRepo
                )
            } finally {
                pending.finish()
            }
        }
    }
}

/** 小组件渲染逻辑，供 Provider 与应用冷启动共用。 */
object RecentWidgetUpdater {

    suspend fun update(
        context: Context,
        personRepo: PersonRepository,
        reminderRepo: ReminderRepository
    ) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, RecentWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val people = personRepo.observeAll().first()
        val others = people.count { !it.isSelf }
        val enabled = reminderRepo.observeEnabled().first()
        val today = LocalDate.now()
        val upcoming = enabled
            .map { r -> r to ReminderCalc.nextOccurrenceDate(r, today) }
            .filter { (_, d) -> !d.isBefore(today) }
            .sortedBy { (_, d) -> d.toEpochDay() }
            .take(2)

        val lines = upcoming.map { (r, d) ->
            val days = d.toEpochDay() - today.toEpochDay()
            val countdown = when {
                days == 0L -> "今天"
                else -> "还有${days}天"
            }
            "${r.title} · $countdown"
        }

        val views = RemoteViews(context.packageName, R.layout.widget_recent).apply {
            setTextViewText(
                R.id.widget_summary,
                "$others 位人物 · ${enabled.size} 条提醒"
            )
            setTextViewText(R.id.widget_line1, lines.getOrNull(0) ?: context.getString(R.string.widget_no_reminders))
            setTextViewText(R.id.widget_line2, lines.getOrNull(1) ?: "")

            val pi = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    // 点击直达提醒中心（设置页），而不是只回到首页
                    .putExtra(MainActivity.EXTRA_OPEN_REMINDERS, true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            setOnClickPendingIntent(R.id.widget_root, pi)
        }
        mgr.updateAppWidget(ids, views)
    }
}
