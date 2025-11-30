package com.example.primeflixlite.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.primeflixlite.data.local.entity.Channel

data class ChannelWithProgress(
    @Embedded
    val channel: Channel,

    // FIX: These annotations are required to match the "AS prog_..." aliases in your WatchProgressDao
    @ColumnInfo(name = "prog_position")
    val position: Long,

    @ColumnInfo(name = "prog_duration")
    val duration: Long,

    @ColumnInfo(name = "prog_lastPlayed")
    val lastPlayed: Long
)