package com.example.primeflixlite.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false // To distinguish "empty start" vs "no results found"
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    // Internal flow for typing debounce
    private val searchFlow = MutableStateFlow("")

    init {
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchFlow
                .debounce(500) // Wait 500ms after last keystroke
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        // Update UI immediately for the text field
        _uiState.value = _uiState.value.copy(query = newQuery)

        // Push to debouncer
        searchFlow.value = newQuery

        // Clear results if empty
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // The repository search is typically exact or LIKE %query%
                // We fetch everything matching the string
                repository.searchChannels(query).collect { hits ->
                    _uiState.value = _uiState.value.copy(
                        results = hits,
                        isLoading = false,
                        hasSearched = true
                    )
                }
            } catch (e: Exception) {
                // Handle error smoothly
                _uiState.value = _uiState.value.copy(isLoading = false, results = emptyList())
            }
        }
    }
}