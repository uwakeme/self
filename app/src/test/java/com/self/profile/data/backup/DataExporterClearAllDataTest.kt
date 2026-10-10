package com.self.profile.data.backup

import android.content.Context
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.util.PhotoStorage
import com.self.profile.work.ReminderReregistrator
import com.self.profile.work.ReminderScheduler
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [DataExporter.clearAllData] 的「清库后不得复活种子」回归测试。
 *
 * 背景：seedIfEmpty() 的判据是「库为空 且 user_initialized 开关为 false」。
 * 清库把 persons 表清空，如果不同时把开关置位，下次冷启动 5 个 demo 人物会原样回来，
 * 用户以为清干净了其实没有——这正是本任务要消灭的行为。
 *
 * 这里钉死 clearAllData() 必须自己把开关置位，不依赖"用户以前播种过"这个前提。
 */
class DataExporterClearAllDataTest {

    private val appContext: Context = mockk(relaxed = true)
    private val personRepo: PersonRepository = mockk(relaxed = true)
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val settingsStore: SettingsDataStore = mockk(relaxed = true)
    private val photoStorage: PhotoStorage = mockk(relaxed = true)
    private val reregistrator: ReminderReregistrator = mockk(relaxed = true)

    @TempDir
    lateinit var photosDir: File

    @BeforeEach
    fun setUp() {
        every { personRepo.observeAll() } returns flowOf(emptyList())
        every { reminderRepo.observeAll() } returns flowOf(emptyList())
        every { photoStorage.photosDir() } returns photosDir
        // ReminderScheduler 是 object，方法体里会摸 WorkManager.getInstance()。
        // 纯 JVM 环境没有 WorkManager，mockkObject 在进入方法体前拦掉。
        mockkObject(ReminderScheduler)
        every { ReminderScheduler.cancelAll(any()) } just Runs
    }

    private fun exporter() = DataExporter(
        appContext, personRepo, reminderRepo, settingsStore, photoStorage, reregistrator
    )

    /** 核心红线：清库必须把「已初始化」开关置位，否则种子会在下次冷启动复活 */
    @Test
    fun `清库后把已初始化开关置为 true`() = runBlocking {
        exporter().clearAllData()

        coVerify(exactly = 1) { settingsStore.setUserInitialized(true) }
    }

    /** 开关置位不能顶掉原有的清库动作 */
    @Test
    fun `清库仍然照常删除人物与提醒`() = runBlocking {
        exporter().clearAllData()

        coVerify(exactly = 1) { personRepo.deleteAll() }
        coVerify(exactly = 1) { reminderRepo.deleteAll() }
    }

    /**
     * 「不触碰设置项」只约束用户偏好类设置，深色模式 / 应用锁 / 提醒默认值 / 模块顺序等
     * 一个都不能动。这条守着别让后来者以为「可以顺便清一下设置」而把 app 偏好一起重置掉。
     *
     * 断言按 [SettingsDataStore] 里的**声明顺序**排列，覆盖全部 14 个 `set*` 中的 13 个
     * （第 14 个 `setUserInitialized` 是本次修复刻意要调用的，单独由上面那条用例断言）。
     * 外加 `restoreFrom` —— 它不是 setter，但同样能整包覆盖设置项，一并守住。
     * 以后给 SettingsDataStore 加 setter 时，按这个顺序对照一眼就知道漏没漏。
     */
    @Test
    fun `清库不触碰用户偏好类设置`() = runBlocking {
        exporter().clearAllData()

        coVerify(exactly = 0) { settingsStore.setDarkMode(any()) }
        coVerify(exactly = 0) { settingsStore.setFontScale(any()) }
        coVerify(exactly = 0) { settingsStore.setShowLunarBirthday(any()) }
        coVerify(exactly = 0) { settingsStore.setUserNickname(any()) }
        coVerify(exactly = 0) { settingsStore.setLastBackupTime(any()) }
        coVerify(exactly = 0) { settingsStore.setDefaultAdvanceDays(any()) }
        coVerify(exactly = 0) { settingsStore.setDefaultReminderTime(any()) }
        coVerify(exactly = 0) { settingsStore.setAppLockEnabled(any()) }
        coVerify(exactly = 0) { settingsStore.setAppLockTimeout(any()) }
        coVerify(exactly = 0) { settingsStore.setGallerySortRule(any()) }
        coVerify(exactly = 0) { settingsStore.setGalleryCustomOrder(any()) }
        coVerify(exactly = 0) { settingsStore.setSelfProfileModuleOrder(any()) }
        coVerify(exactly = 0) { settingsStore.setPersonDetailModuleOrder(any()) }
        coVerify(exactly = 0) { settingsStore.restoreFrom(any()) }
    }

    /** 删除计数仍按清库前的快照计算，供设置页展示 */
    @Test
    fun `清库结果里的删除数量与实际一致`() = runBlocking {
        every { personRepo.observeAll() } returns flowOf(List(6) { mockk() })
        every { reminderRepo.observeAll() } returns flowOf(List(3) { mockk() })

        val summary = exporter().clearAllData()

        assertEquals(6, summary.peopleDeleted)
        assertEquals(3, summary.remindersDeleted)
        assertEquals(0, summary.photosDeleted)
    }
}
