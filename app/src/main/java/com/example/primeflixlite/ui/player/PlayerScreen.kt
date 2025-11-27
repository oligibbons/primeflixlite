package com.example.primeflixlite.ui.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.primeflixlite.data.local.entity.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    initialChannel: Channel,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentProgram by viewModel.currentProgram.collectAsState()
    val isVisible by viewModel.overlayVisible.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()

    var isBuffering by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initialize(initialChannel)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(currentChannel) {
        currentChannel?.let { channel ->
            errorMsg = null
            isBuffering = true
            try {
                exoPlayer.stop()
                val mediaItem = MediaItem.fromUri(channel.url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                errorMsg = "Playback Error: ${e.message}"
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) errorMsg = null
            }
            override fun onPlayerError(error: PlaybackException) {
                errorMsg = "Stream Error: ${error.errorCodeName}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            if (exoPlayer.duration > 0 && exoPlayer.currentPosition > 0) {
                viewModel.onPause(exoPlayer.currentPosition, exoPlayer.duration)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    viewModel.showOverlay()
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> { viewModel.nextChannel(); true }
                        KeyEvent.KEYCODE_DPAD_DOWN -> { viewModel.prevChannel(); true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { viewModel.toggleResizeMode(); true }
                        KeyEvent.KEYCODE_BACK -> { onBack(); true }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // Fixed: Explicit usage of setter/property in factory
                    this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                // Fixed: Explicit setter usage to avoid 'val' error
                view.resizeMode = when(resizeMode) {
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        PlayerOverlay(
            channel = currentChannel,
            program = currentProgram,
            isVisible = isVisible,
            isBuffering = isBuffering,
            error = errorMsg,
            videoAspectRatio = when(resizeMode) { 1 -> "ZOOM" 2 -> "STRETCH" else -> "FIT" }
        )
    }
}