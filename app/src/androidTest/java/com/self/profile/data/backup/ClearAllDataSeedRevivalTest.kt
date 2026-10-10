package com.self.profile.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.local.AppDatabase
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.data.seed.SeedData
import com.self.profile.testutil.createInMemoryDatabase
import com.self.profile.testutil.targetContext
import com.self.profile.util.PhotoStorage
import com.self.profile.work.ReminderReregistrator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 「清库后种子不复活」的接线层测试。
 *
 * JVM 单测（`app/src/test/.../SeedDataUserInitializedTest`、`DataExporterClearAllDataTest`）
 * 已经用 mock 钉住了两边的**判据**：seedIfEmpty 认开关、clearAllData 会置开关。
 * 但 mock 答不了接线本身的问题——判据落到真表上、真 DataStore 键上之后，
 * 「清库 → 再冷启动」这条链到底会不会把 demo 人物塞回来。
 *
 * 这里用真 DataStore + in-memory Room 把整条链跑一遍。
 *
 * 副作用（可接受）：[SettingsDataStore] 走的是与 App 共用的真实 DataStore 文件，
 * [DataExporter.clearAllData] 会撤掉全部提醒调度并清空 `files/photos`。
 * 只有主动跑 connectedAndroidTest 才会发生，且 @After 会把开关恢复成 true。
 */
@RunWith(AndroidJUnit4::class)
class ClearAllDataSeedRevivalTest {

    private lateinit var db: AppDatabase
    private lateinit var personRepo: PersonRepository
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var settingsStore: SettingsDataStore
    private lateinit var seedData: SeedData
    private lateinit var exporter: DataExporter

    @Before
    fun setUp() = runBlocking {
        val context = targetContext()
        db = createInMemoryDatabase()
        personRepo = PersonRepository(db.personDao())
        reminderRepo = ReminderRepository(db.reminderDao())
        settingsStore = SettingsDataStore(context)
        // 复位成「新装」状态：否则 App 启动时已经播种过，开关早就被置 true 了
        settingsStore.setUserInitialized(false)
        seedData = SeedData(personRepo, reminderRepo, settingsStore)
        exporter = DataExporter(
            context,
            personRepo,
            reminderRepo,
            settingsStore,
            PhotoStorage(context),
            ReminderReregistrator(context, reminderRepo)
        )
    }

    @After
    fun tearDown() = runBlocking {
        db.close()
        settingsStore.setUserInitialized(true)
    }

    /** 核心红线：清库 → 模拟下次冷启动 → demo 人物不得回来 */
    @Test
    fun 清库后再次冷启动种子不再复活() = runBlocking {
        seedData.seedIfEmpty()
        assertEquals("首次播种应写入 6 个人物", 6, personRepo.observeAll().first().size)

        exporter.clearAllData()
        assertEquals("清库后人物表应为空", 0, personRepo.observeAll().first().size)
        assertTrue("清库必须把「已初始化」开关置位", settingsStore.isUserInitialized())

        // 模拟下一次冷启动
        seedData.seedIfEmpty()

        assertEquals("清库后人物不得被种子重新播种", 0, personRepo.observeAll().first().size)
        assertEquals("清库后提醒不得被种子重新播种", 0, reminderRepo.observeAll().first().size)
    }

    /** 开关为 false 且库为空时仍要正常播种，别把首次安装的体验改没了 */
    @Test
    fun 新装首启仍然正常播种() = runBlocking {
        seedData.seedIfEmpty()

        assertEquals(6, personRepo.observeAll().first().size)
        assertEquals(3, reminderRepo.observeAll().first().size)
        assertTrue("播种成功后应把开关置位", settingsStore.isUserInitialized())
    }
}