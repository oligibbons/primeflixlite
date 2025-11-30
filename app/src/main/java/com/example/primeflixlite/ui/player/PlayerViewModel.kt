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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<Programme?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    private val _resizeMode = MutableStateFlow(0)
    val resizeMode = _resizeMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying = _isPlaying.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var playlistChannels: List<Channel> = emptyList()
    private var progressJob: Job? = null

    fun initialize(context: Context, channel: Channel) {
        if (player != null && _currentChannel.value?.url == channel.url) return

        _currentChannel.value = channel
        loadEpgForChannel(channel)
        loadPlaylistContext(channel)

        initializePlayerSafely(context, channel)
    }

    // CRITICAL FIX: Allows correct loading from Deep Links or Navigation
    fun loadChannelById(id: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(id)
            if (channel != null) {
                _currentChannel.value = channel
                loadEpgForChannel(channel)
                loadPlaylistContext(channel)
            }
        }
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
                            _isPlaying.value = isPlaying
                            if (isPlaying) startProgressTracking()
                            else stopProgressTracking()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e("PlayerViewModel", "ExoPlayer Critical Error", error)
                            stop()
                        }
                    })
                }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Crash prevented during player init", e)
        }
    }

    private fun loadPlaylistContext(channel: Channel) {
        viewModelScope.launch {
            try {
                repository.getVodChannels(
                    playlistUrl = channel.playlistUrl,
                    type = channel.type,
                    group = channel.group
                ).collect { channels ->
                    playlistChannels = channels
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to load playlist context", e)
            }
        }
    }

    fun nextChannel() {
        val current = _currentChannel.value ?: return
        if (playlistChannels.isEmpty()) return

        val index = playlistChannels.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val nextIndex = if (index + 1 < playlistChannels.size) index + 1 else 0
            switchChannel(playlistChannels[nextIndex])
        }
    }

    fun prevChannel() {
        val current = _currentChannel.value ?: return
        if (playlistChannels.isEmpty()) return

        val index = playlistChannels.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val prevIndex = if (index - 1 >= 0) index - 1 else playlistChannels.lastIndex
            switchChannel(playlistChannels[prevIndex])
        }
    }

    private fun switchChannel(channel: Channel) {
        _currentChannel.value = channel
        loadEpgForChannel(channel)

        player?.apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            play()
        }
    }

    fun toggleFavorite() {
        val current = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            _currentChannel.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    private fun loadEpgForChannel(channel: Channel) {
        viewModelScope.launch {
            val epgId = channel.relationId ?: channel.title
            val program = repository.getCurrentProgram(epgId)
            _currentProgram.value = program
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
        _resizeMode.value = (_resizeMode.value + 1) % 3
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
        val c = _currentChannel.value ?: return
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