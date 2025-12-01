package com.example.primeflixlite.ui.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.primeflixlite.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(url) {
        viewModel.loadContextForUrl(url)
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        exoPlayer.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (uiState.isPlaying) exoPlayer.play()
    }

    var isOsdVisible by remember { mutableStateOf(false) }
    var isChannelListVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun showControls() {
        hideJob?.cancel()
        isOsdVisible = true
        hideJob = coroutineScope.launch {
            delay(4000)
            isOsdVisible = false
            isChannelListVisible = false
        }
    }

    fun toggleChannelList() {
        hideJob?.cancel()
        isChannelListVisible = !isChannelListVisible
        isOsdVisible = false
        if (isChannelListVisible) {
            hideJob = coroutineScope.launch {
                delay(8000)
                isChannelListVisible = false
            }
        }
    }

    BackHandler {
        if (isOsdVisible || isChannelListVisible) {
            isOsdVisible = false
            isChannelListVisible = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    showControls()
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isChannelListVisible) isOsdVisible = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!isOsdVisible && !isChannelListVisible) {
                                toggleChannelList()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isChannelListVisible) {
                                toggleChannelList()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        var currentTime by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isPlaying by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            while(true) {
                currentTime = exoPlayer.currentPosition
                duration = exoPlayer.duration
                isPlaying = exoPlayer.isPlaying
                delay(1000)
            }
        }

        PlayerOverlay(
            isVisible = isOsdVisible || isChannelListVisible,
            isPlaying = isPlaying,
            title = uiState.currentChannel?.title ?: "Loading...",
            currentTime = currentTime,
            duration = if (duration < 0) 0 else duration,
            onPlayPause = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                showControls()
            },
            onSeek = { fraction ->
                if (duration > 0) exoPlayer.seekTo((duration * fraction).toLong())
                showControls()
            },
            onBack = onBack,
            isChannelListVisible = isChannelListVisible,
            channelList = uiState.playlistContext,
            onChannelClick = { channel ->
                val newMediaItem = MediaItem.fromUri(channel.url)
                exoPlayer.setMediaItem(newMediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                viewModel.updateCurrentChannel(channel)
                showControls()
                isOsdVisible = false
                isChannelListVisible = true
            }
        )
    }
}