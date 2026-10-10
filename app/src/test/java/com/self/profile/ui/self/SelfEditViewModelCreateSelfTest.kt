package com.self.profile.ui.self

import android.content.Context
import com.self.profile.R
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import com.self.profile.util.PhotoStorage
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [SelfEditViewModel.createSelfIfAbsent] 的写入契约回归测试。
 *
 * 背景：self 行原本只由种子数据或备份导入产生。禁止重新播种之后，用户执行过
 * 「清除全部数据」就只能靠导入备份找回档案，没导出过的用户无路可走，所以补了这个入口。
 * 这里钉死三件事：写入的确实是 is_self = 1、已有 self 时绝不写第二行、默认值可编辑。
 */
class SelfEditViewModelCreateSelfTest {

    private val personRepo: PersonRepository = mockk(relaxed = true)
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val settingsStore: SettingsDataStore = mockk(relaxed = true)
    private val photoStorage: PhotoStorage = mockk(relaxed = true)
    private val appContext: Context = mockk(relaxed = true)

    private fun viewModel(existingSelf: Person?, nickname: String): SelfEditViewModel {
        every { personRepo.observeSelf() } returns flowOf(existingSelf)
        every { settingsStore.settings } returns flowOf(AppSettings(userNickname = nickname))
        every { appContext.getString(R.string.self_default_name) } returns "我"
        return SelfEditViewModel(personRepo, reminderRepo, settingsStore, photoStorage, appContext)
    }

    private fun existingSelf(): Person = Person(
        id = "self-old",
        name = "小暖",
        avatarColor = "#C4785B",
        avatarInitial = "小",
        relationshipType = RelationshipType.FRIEND,
        isSelf = true,
        createdAt = 0L,
        updatedAt = 0L
    )

    /** 空态点「建立我的档案」必须真的写出一行 is_self = 1 */
    @Test
    fun `空态下创建会写入 is_self 为 true 的行`() = runBlocking {
        val vm = viewModel(existingSelf = null, nickname = "阿暖")

        vm.createSelfIfAbsent()

        val captured = slot<Person>()
        coVerify(exactly = 1) { personRepo.upsert(capture(captured)) }
        assertTrue(captured.captured.isSelf)
    }

    /** 姓名沿用设置页昵称，首字进头像占位（与 AddPersonViewModel 一致） */
    @Test
    fun `创建时姓名取设置页昵称`() = runBlocking {
        val vm = viewModel(existingSelf = null, nickname = "阿暖")

        vm.createSelfIfAbsent()

        val captured = slot<Person>()
        coVerify(exactly = 1) { personRepo.upsert(capture(captured)) }
        assertEquals("阿暖", captured.captured.name)
        assertEquals("阿", captured.captured.avatarInitial)
    }

    /** 昵称被清成空白时兜底成「我」，别给档案留个空名字 */
    @Test
    fun `昵称为空白时落到字符串兜底名`() = runBlocking {
        val vm = viewModel(existingSelf = null, nickname = "   ")

        vm.createSelfIfAbsent()

        val captured = slot<Person>()
        coVerify(exactly = 1) { personRepo.upsert(capture(captured)) }
        assertEquals("我", captured.captured.name)
    }

    /**
     * 空态按钮连点、或空态期间导入了备份，都不能再写第二行 self。
     */
    @Test
    fun `已有 self 时不写入任何数据`() = runBlocking {
        val vm = viewModel(existingSelf = existingSelf(), nickname = "阿暖")

        vm.createSelfIfAbsent()

        coVerify(exactly = 0) { personRepo.upsert(any()) }
    }

    /** id 必须每次新建，避免撞上种子里的 self-001 */
    @Test
    fun `创建出的 id 不复用种子 id`() = runBlocking {
        val vm = viewModel(existingSelf = null, nickname = "阿暖")

        vm.createSelfIfAbsent()

        val captured = slot<Person>()
        coVerify(exactly = 1) { personRepo.upsert(capture(captured)) }
        assertNotEquals("self-001", captured.captured.id)
        assertTrue(captured.captured.id.isNotBlank())
    }

    /** 建档即视为「已初始化」，seed 判据要跟库状态保持自洽 */
    @Test
    fun `创建成功后置位已初始化开关`() = runBlocking {
        val vm = viewModel(existingSelf = null, nickname = "阿暖")

        vm.createSelfIfAbsent()

        coVerify(exactly = 1) { settingsStore.setUserInitialized(true) }
    }
}
