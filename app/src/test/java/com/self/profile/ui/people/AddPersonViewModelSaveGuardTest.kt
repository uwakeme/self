package com.self.profile.ui.people

import com.self.profile.data.repository.PersonRepository
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * [AddPersonViewModel.save] 的写入守卫回归测试。
 *
 * 背景：添加人物页的保存按钮只因「保存中」置灰，姓名不合法时也允许点击，
 * 安全性完全押在 save() 首行的 `if (!isValid() || _saving.value) return` 上。
 * 这里用 mockk 把 [PersonRepository] 挡在边界外，钉死「不合法输入一条都不落库」。
 *
 * 注意：本测试只覆盖「守卫早退」这条路径。守卫一旦放行，save() 会走
 * viewModelScope.launch，需要 Main dispatcher，不属于本测试范围。
 */
class AddPersonViewModelSaveGuardTest {

    private val personRepo: PersonRepository = mockk(relaxed = true)

    /**
     * 空白姓名（含纯空格）必须一条都不写入。
     *
     * 界面判据与写入守卫必须同为「空白」语义：两边若一边用 isEmpty()、一边用
     * isNotBlank()，输入纯空格就会出现「拦了写入但界面无反馈」的静默 no-op。
     */
    @Test
    fun `姓名为空字符串时 save 不写入任何数据`() {
        val vm = AddPersonViewModel(personRepo)

        vm.save(onSaved = {}, onError = {})

        // upsert 是 suspend 函数，校验要用 coVerify（verify 的 lambda 不是挂起上下文）
        coVerify(exactly = 0) { personRepo.upsert(any()) }
    }

    @Test
    fun `姓名只有空白字符时 save 同样不写入任何数据`() {
        val vm = AddPersonViewModel(personRepo)
        vm.setName("   ")

        vm.save(onSaved = {}, onError = {})

        coVerify(exactly = 0) { personRepo.upsert(any()) }
    }

    @Test
    fun `姓名为 tab 与换行等空白字符时 save 不写入任何数据`() {
        val vm = AddPersonViewModel(personRepo)
        vm.setName(" \t\n ")

        vm.save(onSaved = {}, onError = {})

        coVerify(exactly = 0) { personRepo.upsert(any()) }
    }
}