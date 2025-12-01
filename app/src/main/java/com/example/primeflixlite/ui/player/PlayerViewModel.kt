package com.example.primeflixlite.ui.player

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

// Combined UI State for the Screen
data class PlayerUiState(
    val currentChannel: Channel? = null,
    val currentProgram: Programme? = null,
    val isPlaying: Boolean = true,
    val resizeMode: Int = 0,
    val playlistContext: List<Channel> = emptyList() // The "neighbors" for the drawer
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var progressJob: Job? = null

    // Called by PlayerScreen to setup data
    fun loadContextForUrl(url: String) {
        viewModelScope.launch {
            val channel = repository.getChannelByUrl(url)
            if (channel != null) {
                // Set initial channel
                _uiState.update { it.copy(currentChannel = channel) }
                loadEpgForChannel(channel)

                // Load siblings for the drawer
                try {
                    repository.getVodChannels(
                        playlistUrl = channel.playlistUrl,
                        type = channel.type,
                        group = channel.group
                    ).collect { neighbors ->
                        _uiState.update { it.copy(playlistContext = neighbors) }
                    }
                } catch (e: Exception) {
                    Log.e("PlayerVM", "Failed to load context", e)
                }
            }
        }
    }

    fun updateCurrentChannel(channel: Channel) {
        _uiState.update { it.copy(currentChannel = channel) }
        loadEpgForChannel(channel)
    }

    private fun initializePlayerSafely(context: Context, channel: Channel) {
        releasePlayer()
        try {
            player = ExoPlayer.Builder(context)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(channel.url))
                    prepare()
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _uiState.update { it.copy(isPlaying = isPlaying) }
                            if (isPlaying) startProgressTracking()
                            else stopProgressTracking()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e("PlayerViewModel", "ExoPlayer Critical Error", error)
                        }
                    })
                }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Crash prevented during player init", e)
        }
    }

    // Called for channel zapping (Prev/Next)
    fun nextChannel() {
        val current = _uiState.value.currentChannel ?: return
        val list = _uiState.value.playlistContext
        if (list.isEmpty()) return

        val index = list.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val nextIndex = if (index + 1 < list.size) index + 1 else 0
            switchChannel(list[nextIndex])
        }
    }

    fun prevChannel() {
        val current = _uiState.value.currentChannel ?: return
        val list = _uiState.value.playlistContext
        if (list.isEmpty()) return

        val index = list.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val prevIndex = if (index - 1 >= 0) index - 1 else list.lastIndex
            switchChannel(list[prevIndex])
        }
    }

    private fun switchChannel(channel: Channel) {
        updateCurrentChannel(channel)
        player?.apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            play()
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.currentChannel ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            // Update local state if needed
        }
    }

    private fun loadEpgForChannel(channel: Channel) {
        viewModelScope.launch {
            val epgId = channel.relationId ?: channel.title
            val program = repository.getCurrentProgram(epgId)
            _uiState.update { it.copy(currentProgram = program) }
        }
    }

    fun playPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seek(position: Long) {
        player?.seekTo(position)
    }

    fun toggleResizeMode() {
        val newMode = (_uiState.value.resizeMode + 1) % 3
        _uiState.update { it.copy(resizeMode = newMode) }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                saveCurrentProgress()
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        saveCurrentProgress()
    }

    private fun saveCurrentProgress() {
        val p = player ?: return
        val c = _uiState.value.currentChannel ?: return
        if (p.duration > 0) {
            viewModelScope.launch {
                repository.saveProgress(c.url, p.currentPosition, p.duration)
            }
        }
    }

    fun releasePlayer() {
        stopProgressTracking()
        player?.release()
        player = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}