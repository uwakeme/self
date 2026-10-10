package com.self.profile.work

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [ReminderScheduler.scheduleReminder] 的 tag 契约回归测试。
 *
 * 背景：调度时只打了 per-id tag（`"reminder-<id>"`），而 cancelAll() 用
 * `cancelAllWorkByTag("reminder")` 撤全部。WorkManager 的 tag 是精确匹配、不做前缀匹配，
 * 于是 cancelAll() 一条都撤不掉——清库后留下触发时读不到数据的孤儿任务。
 *
 * 这里不跑真实 WorkManager（纯 JVM 环境没有它），而是拦下静态的 `WorkManager.getInstance`，
 * 把真正构造出来的 [OneTimeWorkRequest] 抓出来检查 tag，顺带钉死按 id 取消走的是唯一工作名。
 */
class ReminderSchedulerTagTest {

    private val context: Context = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        // workManager 是 relaxed mock，enqueueUniqueWork / cancelUniqueWork 返回的
        // Operation 已由 mockk 自动造好，不需要额外 stub 返回值。
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    /** 通用 tag 是 cancelAll() 唯一能匹配上的东西，调度时必须打上 */
    @Test
    fun `调度时同时打上通用 tag`() {
        val request = captureRequest { ReminderScheduler.scheduleReminder(context, "r-1", 0L) }

        assertTrue(
            request.tags.contains("reminder"),
            "任务必须带通用 tag \"reminder\"，否则 cancelAll() 撤不掉，实际 tags=${request.tags}"
        )
    }

    /** per-id tag 要保留：它与通用 tag 是并存关系，不是替代关系 */
    @Test
    fun `调度时保留原有的 per-id tag`() {
        val request = captureRequest { ReminderScheduler.scheduleReminder(context, "r-1", 0L) }

        assertTrue(
            request.tags.contains("reminder-r-1"),
            "任务必须保留 per-id tag，实际 tags=${request.tags}"
        )
    }

    /**
     * 守卫「加通用 tag 不会误伤按 id 取消」：cancel() 走的是唯一工作名，与 tag 无关。
     */
    @Test
    fun `按 id 取消走唯一工作名而非 tag`() {
        ReminderScheduler.cancel(context, "r-1")

        verify(exactly = 1) { workManager.cancelUniqueWork("reminder-work-r-1") }
    }

    /** cancelAll() 按通用 tag 撤全部，不碰唯一工作名 */
    @Test
    fun `cancelAll 按通用 tag 撤销`() {
        ReminderScheduler.cancelAll(context)

        verify(exactly = 1) { workManager.cancelAllWorkByTag("reminder") }
    }

    private inline fun captureRequest(block: () -> Unit): OneTimeWorkRequest {
        val slot = slot<OneTimeWorkRequest>()
        block()
        verify { workManager.enqueueUniqueWork(any<String>(), any(), capture(slot)) }
        return slot.captured
    }
}