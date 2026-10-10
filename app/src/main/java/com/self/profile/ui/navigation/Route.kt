package com.self.profile.ui.navigation

/**
 * 应用路由表。
 *
 * Bottom Tab 用顶级 5 个 destination；子页面（人物详情等）走 NavHost 内层。
 */
sealed class Route(val path: String) {
    // 顶级 Tab
    data object Home : Route("home")
    /**
     * Self tab 路由：默认 `self`，可加 `?scrollTo=<moduleName>` 跳到指定 section。
     * - scrollTo = "notes"          → 滚到"随想笔记"
     * - scrollTo = "anniversaries"  → 滚到"纪念日"
     */
    data object Self : Route("self?scrollTo={scrollTo}") {
        const val ARG_SCROLL_TO = "scrollTo"
        fun build(scrollTo: String? = null): String =
            if (scrollTo.isNullOrBlank()) "self" else "self?scrollTo=$scrollTo"
    }
    data object People : Route("people")
    data object Map : Route("map")
    data object Settings : Route("settings")

    // 子页面
    data object PersonDetail : Route("people/{personId}") {
        const val ARG_PERSON_ID = "personId"
        fun build(personId: String) = "people/$personId"
    }

    /**
     * 添加人物页路由。**不能**放回 `people/` 命名空间。
     *
     * 历史原因：原路径为 `people/add`，而 [PersonDetail] 的 `people/{personId}` 在
     * AppNav 里注册得更早。Navigation Compose 按**节点注册顺序**取第一个
     * `matchRoute` 命中，没有「字面量优先于参数」规则，于是 `navigate("people/add")`
     * 会命中详情路由、`personId="add"`，FAB 点开变成详情页并提示「人物不存在」。
     * 改成 `people/new` 之类同样无效——仍是 `people/` 下的单段路径。
     * 现改为顶级 `person-add`，与 `people/{personId}` 不再共享命名空间。
     */
    data object AddPerson : Route("person-add")

    data object RecycleBin : Route("settings/recycle-bin")

    data object GlobalSearch : Route("search")
}

/**
 * 5 个底部 Tab 的描述，用于 BottomBar 渲染。
 */
data class BottomTab(
    val route: Route,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
