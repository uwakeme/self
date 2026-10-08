package com.self.profile.domain

import com.self.profile.domain.model.SelfModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [SelfModule.orderedFrom] / [SelfModule.orderOf] 归一化逻辑：
 * moveModule 的索引语义（UI 索引 == 渲染索引）依赖这里的行为。
 */
class SelfModuleOrderTest {

    @Test
    fun `默认顺序完整且不重不漏`() {
        val ordered = SelfModule.orderedFrom(SelfModule.defaultOrder)
        assertEquals(SelfModule.values().size, ordered.size)
        assertEquals(SelfModule.values().toList(), ordered)
    }

    @Test
    fun `全无效 id 回退为默认全量`() {
        val ordered = SelfModule.orderedFrom(listOf(99, 0, -1))
        assertEquals(SelfModule.values().toList(), ordered)
    }

    @Test
    fun `无效 id 被丢弃 其余保序`() {
        val ordered = SelfModule.orderedFrom(listOf(99, 2, 11))
        assertEquals(
            listOf(SelfModule.BODY, SelfModule.NOTES),
            ordered.take(2)
        )
        // 缺失的模块全部追加到末尾，总数守恒
        assertEquals(SelfModule.values().size, ordered.size)
    }

    @Test
    fun `缺失模块按声明顺序追加到末尾`() {
        val ordered = SelfModule.orderedFrom(listOf(2, 11))
        assertEquals(SelfModule.BODY, ordered[0])
        assertEquals(SelfModule.NOTES, ordered[1])
        assertEquals(SelfModule.BACKGROUND, ordered[2])
        assertEquals(SelfModule.values().size, ordered.size)
    }

    @Test
    fun `orderOf 与 orderedFrom 互逆`() {
        val order = SelfModule.orderedFrom(listOf(3, 1, 7))
        val roundTrip = SelfModule.orderedFrom(SelfModule.orderOf(order))
        assertEquals(order, roundTrip)
    }
}
