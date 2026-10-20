package com.cinesakura.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
    data object Settings : Screen("settings")

    data object Details : Screen("details/{id}/{type}") {
        fun createRoute(id: String, type: String = "movie") = "details/$id/$type"
    }

    data object Player : Screen("player/{imdbId}/{title}/{isTv}/{season}/{episode}") {
        fun createRoute(imdbId: String, title: String, isTv: Boolean, season: Int = 1, episode: Int = 1): String {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "player/$imdbId/$encodedTitle/$isTv/$season/$episode"
        }
    }
}
