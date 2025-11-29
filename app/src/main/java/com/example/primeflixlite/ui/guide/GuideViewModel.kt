package com.example.primeflixlite.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuideChannelItem(
    val channel: Channel,
    val programmes: List<Programme> = emptyList()
)

data class GuideUiState(
    val channels: List<GuideChannelItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategory: String = "All"
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState = _uiState.asStateFlow()

    // Cache of all live channels to allow filtering
    private var allLiveChannels: List<Channel> = emptyList()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // We only want LIVE channels for the guide
            repository.playlists.collect { playlists ->
                if (playlists.isNotEmpty()) {
                    // Just grabbing from the first playlist for now for simplicity
                    // In a real multi-playlist app, you'd merge or select.
                    repository.getChannels(playlists.first().url).collect { channels ->
                        allLiveChannels = channels.filter { it.type == StreamType.LIVE }
                        filterChannels()
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        filterChannels()
    }

    private fun filterChannels() {
        viewModelScope.launch {
            val category = _uiState.value.selectedCategory
            val filtered = if (category == "All") {
                allLiveChannels
            } else {
                allLiveChannels.filter { it.group == category }
            }

            // Build the initial list without programs to show UI fast
            var guideItems = filtered.map { GuideChannelItem(it) }
            _uiState.value = _uiState.value.copy(channels = guideItems, isLoading = false)

            // Now asynchronously fetch EPG for these channels (Next 4 hours only)
            val now = System.currentTimeMillis()
            val end = now + (4 * 60 * 60 * 1000) // 4 hours ahead

            // We fetch in parallel-ish chunks to avoid freezing UI
            val updatedItems = guideItems.map { item ->
                val epgId = item.channel.relationId ?: item.channel.title
                // Add the missing method to Repository next!
                val programs = repository.getProgrammesForChannel(epgId, now, end)
                item.copy(programmes = programs)
            }

            _uiState.value = _uiState.value.copy(channels = updatedItems)
        }
    }
}