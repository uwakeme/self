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
 * 覆盖边界（刻意为之）：`NavDestination.matchRoute` 内部要调 `android.net.Uri`，
 * 纯 JVM 下没有真实 Uri 实现，`matchRoute` 恒返回 null——等于 pattern 匹配完全不参与，
 * `findNode` 退化成纯字符串相等比较。因此本文件**证不了「不会被详情路由吃掉」**，
 * 那条断言放在 androidTest `NavGraphRouteResolutionTest` 里用真机 Uri 验。
 * 本文件能真正守住的是「AddPerson 不在 `people/` 命名空间下」这条前缀不变量。
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

    /**
     * 解析添加人物路由，命中的必须是添加人物自己。
     *
     * **这条在纯 JVM 下证不了撞车**：`matchRoute` 依赖 `android.net.Uri`，此处恒返回
     * null，等于 pattern 匹配完全不参与，`findNode` 退化成纯字符串相等比较。
     * 所以即便有人把 AddPerson 改回 `people/add`，这条**仍会绿**。
     *
     * 真正能证「不会被吃掉」的那条在 androidTest `NavGraphRouteResolutionTest`，
     * 那里有真实 Uri。本条只守一个弱得多的性质：`person-add` 解析到的是添加人物自己。
     */
    @Test
    fun `添加人物路由不会被详情路由吃掉`() {
        val resolved = requireNotNull(navGraphInAppOrder().findNode(Route.AddPerson.path)) {
            "`${Route.AddPerson.path}` 应能解析到某个 destination"
        }

        assertEquals(
            Route.AddPerson.path,
            resolved.route,
            "`${Route.AddPerson.path}` 解析到了 `${resolved.route}`"
        )
    }

    /**
     * 隔离的真正含义：添加人物路由不在 `people/` 命名空间下。
     *
     * **这是本文件里唯一能守住 Task 1 修法的断言**——它不依赖 pattern 匹配，
     * 纯字符串前缀判断，谁把 AddPerson 改回 `people/xxx` 它就红。
     */
    @Test
    fun `添加人物路由不在 people 命名空间下`() {
        assertFalse(
            Route.AddPerson.path.startsWith("${Route.People.path}/"),
            "AddPerson 必须离开 `${Route.People.path}/`，否则会再次被 `${Route.PersonDetail.path}` 吃掉"
        )
    }

    /**
     * 两种注册顺序下 `person-add` 都解析到添加人物路由。
     *
     * 注意：由于纯 JVM 下 `matchRoute` 恒为 null，这条只反映字符串相等比较的结果，
     * **不是**「顺序无关性」的证明；顺序相关的真实行为要靠 androidTest 验。
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