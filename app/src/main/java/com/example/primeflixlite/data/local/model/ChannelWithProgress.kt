package com.example.primeflixlite.data.local.model

import androidx.room.Embedded
import com.example.primeflixlite.data.local.entity.Channel

data class ChannelWithProgress(
    @Embedded val channel: Channel,

    // Embedded fields from the JOIN query
    val prog_position: Long,
    val prog_duration: Long,
    val prog_lastPlayed: Long
) {
    fun getProgressFloat(): Float {
        if (prog_duration <= 0) return 0f
        return prog_position.toFloat() / prog_duration.toFloat()
    }
}