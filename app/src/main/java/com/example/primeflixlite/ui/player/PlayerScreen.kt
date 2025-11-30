package com.example.primeflixlite.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    initialChannel: Channel,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Initialize Player Safely via ViewModel (runs once)
    DisposableEffect(initialChannel) {
        viewModel.initialize(context, initialChannel)
        onDispose {
            viewModel.releasePlayer()
        }
    }

    // Observe State
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentProgram by viewModel.currentProgram.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()

    // Local UI State for Seekbar
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Polling Loop for UI Updates
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.player?.let { p ->
                if (p.isPlaying) {
                    currentPosition = p.currentPosition
                    duration = p.duration
                }
            }
            delay(1000)
        }
    }

    BackHandler {
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                }
            },
            update = { playerView ->
                // Link view to ViewModel's player
                playerView.player = viewModel.player

                playerView.resizeMode = when (resizeMode) {
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        if (currentChannel != null) {
            PlayerOverlay(
                channel = currentChannel!!,
                program = currentProgram,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = viewModel::playPause,
                onSeek = { pos ->
                    viewModel.seek(pos)
                    currentPosition = pos
                },
                onNext = viewModel::nextChannel,
                onPrev = viewModel::prevChannel,
                onResize = viewModel::toggleResizeMode,
                onFavorite = viewModel::toggleFavorite,
                onBack = onBack
            )
        }
    }
}