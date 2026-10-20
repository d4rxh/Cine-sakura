package com.cinesakura.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.MediaDetail
import com.cinesakura.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val detail: MediaDetail? = null,
    val selectedSeason: Int = 1,
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class DetailsViewModel : ViewModel() {

    private val movieRepo = CineSakuraApp.instance.movieRepository
    private val watchlistRepo = CineSakuraApp.instance.watchlistRepository

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(id: String, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val detail = movieRepo.getMediaDetails(id, type)
                val bookmarked = watchlistRepo.isBookmarked(detail.imdbId)
                _uiState.value = DetailsUiState(
                    detail = detail,
                    selectedSeason = 1,
                    isBookmarked = bookmarked,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load movie details"
                )
            }
        }
    }

    fun setSelectedSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun toggleWatchlist() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val item = MediaItem(
                id = detail.id,
                imdbId = detail.imdbId,
                title = detail.title,
                type = detail.type,
                poster = detail.poster,
                banner = detail.banner,
                releaseYear = detail.releaseYear,
                rating = detail.rating,
                description = detail.description,
                genres = detail.genres
            )
            watchlistRepo.toggleWatchlist(item)
            _uiState.value = _uiState.value.copy(
                isBookmarked = !_uiState.value.isBookmarked
            )
        }
    }
}
