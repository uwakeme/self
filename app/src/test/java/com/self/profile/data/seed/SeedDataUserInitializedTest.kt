package com.self.profile.data.seed

import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [SeedData.seedIfEmpty] 的播种判据回归测试。
 *
 * 背景：原实现只看「persons 表行数是否为 0」。用户执行「清除全部数据」后表被清空，
 * 下一次冷启动 demo 人物（妈妈 / 小林 …）会原样回来，用户以为清干净了其实没有。
 * 现在判据是「库为空 **且** user_initialized 开关为 false」。
 */
class SeedDataUserInitializedTest {

    private val personRepo: PersonRepository = mockk(relaxed = true)
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val settingsStore: SettingsDataStore = mockk(relaxed = true)

    private fun seedData(userInitialized: Boolean, dbEmpty: Boolean): SeedData {
        coEvery { settingsStore.isUserInitialized() } returns userInitialized
        coEvery { personRepo.isEmpty() } returns dbEmpty
        return SeedData(personRepo, reminderRepo, settingsStore)
    }

    /**
     * 开关已置位（用户播种过或清过库）时，一律不写种子——这条是本次修复的核心红线。
     */
    @Test
    fun `开关已置位时即便库为空也不再播种`() = runBlocking {
        seedData(userInitialized = true, dbEmpty = true).seedIfEmpty()

        coVerify(exactly = 0) { personRepo.upsertAll(any()) }
        coVerify(exactly = 0) { reminderRepo.upsertAll(any()) }
    }

    /** 新装首启：开关默认 false + 库为空 → 照旧写入 6 个人物 + 3 条提醒 */
    @Test
    fun `新装首启开关为 false 且库为空时正常播种`() = runBlocking {
        seedData(userInitialized = false, dbEmpty = true).seedIfEmpty()

        val persons = slot<List<com.self.profile.domain.model.Person>>()
        coVerify(exactly = 1) { personRepo.upsertAll(capture(persons)) }
        coVerify(exactly = 1) { reminderRepo.upsertAll(any()) }
        assertEquals(6, persons.captured.size)
        assertEquals(1, persons.captured.count { it.isSelf })
    }

    /** 播种成功后必须置位开关，否则「清库 → 冷启动」依然会复活 */
    @Test
    fun `播种成功后把开关置为 true`() = runBlocking {
        seedData(userInitialized = false, dbEmpty = true).seedIfEmpty()

        coVerify(exactly = 1) { settingsStore.setUserInitialized(true) }
    }

    /**
     * 升级安装：库里已有数据、开关仍是 false。不播种是对的，但必须顺手把开关补成 true，
     * 否则开关长期停在 false，用户日后清库后种子又会在下次冷启动复活。
     */
    @Test
    fun `升级安装库非空时不播种但补齐开关`() = runBlocking {
        seedData(userInitialized = false, dbEmpty = false).seedIfEmpty()

        coVerify(exactly = 0) { personRepo.upsertAll(any()) }
        coVerify(exactly = 0) { reminderRepo.upsertAll(any()) }
        coVerify(exactly = 1) { settingsStore.setUserInitialized(true) }
    }

    /** 开关已 true + 库非空：完全不作为，连开关都不重复写 */
    @Test
    fun `开关已置位且库非空时完全空转`() = runBlocking {
        seedData(userInitialized = true, dbEmpty = false).seedIfEmpty()

        coVerify(exactly = 0) { personRepo.upsertAll(any()) }
        coVerify(exactly = 0) { settingsStore.setUserInitialized(any()) }
    }
}
