package com.self.profile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.self.profile.ui.home.HomeScreen
import com.self.profile.ui.map.RelationshipMapScreen
import com.self.profile.ui.people.AddPersonScreen
import com.self.profile.ui.people.PeopleGalleryScreen
import com.self.profile.ui.people.PersonDetailScreen
import com.self.profile.ui.self.SelfDetailScreen
import com.self.profile.ui.search.GlobalSearchScreen
import com.self.profile.ui.settings.RecycleBinScreen
import com.self.profile.ui.settings.SettingsScreen

/**
 * 应用顶层 NavHost。
 *
 * 用 Scaffold 包 5 Tab + BottomBar。子页面（人物详情、添加人物）走 NavHost 内层，
 * BottomBar 内部判断当前路由决定是否隐藏。
 */
@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
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
                    onNavigateToSelf = { scrollTo -> navController.navigate(Route.Self.build(scrollTo)) },
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
