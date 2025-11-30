package com.example.primeflixlite.data.local.model

import androidx.room.Embedded
import com.example.primeflixlite.data.local.entity.Channel

data class ChannelWithProgress(
    @Embedded
    val channel: Channel,

    @androidx.room.ColumnInfo(name = "progress_position")
    val position: Long,

    @androidx.room.ColumnInfo(name = "progress_duration")
    val duration: Long,

    @androidx.room.ColumnInfo(name = "progress_lastPlayed")
    val lastPlayed: Long
)