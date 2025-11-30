package com.example.primeflixlite.ui.details

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.MediaMetadata
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.parser.xtream.XtreamChannelInfo
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsState(
    val isLoading: Boolean = true,
    val metadata: MediaMetadata? = null,
    val versions: List<Channel> = emptyList(), // For Movies: 4K, 1080p, etc.
    val episodes: List<XtreamChannelInfo.Episode> = emptyList() // For Series
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    application: Application,
    private val repository: PrimeFlixRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DetailsState())
    val uiState: StateFlow<DetailsState> = _uiState.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    // UI State for Series selection
    var selectedSeason by mutableStateOf(1)

    // Cached full list of episodes to avoid re-querying
    private var allEpisodes: List<XtreamChannelInfo.Episode> = emptyList()

    fun loadChannelById(id: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(id)
            if (channel != null) {
                loadContent(channel)
            }
        }
    }

    fun loadContent(channel: Channel) {
        _currentChannel.value = channel
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // 1. Fetch Metadata (TMDB)
            // Use canonical title to get better matches (removes "4K", "HEVC" etc tags)
            val meta = repository.getMetadata(
                channel.canonicalTitle ?: channel.title,
                channel.type
            )

            // 2. Fetch Content Specifics
            var versions: List<Channel> = emptyList()
            var episodes: List<XtreamChannelInfo.Episode> = emptyList()

            if (channel.type == StreamType.MOVIE.name) {
                // Get other quality versions of this movie
                versions = repository.getVersions(
                    channel.playlistUrl,
                    StreamType.MOVIE,
                    channel.canonicalTitle ?: channel.title
                )
            } else if (channel.type == StreamType.SERIES.name) {
                // Fetch episodes from Xtream API
                // We use streamId as the series_id
                channel.streamId?.toIntOrNull()?.let { seriesId ->
                    episodes = repository.getSeriesEpisodes(channel.playlistUrl, seriesId)
                }
            }

            allEpisodes = episodes

            // Update State
            _uiState.update {
                it.copy(
                    isLoading = false,
                    metadata = meta,
                    versions = versions,
                    episodes = episodes
                )
            }

            // Reset series selection if new content loaded
            if (episodes.isNotEmpty()) {
                val minSeason = episodes.minOfOrNull { ep -> ep.seasonNum } ?: 1
                selectedSeason = minSeason
            }
        }
    }

    // --- Series Helpers ---

    fun getSeasons(): List<Int> {
        return allEpisodes.map { it.seasonNum }.distinct().sorted()
    }

    fun getEpisodesForSeason(season: Int): List<XtreamChannelInfo.Episode> {
        return allEpisodes.filter { it.seasonNum == season }.sortedBy { it.episodeNum }
    }
}