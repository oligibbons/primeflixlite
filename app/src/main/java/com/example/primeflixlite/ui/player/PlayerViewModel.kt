package com.example.primeflixlite.ui.player

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: PrimeFlixRepository
) : AndroidViewModel(application) {

    // Retrieve the URL passed via Navigation arguments
    private val videoUrl: String = savedStateHandle.get<String>("videoUrl") ?: ""

    private var _player: ExoPlayer? = null
    val player: ExoPlayer? get() = _player

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    // Using your full repo, we can fetch the channel context
    private var currentChannel: Channel? = null

    data class PlayerUiState(
        val isLoading: Boolean = true,
        val isBuffering: Boolean = false,
        val isError: Boolean = false,
        val errorMessage: String? = null,
        val isControlsVisible: Boolean = false,
        val videoTitle: String = ""
    )

    init {
        initializePlayer()
        fetchChannelInfo()
    }

    private fun fetchChannelInfo() {
        viewModelScope.launch {
            if (videoUrl.isNotBlank()) {
                currentChannel = repository.getChannelByUrl(videoUrl)
                currentChannel?.let {
                    _uiState.value = _uiState.value.copy(videoTitle = it.title)

                    // Restore progress if VOD
                    if (it.type != "LIVE") {
                        // Logic to seek to resume position could go here
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        if (videoUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(isError = true, errorMessage = "Invalid Stream URL")
            return
        }

        try {
            // Track Selector for adaptive streaming on low-end devices
            val trackSelector = DefaultTrackSelector(getApplication())
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setMaxVideoSizeSd() // Prefer SD on 1GB RAM if available to prevent OOM/Stutter
                    .setForceLowestBitrate(false)
            )

            _player = ExoPlayer.Builder(getApplication())
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    playWhenReady = true
                    addListener(playerListener)
                }

            val mediaSource = buildMediaSource(Uri.parse(videoUrl))
            _player?.setMediaSource(mediaSource)
            _player?.prepare()

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isError = true, errorMessage = e.localizedMessage)
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaSource(uri: Uri): MediaSource {
        val userAgent = "PrimeFlixLite/1.0 (Linux;Android 11) ExoPlayerLib/2.19.1"
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)

        val type = androidx.media3.common.util.Util.inferContentType(uri)
        return if (type == C.CONTENT_TYPE_HLS) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.value = _uiState.value.copy(isBuffering = true, isLoading = false)
                }
                Player.STATE_READY -> {
                    _uiState.value = _uiState.value.copy(isBuffering = false, isLoading = false, isError = false)
                }
                Player.STATE_ENDED -> {
                    // Handle end of content
                }
                Player.STATE_IDLE -> { }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.value = _uiState.value.copy(
                isError = true,
                errorMessage = "Stream Error: ${error.errorCodeName}",
                isLoading = false
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressTracking()
            }
        }
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while(_player?.isPlaying == true) {
                val currentPos = _player?.currentPosition ?: 0L
                val duration = _player?.duration ?: 0L
                if (duration > 0 && videoUrl.isNotBlank()) {
                    repository.saveProgress(videoUrl, currentPos, duration)
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }

    fun showControls() {
        _uiState.value = _uiState.value.copy(isControlsVisible = true)
        // Auto-hide after 5 seconds
        viewModelScope.launch {
            delay(5000)
            _uiState.value = _uiState.value.copy(isControlsVisible = false)
        }
    }

    fun togglePlayPause() {
        _player?.let {
            if (it.isPlaying) it.pause() else it.play()
            showControls()
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    fun releasePlayer() {
        _player?.removeListener(playerListener)
        _player?.release()
        _player = null
    }
}