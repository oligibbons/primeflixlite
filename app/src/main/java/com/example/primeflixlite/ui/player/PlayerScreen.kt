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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

    // --- PLAYER STATE ---
    // We hold the ExoPlayer instance here to tie it to the Composable lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Initialize / Update Player Media
    LaunchedEffect(url) {
        // Load the channel context (neighbors) for the drawer
        viewModel.loadContextForUrl(url)

        // Prepare the stream
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Cleanup on Exit
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Pause/Resume on lifecycle changes (Backgrounding the app)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        exoPlayer.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (uiState.isPlaying) exoPlayer.play()
    }

    // --- OSD VISIBILITY LOGIC ---
    var isOsdVisible by remember { mutableStateOf(false) }
    var isChannelListVisible by remember { mutableStateOf(false) }

    // Auto-hide timer
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun showControls() {
        hideJob?.cancel()
        isOsdVisible = true
        hideJob = coroutineScope.launch {
            delay(4000) // 4 seconds of idle time
            isOsdVisible = false
            isChannelListVisible = false
        }
    }

    fun toggleChannelList() {
        hideJob?.cancel()
        isChannelListVisible = !isChannelListVisible
        // If opening list, hide standard OSD; if closing, hide everything
        isOsdVisible = false
        if (isChannelListVisible) {
            // Keep it open longer if interacting with list
            hideJob = coroutineScope.launch {
                delay(8000)
                isChannelListVisible = false
            }
        }
    }

    // Handle Back Press
    BackHandler {
        if (isOsdVisible || isChannelListVisible) {
            isOsdVisible = false
            isChannelListVisible = false
        } else {
            onBack()
        }
    }

    // --- UI COMPOSITION ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            // KEY EVENT INTERCEPTOR
            // This is the "Remote Control" logic
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    showControls() // Reset timer on any key
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isChannelListVisible) isOsdVisible = true
                            true // Handle it
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
                                toggleChannelList() // Close drawer
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
            .focusable() // Important: Make the box focusable to catch keys
    ) {
        // 1. VIDEO SURFACE
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // We use our own overlay
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. OVERLAY UI (Compose)
        // We defer state updates to a LaunchedEffect loop to update UI progress
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
            // Channel List Props
            isChannelListVisible = isChannelListVisible,
            channelList = uiState.playlistContext,
            onChannelClick = { channel ->
                // Switch Channel Logic
                val newMediaItem = MediaItem.fromUri(channel.url)
                exoPlayer.setMediaItem(newMediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                viewModel.updateCurrentChannel(channel)
                // Keep drawer open but reset timer
                showControls()
                // Close standard OSD if it popped up
                isOsdVisible = false
                isChannelListVisible = true
            }
        )
    }
}