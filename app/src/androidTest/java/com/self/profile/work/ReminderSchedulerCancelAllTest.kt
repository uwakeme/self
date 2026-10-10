package com.self.profile.work

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.self.profile.testutil.targetContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * [ReminderScheduler.cancelAll] 的真实行为测试。
 *
 * 背景：调度时只打 per-id tag（`reminder-<id>`），而 cancelAll() 用
 * `cancelAllWorkByTag("reminder")` 撤全部。WorkManager 的 tag 是精确匹配、不做前缀匹配，
 * 于是 cancelAll() 一条都撤不掉——清库后留下触发时读不到数据的孤儿任务。
 *
 * JVM 单测（`app/src/test/.../ReminderSchedulerTagTest`）用 mock 钉住了「调度时
 * 必须打上通用 tag」这个判据，但 mock 的 WorkManager 不会真的入队，
 * 「撤完之后 pending 归零」这种状态断言只能在真 WorkManager 上验。
 *
 * 用的是 App 自己那份 WorkManager（Manifest 里移除了默认初始化器，靠
 * `SelfApp : Configuration.Provider` 按需初始化），不引 `work-testing`。
 */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulerCancelAllTest {

    /**
     * 镜像 [ReminderScheduler] 里的私有常量 TAG_REMINDER。
     *
     * 调度与取消两侧都用这个字面量，所以下面每条用例都先断言「调度完确实能按这个 tag
     * 查到 N 条」——常量一旦改名，前置断言就会红，不会变成一句永远成立的空话。
     */
    private val tagReminder = "reminder"

    /** 7 天后触发，确保测试期间任务不会真的跑起来变成「已完成」 */
    private val triggerAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)

    private val workManager: WorkManager
        get() = WorkManager.getInstance(targetContext())

    @Before
    fun setUp() {
        // App 冷启动时 reregisterAll() 会排一堆提醒，先全撤干净再开始计数
        ReminderScheduler.cancelAll(targetContext())
    }

    @After
    fun tearDown() {
        ReminderScheduler.cancelAll(targetContext())
    }

    /** 未完成（未跑、未取消）的提醒任务数 */
    private fun pendingReminderCount(): Int =
        workManager.getWorkInfosByTag(tagReminder).get()
            .count { !it.state.isFinished }

    /** 核心红线：调度 N 条后 cancelAll()，pending 必须归零 */
    @Test
    fun cancelAll后pending任务归零() = runBlocking {
        repeat(5) { i -> ReminderScheduler.scheduleReminder(targetContext(), "cancel-all-$i", triggerAt) }

        assertEquals("前置条件：5 条都应处于未完成状态", 5, pendingReminderCount())

        ReminderScheduler.cancelAll(targetContext())

        assertEquals("cancelAll 后不应再有未完成的提醒任务", 0, pendingReminderCount())
    }

    /** 加通用 tag 不能误伤按 id 取消：cancel() 只撤那一条 */
    @Test
    fun 按id取消只撤掉那一条() = runBlocking {
        repeat(3) { i -> ReminderScheduler.scheduleReminder(targetContext(), "cancel-one-$i", triggerAt) }
        assertEquals("前置条件：3 条都应处于未完成状态", 3, pendingReminderCount())

        ReminderScheduler.cancel(targetContext(), "cancel-one-1")

        assertEquals("按 id 取消应只撤掉被点名的那一条", 2, pendingReminderCount())
    }

    /** 撤完再撤一次不应报错，也不会把新调度的影响搞乱 */
    @Test
    fun 重复调用cancelAll是幂等的() = runBlocking {
        repeat(2) { i -> ReminderScheduler.scheduleReminder(targetContext(), "cancel-idem-$i", triggerAt) }
        assertEquals("前置条件：2 条都应处于未完成状态", 2, pendingReminderCount())

        ReminderScheduler.cancelAll(targetContext())
        ReminderScheduler.cancelAll(targetContext())

        assertEquals("重复 cancelAll 后 pending 仍应归零", 0, pendingReminderCount())
        assertEquals(
            "被取消的 WorkInfo 仍留在库里，只是状态已终结",
            2,
            cancelledReminderCount()
        )
    }

    /** 处于 CANCELLED 状态的提醒任务数 */
    private fun cancelledReminderCount(): Int =
        workManager.getWorkInfosByTag(tagReminder).get()
            .count { it.state == WorkInfo.State.CANCELLED }
}