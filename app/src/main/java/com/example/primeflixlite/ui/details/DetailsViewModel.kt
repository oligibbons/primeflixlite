package com.example.primeflixlite.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.parser.xtream.XtreamChannelInfo
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val isLoading: Boolean = false,
    val episodes: List<XtreamChannelInfo.Episode> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState = _uiState.asStateFlow()

    // We track the channel object here to update its favorite status
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    var selectedSeason = 1 // Simplified state (could be Flow if needed)

    fun loadContent(channel: Channel) {
        _currentChannel.value = channel

        if (channel.type == StreamType.SERIES) {
            loadEpisodes(channel)
        }
    }

    fun toggleFavorite() {
        val current = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            // Update local object immediately
            _currentChannel.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    private fun loadEpisodes(channel: Channel) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState(isLoading = true)
            try {
                // Parse series ID safely
                val seriesId = channel.streamId?.toIntOrNull() ?: 0
                if (seriesId != 0) {
                    val episodes = repository.getSeriesEpisodes(channel.playlistUrl, seriesId)
                    _uiState.value = DetailsUiState(episodes = episodes)

                    // Auto-select first available season
                    episodes.minByOrNull { it.season }?.let {
                        selectedSeason = it.season
                    }
                } else {
                    _uiState.value = DetailsUiState(error = "Invalid Series ID")
                }
            } catch (e: Exception) {
                _uiState.value = DetailsUiState(error = "Failed to load episodes: ${e.message}")
            }
        }
    }

    fun getSeasons(): List<Int> {
        return uiState.value.episodes.map { it.season }.distinct().sorted()
    }

    fun getEpisodesForSeason(season: Int): List<XtreamChannelInfo.Episode> {
        return uiState.value.episodes.filter { it.season == season }.sortedBy { it.episode_num }
    }

    // Helper to update the season selection from UI
    fun selectSeason(season: Int) {
        selectedSeason = season
    }
}