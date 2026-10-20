package com.cinesakura.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cinesakura.app.domain.model.Episode
import com.cinesakura.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    mediaType: String,
    viewModel: DetailsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onPlayMovie: (String, String, Boolean, Int, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId, mediaType)
    }

    Scaffold(
        containerColor = DarkBg
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SakuraPink)
            }
        } else if (uiState.detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.errorMessage ?: "Details not available", color = TextSecondary)
            }
        } else {
            val detail = uiState.detail!!
            val isTv = detail.type.equals("series", ignoreCase = true) || detail.type.equals("tv", ignoreCase = true)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Backdrop Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        AsyncImage(
                            model = detail.banner.ifEmpty { detail.poster },
                            contentDescription = detail.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            DarkBg.copy(alpha = 0.8f),
                                            DarkBg
                                        )
                                    )
                                )
                        )

                        // Top Back Button & Watchlist Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                            }

                            IconButton(
                                onClick = { viewModel.toggleWatchlist() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (uiState.isBookmarked) SakuraPink else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Title & Info Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Metadata Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (detail.rating.isNotEmpty() && detail.rating != "N/A") {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldRating.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldRating, modifier = Modifier.size(13.dp))
                                        Text(detail.rating, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldRating)
                                    }
                                }
                            }

                            if (detail.releaseYear.isNotEmpty()) {
                                Text(detail.releaseYear, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }

                            Text("•", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SakuraPink.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isTv) "TV SERIES" else "MOVIE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuraPink,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (detail.runtime.isNotEmpty()) {
                                Text("•", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(detail.runtime, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }

                        // Genres Chips
                        if (detail.genres.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(detail.genres) { g ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkCard,
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Text(
                                            text = g,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Play Action Button
                        Button(
                            onClick = {
                                onPlayMovie(detail.imdbId, detail.title, isTv, uiState.selectedSeason, 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTv) "Start Watching (S${uiState.selectedSeason}:E1)" else "Play Movie in HD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Synopsis
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )

                        // Cast
                        if (detail.cast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Starring",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = detail.cast.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // If TV Series: Seasons & Episodes Section
                if (isTv && detail.episodes.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            // Season selector chips
                            if (detail.seasonsCount > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items((1..detail.seasonsCount).toList()) { s ->
                                        FilterChip(
                                            selected = uiState.selectedSeason == s,
                                            onClick = { viewModel.setSelectedSeason(s) },
                                            label = { Text("Season $s") },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SakuraPink,
                                                selectedLabelColor = TextPrimary,
                                                containerColor = DarkCard,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Episodes list for selected season
                    val seasonEpisodes = detail.episodes.filter { it.seasonNumber == uiState.selectedSeason }
                    val displayEpisodes = if (seasonEpisodes.isNotEmpty()) seasonEpisodes else detail.episodes.take(24)

                    items(displayEpisodes, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onPlayMovie(detail.imdbId, detail.title, true, ep.seasonNumber, ep.episodeNumber)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceVariant
                                ) {
                                    Text(
                                        text = "E${ep.episodeNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SakuraLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (ep.overview.isNotEmpty()) {
                                        Text(
                                            text = ep.overview,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = SakuraPink,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
