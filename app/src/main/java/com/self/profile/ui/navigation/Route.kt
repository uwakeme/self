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

    data object AddPerson : Route("people/add")
    data object ReminderCenter : Route("reminders")
}

/**
 * 5 个底部 Tab 的描述，用于 BottomBar 渲染。
 */
data class BottomTab(
    val route: Route,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
