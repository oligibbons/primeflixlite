package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.primeflixlite.Exclude
import com.example.primeflixlite.Likable
import kotlinx.serialization.Serializable

@Entity(tableName = "programmes")
@Immutable
@Serializable
@Likable
data class Programme(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Exclude
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
    val channelId: String, // References relationId in Channel
    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String
)