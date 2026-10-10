package com.self.profile.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.self.profile.R
import com.self.profile.ui.theme.WarmJournalColors

/**
 * 5 个 Tab 的配置。
 * selected / unselected 用不同 Icon 提升视觉反馈。
 */
data class TabSpec(
    val route: Route,
    val labelRes: Int,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector
)

val bottomTabs: List<TabSpec> = listOf(
    TabSpec(Route.Home, R.string.tab_home, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
    TabSpec(Route.Self, R.string.tab_self, Icons.Outlined.PersonPin, Icons.Filled.PersonPin),
    TabSpec(Route.People, R.string.tab_people, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
    TabSpec(Route.Map, R.string.tab_map, Icons.Outlined.Hub, Icons.Filled.Hub),
    TabSpec(Route.Settings, R.string.tab_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
)

/**
 * 显示 BottomBar 的路由白名单：只有 5 个主 Tab 本身才显示。
 *
 * 用白名单而非黑名单（原来只挡 `people/` 前缀），否则新增子路由
 * （全局搜索 `search`、回收站 `settings/recycle-bin` 等）会漏出底部 Tab。
 */
val BOTTOM_BAR_VISIBLE_ROUTES = setOf(
    Route.Home.path,
    Route.Self.path,
    Route.People.path,
    Route.Map.path,
    Route.Settings.path
)

@Composable
fun SelfBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    // destination.route 返回的是注册时的 pattern 而非用户实际导航到的具体路径，
    // 所以 Route.Self 的 `self?scrollTo={scrollTo}` 可以直接进白名单
    val currentRoute = backStackEntry?.destination?.route

    // 子页面（人物详情、添加人物、全局搜索、回收站）时不显示 BottomBar
    if (currentRoute == null || currentRoute !in BOTTOM_BAR_VISIBLE_ROUTES) {
        return
    }

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmJournalColors.Bg,
        tonalElevation = 0.dp
    ) {
        bottomTabs.forEach { tab ->
            val selected = backStackEntry?.destination?.hierarchy
                ?.any { it.route == tab.route.path } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    // 标准 BottomNav 写法：popUpTo 起点 + saveState + restoreState
                    // launchSingleTop 顶替"已经在当前 tab 不重复 push"的判断。
                    // Self 的 path 是带占位符的 pattern（self?scrollTo={scrollTo}），
                    // 不能直接 navigate 字面量，要走 build() 拿干净路径
                    val target = if (tab.route is Route.Self) {
                        Route.Self.build(null)
                    } else {
                        tab.route.path
                    }
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WarmJournalColors.Primary,
                    selectedTextColor = WarmJournalColors.Primary,
                    indicatorColor = WarmJournalColors.PrimaryLightest,
                    unselectedIconColor = WarmJournalColors.TextTertiary,
                    unselectedTextColor = WarmJournalColors.TextTertiary
                )
            )
        }
    }
}
