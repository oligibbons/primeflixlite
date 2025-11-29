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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuideItem(
    val channel: Channel,
    val programs: List<Programme> = emptyList()
)

data class GuideUiState(
    val isLoading: Boolean = true,
    val channels: List<GuideItem> = emptyList()
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadGuide()
    }

    private fun loadGuide() {
        viewModelScope.launch {
            repository.playlists.collectLatest { playlists ->
                // Just grab first active playlist for Lite version demo
                val activePlaylist = playlists.firstOrNull()
                if (activePlaylist != null) {
                    repository.getChannelsWithEpg(activePlaylist.url).collect { channelsWithEpg ->
                        val items = channelsWithEpg
                            // FIX: Compare String vs String (.name)
                            .filter { it.channel.type == StreamType.LIVE.name }
                            .map {
                                GuideItem(
                                    channel = it.channel,
                                    programs = it.currentProgram?.let { p -> listOf(p) } ?: emptyList()
                                    // Note: In a full app we'd fetch range of programs, here just current
                                )
                            }

                        _uiState.value = GuideUiState(
                            isLoading = false,
                            channels = items
                        )
                    }
                } else {
                    _uiState.value = GuideUiState(isLoading = false)
                }
            }
        }
    }
}