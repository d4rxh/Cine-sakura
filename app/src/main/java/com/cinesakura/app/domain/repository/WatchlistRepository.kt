package com.cinesakura.app.domain.repository

import android.content.Context
import com.cinesakura.app.domain.model.MediaItem
import com.cinesakura.app.domain.model.WatchHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WatchlistRepository(context: Context) {

    private val watchlistFile = File(context.filesDir, "cinesakura_watchlist.json")
    private val historyFile = File(context.filesDir, "cinesakura_history.json")

    private val _watchlistFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    val watchlistFlow: StateFlow<List<MediaItem>> = _watchlistFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<WatchHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<WatchHistoryItem>> = _historyFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Load watchlist
        if (watchlistFile.exists()) {
            try {
                val array = JSONArray(watchlistFile.readText())
                val list = mutableListOf<MediaItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MediaItem(
                            id = obj.getString("id"),
                            imdbId = obj.getString("imdbId"),
                            title = obj.getString("title"),
                            type = obj.getString("type"),
                            poster = obj.getString("poster"),
                            banner = obj.optString("banner", null),
                            releaseYear = obj.optString("releaseYear", ""),
                            rating = obj.optString("rating", "N/A"),
                            description = obj.optString("description", "")
                        )
                    )
                }
                _watchlistFlow.value = list
            } catch (_: Exception) {}
        }

        // Load watch history
        if (historyFile.exists()) {
            try {
                val array = JSONArray(historyFile.readText())
                val list = mutableListOf<WatchHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        WatchHistoryItem(
                            mediaId = obj.getString("mediaId"),
                            imdbId = obj.getString("imdbId"),
                            title = obj.getString("title"),
                            poster = obj.getString("poster"),
                            type = obj.getString("type"),
                            season = obj.optInt("season", 1),
                            episode = obj.optInt("episode", 1),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                _historyFlow.value = list.sortedByDescending { it.timestamp }
            } catch (_: Exception) {}
        }
    }

    suspend fun toggleWatchlist(item: MediaItem) = withContext(Dispatchers.IO) {
        val current = _watchlistFlow.value.toMutableList()
        val exists = current.any { it.imdbId == item.imdbId }
        if (exists) {
            current.removeAll { it.imdbId == item.imdbId }
        } else {
            current.add(0, item)
        }
        _watchlistFlow.value = current

        // Save
        val array = JSONArray()
        for (m in current) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("imdbId", m.imdbId)
                put("title", m.title)
                put("type", m.type)
                put("poster", m.poster)
                put("banner", m.banner)
                put("releaseYear", m.releaseYear)
                put("rating", m.rating)
                put("description", m.description)
            }
            array.put(obj)
        }
        watchlistFile.writeText(array.toString())
    }

    fun isBookmarked(imdbId: String): Boolean {
        return _watchlistFlow.value.any { it.imdbId == imdbId }
    }

    suspend fun recordHistory(item: WatchHistoryItem) = withContext(Dispatchers.IO) {
        val current = _historyFlow.value.filterNot { it.imdbId == item.imdbId }.toMutableList()
        current.add(0, item)
        val trimmed = current.take(50)
        _historyFlow.value = trimmed

        val array = JSONArray()
        for (h in trimmed) {
            val obj = JSONObject().apply {
                put("mediaId", h.mediaId)
                put("imdbId", h.imdbId)
                put("title", h.title)
                put("poster", h.poster)
                put("type", h.type)
                put("season", h.season)
                put("episode", h.episode)
                put("timestamp", h.timestamp)
            }
            array.put(obj)
        }
        historyFile.writeText(array.toString())
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _historyFlow.value = emptyList()
        if (historyFile.exists()) historyFile.delete()
    }
}
