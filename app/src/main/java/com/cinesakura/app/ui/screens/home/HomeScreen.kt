package com.cinesakura.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesakura.app.domain.model.MediaItem
import com.cinesakura.app.ui.components.HeroSpotlight
import com.cinesakura.app.ui.components.MediaRow
import com.cinesakura.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToDetails: (String, String) -> Unit,
    onNavigateToPlayer: (String, String, Boolean) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🌸 Cine-Sakura",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SakuraLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg.copy(alpha = 0.95f))
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SakuraPink)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Fetching Sakura Streaming Catalog...", color = TextSecondary)
                }
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.errorMessage ?: "", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadHomeData() },
                        colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Spotlight Hero
                uiState.spotlight?.let { spot ->
                    item {
                        HeroSpotlight(
                            item = spot,
                            onWatchClick = {
                                val isTv = spot.type.equals("series", ignoreCase = true) || spot.type.equals("tv", ignoreCase = true)
                                onNavigateToPlayer(spot.imdbId, spot.title, isTv)
                            },
                            onDetailsClick = {
                                onNavigateToDetails(spot.id, spot.type)
                            }
                        )
                    }
                }

                // Trending Movies Row
                if (uiState.trendingMovies.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = "🔥 Trending Movies",
                            items = uiState.trendingMovies,
                            onItemClick = { item -> onNavigateToDetails(item.id, item.type) }
                        )
                    }
                }

                // Popular TV Series Row
                if (uiState.popularSeries.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = "📺 Binge-Worthy TV Series",
                            items = uiState.popularSeries,
                            onItemClick = { item -> onNavigateToDetails(item.id, item.type) }
                        )
                    }
                }

                // Action / Anime Row (take slice of series/movies)
                val animeAction = (uiState.trendingMovies + uiState.popularSeries).shuffled().take(12)
                if (animeAction.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = "⚡ Popular Action & Highlights",
                            items = animeAction,
                            onItemClick = { item -> onNavigateToDetails(item.id, item.type) }
                        )
                    }
                }
            }
        }
    }
}
