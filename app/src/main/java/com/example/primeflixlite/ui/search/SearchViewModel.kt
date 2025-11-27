package com.example.primeflixlite.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Channel>>(emptyList())
    val results: StateFlow<List<Channel>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(500) // Wait 500ms after user stops typing
                .filter { it.length >= 2 } // Only search if 2+ chars
                .collectLatest { searchQuery ->
                    _isLoading.value = true
                    // Note: You must add 'searchChannels' to Repository in the next step
                    // We are creating the VM first to define the logic
                    repository.searchChannels(searchQuery).collect { channels ->
                        _results.value = channels
                        _isLoading.value = false
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.length < 2) {
            _results.value = emptyList()
        }
    }
}