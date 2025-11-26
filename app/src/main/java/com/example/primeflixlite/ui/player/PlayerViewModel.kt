package com.example.primeflixlite.ui.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer().also { _player = it }

    private fun createPlayer(): ExoPlayer {
        val context = getApplication<Application>()

        // Use DefaultHttpDataSource to support standard protocols
        // On low-end devices, we stick to defaults unless buffering issues occur
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                playWhenReady = true
            }
    }

    @OptIn(UnstableApi::class)
    fun play(url: String) {
        val exoPlayer = player
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    fun releasePlayer() {
        _player?.release()
        _player = null
    }
}