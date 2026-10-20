package com.cinesakura.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cinesakura.app.ui.screens.details.DetailsScreen
import com.cinesakura.app.ui.screens.home.HomeScreen
import com.cinesakura.app.ui.screens.player.PlayerScreen
import com.cinesakura.app.ui.screens.search.SearchScreen
import com.cinesakura.app.ui.screens.settings.SettingsScreen
import com.cinesakura.app.ui.screens.watchlist.WatchlistScreen
import com.cinesakura.app.ui.theme.DarkBg
import com.cinesakura.app.ui.theme.DarkSurface
import com.cinesakura.app.ui.theme.SakuraPink
import java.net.URLDecoder

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun CineSakuraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Search", Screen.Search.route, Icons.Default.Search),
        BottomNavItem("Library", Screen.Watchlist.route, Icons.Default.VideoLibrary),
        BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = SakuraPink
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SakuraPink,
                                selectedTextColor = SakuraPink,
                                indicatorColor = SakuraPink.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetails = { id, type ->
                        navController.navigate(Screen.Details.createRoute(id, type))
                    },
                    onNavigateToPlayer = { imdbId, title, isTv ->
                        navController.navigate(Screen.Player.createRoute(imdbId, title, isTv))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToDetails = { id, type ->
                        navController.navigate(Screen.Details.createRoute(id, type))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onMediaClick = { item ->
                        navController.navigate(Screen.Details.createRoute(item.id, item.type))
                    },
                    onPlayHistoryClick = { historyItem ->
                        val isTv = historyItem.type.equals("series", ignoreCase = true)
                        navController.navigate(
                            Screen.Player.createRoute(
                                imdbId = historyItem.imdbId,
                                title = historyItem.title,
                                isTv = isTv,
                                season = historyItem.season,
                                episode = historyItem.episode
                            )
                        )
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "movie" }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: "movie"
                DetailsScreen(
                    mediaId = id,
                    mediaType = type,
                    onNavigateBack = { navController.popBackStack() },
                    onPlayMovie = { imdbId, title, isTv, season, episode ->
                        navController.navigate(
                            Screen.Player.createRoute(imdbId, title, isTv, season, episode)
                        )
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("imdbId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("isTv") { type = NavType.BoolType; defaultValue = false },
                    navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 1 }
                )
            ) { backStackEntry ->
                val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val title = try {
                    URLDecoder.decode(encodedTitle, "UTF-8")
                } catch (_: Exception) {
                    encodedTitle
                }
                val isTv = backStackEntry.arguments?.getBoolean("isTv") ?: false
                val season = backStackEntry.arguments?.getInt("season") ?: 1
                val episode = backStackEntry.arguments?.getInt("episode") ?: 1

                PlayerScreen(
                    imdbId = imdbId,
                    title = title,
                    isTv = isTv,
                    season = season,
                    episode = episode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
