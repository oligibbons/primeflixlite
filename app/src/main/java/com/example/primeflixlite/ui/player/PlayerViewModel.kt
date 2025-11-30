package com.example.primeflixlite.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val currentChannel: Channel? = null,
    val playlistContext: List<Channel> = emptyList(), // Neighboring channels for the drawer
    val isPlaying: Boolean = true
)

class PlayerViewModel(
    application: Application,
    private val repository: PrimeFlixRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    // Load metadata and neighbors based on the raw URL passed to the player
    fun loadContextForUrl(url: String) {
        viewModelScope.launch {
            // 1. Find the channel in DB to get its Title/Logo
            val channel = repository.getChannelByUrl(url)

            if (channel != null) {
                _uiState.update { it.copy(currentChannel = channel) }

                // 2. Load "Context" (Same Category Channels)
                // We fetch the channel's category and load others from it
                // This allows the "Channel Drawer" to show relevant content
                val siblings = repository.getChannelsByCategory(channel.playlistId, channel.category)
                _uiState.update { it.copy(playlistContext = siblings) }
            } else {
                // Fallback for direct URLs not in DB (rare)
                _uiState.update {
                    it.copy(currentChannel = Channel(
                        url = url,
                        title = "Unknown Stream",
                        playlistId = 0,
                        category = "Unknown",
                        type = "LIVE"
                    ))
                }
            }
        }
    }

    fun updateCurrentChannel(channel: Channel) {
        _uiState.update { it.copy(currentChannel = channel) }
    }
}