package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "programmes",
    indices = [
        Index(value = ["channel_id"]),
        Index(value = ["start"]),
        Index(value = ["end"]),
        Index(value = ["playlist_url"])
    ]
)
data class Programme(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "channel_id")
    val channelId: String, // Maps to Channel.relationId

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "start")
    val start: Long, // Epoch Millis

    @ColumnInfo(name = "end")
    val end: Long,   // Epoch Millis

    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String
)