package com.self.profile.ui.navigation

import androidx.navigation.NavArgument
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 路由命名空间隔离的真机版本。
 *
 * 与 `app/src/test/.../RouteNamespaceIsolationTest`（JVM）分工：
 * JVM 那份验「路由表本身」的隔离不变量，跑得快、能进 CI；
 * 但 `NavDestination.matchRoute` 内部要调 `android.net.Uri.parse`，
 * 纯 JVM 没有真实 Uri 实现，带占位符的匹配跑不了。
 * 这份在设备上补齐那一段：真的让 Navigation 拿真 Uri 去匹配一次。
 */
@RunWith(AndroidJUnit4::class)
class NavGraphRouteResolutionTest {

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

    /**
     * 核心红线：详情路由的 pattern 不能匹配到添加人物路由。
     *
     * 这条**刻意绕开 `findNode`**，直接对带 `{personId}` 占位符的 pattern 调 `matchRoute`。
     *
     * 绕开的原因：`NavGraph.findNode(route)` 是单趟 `firstOrNull`，逐节点判
     * `it.route.equals(route) || it.matchRoute(route) != null`，且详情路由注册在前。
     * 只要 pattern 匹配成立，第一个节点就当场返回——`findNode` 的结果同时受
     * "注册顺序" 和 "该节点自身是否命中" 两个因素影响，不是一条干净的隔离断言。
     * 直接问 pattern 本身，则只检验「命名空间隔离」这一条不变量。
     *
     * 这样即使将来 Navigation 改动 `findNode` 的内部实现（例如改成两趟、
     * 或加字面量优先），这条断言的意图依然成立。
     */
    @Test
    fun 详情路由不匹配添加人物路由() {
        assertNull(
            "`${Route.PersonDetail.path}` 不该匹配 `${Route.AddPerson.path}`",
            personDetailDestination().matchRoute(Route.AddPerson.path)
        )
    }

    /** 隔离不能顺手把详情页改坏：真实 id 仍要匹配上，且 personId 解析正确 */
    @Test
    fun 详情路由仍能匹配真实人物id() {
        val matched = requireNotNull(
            personDetailDestination().matchRoute(Route.PersonDetail.build("p-mom"))
        ) {
            "详情路由应匹配 `people/p-mom`"
        }

        assertEquals(Route.PersonDetail.path, matched.destination.route)
        assertEquals(
            "personId 应解析成 p-mom",
            "p-mom",
            requireNotNull(matched.matchingArgs) { "匹配结果应带参数字典" }
                .getString(Route.PersonDetail.ARG_PERSON_ID)
        )
    }

    /** 通过 NavGraph 解析添加人物路由，命中的必须是添加人物自己 */
    @Test
    fun personAdd解析到添加人物路由() {
        val graph = NavGraph(TestNavigator()).apply {
            addDestination(personDetailDestination())
            addDestination(destination(Route.AddPerson.path))
        }

        assertEquals(
            Route.AddPerson.path,
            graph.findNode(Route.AddPerson.path)?.route
        )
    }

    /** 对照：详情路由经 NavGraph 解析真实 id 仍然落到详情 */
    @Test
    fun 详情路由经图解析真实id仍落到详情() {
        val graph = NavGraph(TestNavigator()).apply {
            addDestination(personDetailDestination())
            addDestination(destination(Route.AddPerson.path))
        }

        val resolved = requireNotNull(graph.findNode(Route.PersonDetail.build("p-lin"))) {
            "`people/p-lin` 应能解析"
        }
        assertEquals(Route.PersonDetail.path, resolved.route)
    }
}