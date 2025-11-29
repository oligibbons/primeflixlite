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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultAllocator
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

    LaunchedEffect(initialChannel) {
        viewModel.initialize(initialChannel)
    }

    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentProgram by viewModel.currentProgram.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()

    val player = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(currentChannel) {
        currentChannel?.let { channel ->
            val mediaItem = MediaItem.fromUri(channel.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            viewModel.startProgressTracking(player)
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration
            }
        }
        player.addListener(listener)
        while (isActive) {
            currentPosition = player.currentPosition
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveFinalProgress(player.currentPosition, player.duration)
            player.release()
        }
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = when(resizeMode) {
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (currentChannel != null) {
            PlayerOverlay(
                channel = currentChannel!!,
                program = currentProgram,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                onSeek = { player.seekTo(it); currentPosition = it },
                onNext = { viewModel.nextChannel() },
                onPrev = { viewModel.prevChannel() },
                onResize = { viewModel.toggleResizeMode() },
                // FIX: Passed the missing callback
                onFavorite = { viewModel.toggleFavorite() },
                onBack = onBack
            )
        }
    }
}