package com.example.primeflixlite.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.MediaMetadata
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
    val error: String? = null,
    // [New] Metadata fields
    val metadata: MediaMetadata? = null,
    val versions: List<Channel> = emptyList() // For Movies: 4K, 1080p, etc.
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    var selectedSeason = 1

    fun loadContent(channel: Channel) {
        _currentChannel.value = channel

        // [New] Fetch Rich Metadata (Async)
        fetchMetadata(channel)

        // Branch logic: Series vs Movie
        if (channel.type == StreamType.SERIES.name) {
            loadEpisodes(channel)
        } else if (channel.type == StreamType.MOVIE.name) {
            loadMovieVersions(channel)
        }
    }

    fun loadChannelById(id: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(id)
            if (channel != null) {
                loadContent(channel)
            }
        }
    }

    fun toggleFavorite() {
        val current = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            _currentChannel.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    // --- METADATA LOGIC ---
    private fun fetchMetadata(channel: Channel) {
        viewModelScope.launch {
            // Use canonical title if available (deduplicated), otherwise raw title
            val queryTitle = channel.canonicalTitle ?: channel.title
            val type = if (channel.type == StreamType.SERIES.name) "tv" else "movie"

            val meta = repository.getMetadata(queryTitle, type)
            _uiState.value = _uiState.value.copy(metadata = meta)
        }
    }

    // --- MOVIE VERSION LOGIC ---
    private fun loadMovieVersions(channel: Channel) {
        viewModelScope.launch {
            // If we have a canonical title, find all variants (4K, 1080p)
            if (channel.canonicalTitle != null) {
                val versions = repository.getVersions(
                    channel.playlistUrl,
                    StreamType.MOVIE,
                    channel.canonicalTitle
                )
                // If versions found, update state. If not, fallback to current channel.
                val finalVersions = if (versions.isNotEmpty()) versions else listOf(channel)
                _uiState.value = _uiState.value.copy(versions = finalVersions)
            } else {
                _uiState.value = _uiState.value.copy(versions = listOf(channel))
            }
        }
    }

    // --- SERIES LOGIC ---
    private fun loadEpisodes(channel: Channel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val seriesId = channel.streamId.toIntOrNull() ?: 0
                if (seriesId != 0) {
                    val episodes = repository.getSeriesEpisodes(channel.playlistUrl, seriesId)
                    _uiState.value = _uiState.value.copy(
                        episodes = episodes,
                        isLoading = false
                    )

                    episodes.minByOrNull { it.season }?.let {
                        selectedSeason = it.season
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Invalid Series ID",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load episodes: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun getSeasons(): List<Int> {
        return uiState.value.episodes.map { it.season }.distinct().sorted()
    }

    fun getEpisodesForSeason(season: Int): List<XtreamChannelInfo.Episode> {
        return uiState.value.episodes
            .filter { it.season == season }
            .sortedBy { it.episodeNum }
    }

    fun selectSeason(season: Int) {
        selectedSeason = season
    }
}