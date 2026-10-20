package com.cinesakura.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.MediaItem
import com.cinesakura.app.domain.model.WatchHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel : ViewModel() {

    private val repository = CineSakuraApp.instance.watchlistRepository

    val watchlist: StateFlow<List<MediaItem>> = repository.watchlistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WatchHistoryItem>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromWatchlist(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleWatchlist(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
