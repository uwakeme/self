package com.self.profile.ui.navigation

import androidx.navigation.NavArgument
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavType
import androidx.navigation.Navigator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * 路由命名空间隔离回归测试。
 *
 * 背景：添加人物页原路径是 `people/add`，而人物详情是 `people/{personId}`。
 * 两者共享 `people/` 命名空间时，添加人物页会被详情页吃掉（`personId = "add"`），
 * FAB 点开变成详情页并提示「人物不存在」。修法是把 [Route.AddPerson] 移出
 * `people/` 命名空间（现为顶级 `person-add`）。
 *
 * 本测试钉死这个修法不被将来新增的 `people/xxx` 单段路由破坏。
 *
 * 为什么能在纯 JVM 跑：复刻 AppNav 的注册顺序，把 destination 塞进 [NavGraph]，
 * 再用 Navigation 解析字符串路由的入口 [NavGraph.findNode]。
 * 整条链路不碰 `android.content.Context`，因此不需要设备、不需要 Robolectric。
 *
 * 覆盖边界（刻意为之）：`NavDestination.matchRoute` 内部会调 `android.net.Uri.parse`，
 * 纯 JVM 下没有真实 Uri 实现，带占位符的匹配跑不了。那部分放在 androidTest
 * `NavGraphRouteResolutionTest` 里用真机 Uri 验。
 */
class RouteNamespaceIsolationTest {

    /** [NavGraph] 构造需要一个 Navigator；这里只要占位实现，从不真正 attach 到 NavController */
    @Navigator.Name("RouteTestNavigator")
    private class TestNavigator : Navigator<NavGraph>() {
        override fun createDestination(): NavGraph = NavGraph(this)
    }

    /** 注意：`NavDestination(String)` 的入参是 navigatorName，路由要走 route 属性设置 */
    private fun destination(route: String): NavDestination =
        NavDestination("RouteTestNavigator").apply { this.route = route }

    /** 复刻 AppNav 里 PersonDetail 的注册方式：pattern + 一个 StringType 参数 */
    private fun personDetailDestination(): NavDestination =
        destination(Route.PersonDetail.path).apply {
            addArgument(
                Route.PersonDetail.ARG_PERSON_ID,
                NavArgument.Builder().setType(NavType.StringType).build()
            )
        }

    /** 按 AppNav 里的真实顺序：详情路由先注册，添加人物后注册 */
    private fun navGraphInAppOrder(): NavGraph =
        NavGraph(TestNavigator()).apply {
            addDestination(personDetailDestination())
            addDestination(destination(Route.AddPerson.path))
        }

    /** 核心红线：解析添加人物路由，命中的必须是添加人物自己 */
    @Test
    fun `添加人物路由不会被详情路由吃掉`() {
        val resolved = requireNotNull(navGraphInAppOrder().findNode(Route.AddPerson.path)) {
            "`${Route.AddPerson.path}` 应能解析到某个 destination"
        }

        assertEquals(
            Route.AddPerson.path,
            resolved.route,
            "`${Route.AddPerson.path}` 解析到了 `${resolved.route}`，说明被详情路由吃掉了"
        )
    }

    /**
     * 隔离的真正含义：添加人物路由不在 `people/` 命名空间下。
     * 这是 Task 1 修法本身，也是上面那条断言的根因——将来谁把它改回 `people/xxx`，这条立刻红。
     */
    @Test
    fun `添加人物路由不在 people 命名空间下`() {
        assertFalse(
            Route.AddPerson.path.startsWith("${Route.People.path}/"),
            "AddPerson 必须离开 `${Route.People.path}/`，否则会再次被 `${Route.PersonDetail.path}` 吃掉"
        )
    }

    /**
     * 隔离后结果与注册顺序无关：两种顺序都只解析到添加人物路由。
     * 这条守的是 FIX_PLAN 里的原始故障模型——按顺序取第一个命中。
     * 顺序一变就红，说明有人又把两条路由放回同一个命名空间了。
     */
    @Test
    fun `两种注册顺序下 person-add 都解析到添加人物路由`() {
        val reversed = NavGraph(TestNavigator()).apply {
            addDestination(destination(Route.AddPerson.path))
            addDestination(personDetailDestination())
        }

        assertEquals(
            Route.AddPerson.path,
            navGraphInAppOrder().findNode(Route.AddPerson.path)?.route
        )
        assertEquals(
            Route.AddPerson.path,
            reversed.findNode(Route.AddPerson.path)?.route
        )
    }
}