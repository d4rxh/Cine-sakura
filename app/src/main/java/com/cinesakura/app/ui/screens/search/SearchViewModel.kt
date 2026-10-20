package com.cinesakura.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<MediaItem> = emptyList(),
    val filteredResults: List<MediaItem> = emptyList(),
    val selectedFilter: String = "All", // "All", "Movies", "Series"
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel : ViewModel() {

    private val movieRepo = CineSakuraApp.instance.movieRepository
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), filteredResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = null)
            try {
                val data = movieRepo.searchMedia(newQuery)
                val filtered = applyFilter(data, _uiState.value.selectedFilter)
                _uiState.value = _uiState.value.copy(
                    results = data,
                    filteredResults = filtered,
                    isSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = e.localizedMessage ?: "Failed to perform search"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredResults = applyFilter(_uiState.value.results, filter)
        )
    }

    private fun applyFilter(items: List<MediaItem>, filter: String): List<MediaItem> {
        return when (filter) {
            "Movies" -> items.filter { it.type.equals("movie", ignoreCase = true) }
            "Series" -> items.filter { it.type.equals("series", ignoreCase = true) || it.type.equals("tv", ignoreCase = true) }
            else -> items
        }
    }
}
