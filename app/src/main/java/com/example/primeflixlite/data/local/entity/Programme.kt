package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "programmes")
@Immutable
@Serializable
data class Programme(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "start")
    val start: Long,

    @ColumnInfo(name = "end")
    val end: Long,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "channel_id")
    val channelId: String,

    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String
)