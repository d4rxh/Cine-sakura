package com.cinesakura.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val spotlight: MediaItem? = null,
    val trendingMovies: List<MediaItem> = emptyList(),
    val popularSeries: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val movieRepo = CineSakuraApp.instance.movieRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val movies = movieRepo.getTrendingMovies()
                val series = movieRepo.getPopularSeries()

                val spotlightItem = movies.firstOrNull() ?: series.firstOrNull()
                val trending = if (movies.isNotEmpty()) movies.drop(1) else emptyList()

                _uiState.value = HomeUiState(
                    spotlight = spotlightItem,
                    trendingMovies = trending,
                    popularSeries = series,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load home catalog"
                )
            }
        }
    }
}
