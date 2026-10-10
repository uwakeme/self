package com.self.profile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.self.profile.MainActivity
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.ui.home.HomeScreen
import com.self.profile.ui.map.RelationshipMapScreen
import com.self.profile.ui.people.AddPersonScreen
import com.self.profile.ui.people.PeopleGalleryScreen
import com.self.profile.ui.people.PersonDetailScreen
import com.self.profile.ui.self.SelfDetailScreen
import com.self.profile.ui.search.GlobalSearchScreen
import com.self.profile.ui.settings.RecycleBinScreen
import com.self.profile.ui.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 深链辅助：reminderId → personId */
@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository
) : ViewModel() {
    suspend fun personIdOf(reminderId: String): String? =
        reminderRepo.getById(reminderId)?.personId
}

/**
 * 应用顶层 NavHost。
 *
 * 用 Scaffold 包 5 Tab + BottomBar。子页面（人物详情、添加人物）走 NavHost 内层，
 * BottomBar 内部判断当前路由决定是否隐藏。
 *
 * [reminderDeepLink]：通知 / 小组件点击带来的入口标记，消费一次后由
 * [onReminderDeepLinkConsumed] 清空。
 */
@Composable
fun AppNav(
    navController: NavHostController = rememberNavController(),
    reminderDeepLink: String? = null,
    onReminderDeepLinkConsumed: () -> Unit = {}
) {
    val deepLinkVm: DeepLinkViewModel = hiltViewModel()

    // 深链只消费一次：人物提醒 → 人物详情；其余（自定义提醒 / 小组件）→ 提醒中心
    LaunchedEffect(reminderDeepLink) {
        val link = reminderDeepLink ?: return@LaunchedEffect
        when (link) {
            MainActivity.EXTRA_OPEN_REMINDERS ->
                navController.navigate(Route.Settings.path)
            else -> {
                val personId = deepLinkVm.personIdOf(link)
                if (personId != null) {
                    navController.navigate(Route.PersonDetail.build(personId))
                } else {
                    navController.navigate(Route.Settings.path)
                }
            }
        }
        onReminderDeepLinkConsumed()
    }

    Scaffold(
        bottomBar = { SelfBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    onNavigateToSelf = { scrollTo ->
                        navController.navigate(Route.Self.build(scrollTo)) {
                            // 与 BottomBar.kt 标准写法的差异：不带 saveState / restoreState。
                            //
                            // restoreState 不能要：Navigation 2.8.3 的 restoreStateInternal
                            // 是拿 backStackStates 里**旧的** NavBackStackEntryState 重建 entry，
                            // 那份 entry 带的是上一次访问时的 scrollTo，本次跳转的 scrollTo
                            // 会被丢弃（"写笔记"跳过去却停在原 section）。
                            // saveState 同样不能要：它会把本次弹掉的 Self 记到 backStackMap
                            // 里，随后 BottomBar 的 restoreState 会把它捞回来，等于把上面的坑
                            // 从另一条路径重新引入。
                            //
                            // launchSingleTop 本身不丢参数：2.8.3 的 launchSingleTopInternal
                            // 会用新参数重建 entry（沿用旧 entry 的 SavedStateHandle），随后
                            // NavigatorState.onLaunchSingleTop 替换栈顶 entry 并重新 emit，
                            // 所以带 scrollTo 的跳转依旧生效。
                            //
                            // popUpTo 起点（不 saveState）保证 Home 之上最多只有一个 Self
                            // entry，连点多次「写笔记」后返回键一次即退出。
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPeople = { navController.navigate(Route.People.path) },
                    onNavigateToMap = { navController.navigate(Route.Map.path) },
                    onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                    onNavigateToSearch = { navController.navigate(Route.GlobalSearch.path) }
                )
            }

            composable(Route.GlobalSearch.path) {
                GlobalSearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPerson = { person ->
                        if (person.isSelf) {
                            navController.navigate(Route.Self.build(null))
                        } else {
                            navController.navigate(Route.PersonDetail.build(person.id))
                        }
                    }
                )
            }
            composable(
                route = Route.Self.path,
                arguments = listOf(navArgument(Route.Self.ARG_SCROLL_TO) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                val scrollTo = entry.arguments?.getString(Route.Self.ARG_SCROLL_TO)
                SelfDetailScreen(scrollTo = scrollTo)
            }
            composable(Route.People.path) {
                PeopleGalleryScreen(
                    onOpenPerson = { id ->
                        navController.navigate(Route.PersonDetail.build(id))
                    },
                    onAddPerson = {
                        navController.navigate(Route.AddPerson.path)
                    }
                )
            }
            composable(Route.Map.path) {
                RelationshipMapScreen(
                    onOpenPerson = { id ->
                        navController.navigate(Route.PersonDetail.build(id))
                    }
                )
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    onNavigateToRecycleBin = { navController.navigate(Route.RecycleBin.path) }
                )
            }
            composable(Route.RecycleBin.path) {
                RecycleBinScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Route.PersonDetail.path,
                arguments = listOf(navArgument(Route.PersonDetail.ARG_PERSON_ID) {
                    type = NavType.StringType
                })
            ) { entry ->
                val personId = entry.arguments?.getString(Route.PersonDetail.ARG_PERSON_ID).orEmpty()
                PersonDetailScreen(
                    personId = personId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.AddPerson.path) {
                AddPersonScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
