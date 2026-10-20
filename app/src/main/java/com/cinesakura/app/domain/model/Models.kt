package com.cinesakura.app.domain.model

data class MediaItem(
    val id: String,
    val imdbId: String,
    val title: String,
    val type: String, // "movie" or "series"
    val poster: String,
    val banner: String? = null,
    val releaseYear: String = "",
    val rating: String = "N/A",
    val description: String = "",
    val genres: List<String> = emptyList()
)

data class Episode(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val overview: String = "",
    val thumbnail: String? = null
)

data class MediaDetail(
    val id: String,
    val imdbId: String,
    val title: String,
    val type: String,
    val poster: String,
    val banner: String,
    val releaseYear: String,
    val rating: String,
    val description: String,
    val genres: List<String>,
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val runtime: String = "",
    val trailerUrl: String? = null,
    val episodes: List<Episode> = emptyList(),
    val seasonsCount: Int = 1
)

data class StreamServer(
    val name: String,
    val serverKey: String,
    val embedUrl: String,
    val badge: String = "HD",
    val isHindiDubAvailable: Boolean = false
)

data class WatchHistoryItem(
    val mediaId: String,
    val imdbId: String,
    val title: String,
    val poster: String,
    val type: String,
    val season: Int = 1,
    val episode: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
