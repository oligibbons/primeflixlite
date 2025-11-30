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

    // --- State ---
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<Programme?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    private val _resizeMode = MutableStateFlow(0) // 0: Fit, 1: Fill, 2: Zoom
    val resizeMode = _resizeMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying = _isPlaying.asStateFlow()

    // Player Instance (Managed here to prevent crashes)
    var player: ExoPlayer? = null
        private set

    // Internal State
    private var playlistChannels: List<Channel> = emptyList()
    private var progressJob: Job? = null

    // --- Initialization ---
    fun initialize(context: Context, channel: Channel) {
        // Prevent re-initialization if already playing the same channel
        if (player != null && _currentChannel.value?.url == channel.url) return

        _currentChannel.value = channel
        loadEpgForChannel(channel)
        loadPlaylistContext(channel)

        initializePlayerSafely(context, channel)
    }

    fun loadChannelById(id: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(id)
            if (channel != null) {
                _currentChannel.value = channel
                loadEpgForChannel(channel)
                loadPlaylistContext(channel)
                // Note: Actual ExoPlayer initialization still happens in the Composable's DisposableEffect
                // using the passed context, but this ensures we have the full data object.
            }
        }
    }

    private fun initializePlayerSafely(context: Context, channel: Channel) {
        releasePlayer() // Cleanup any old instance

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
                            // Crash Prevention: Don't let the app die, just stop playback
                            stop()
                        }
                    })
                }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Crash prevented during player init", e)
        }
    }

    // --- Zapping Logic (OPTIMIZED) ---
    private fun loadPlaylistContext(channel: Channel) {
        viewModelScope.launch {
            try {
                // MEMORY OPTIMIZATION:
                // Instead of loading ALL channels (which can be 20k+),
                // we only load channels in the CURRENT GROUP and TYPE.
                // This reduces the list size to usually < 200 items, safe for 1GB RAM.
                // We reuse 'getVodChannels' because it returns a clean List<Channel> without EPG joins.
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

        // Fast switch without recreating the whole player
        player?.apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            play()
        }
    }

    // --- Favorites Logic ---
    fun toggleFavorite() {
        val current = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            _currentChannel.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    // --- EPG Logic ---
    private fun loadEpgForChannel(channel: Channel) {
        viewModelScope.launch {
            val epgId = channel.relationId ?: channel.title
            val program = repository.getCurrentProgram(epgId)
            _currentProgram.value = program
        }
    }

    // --- Player Controls ---
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