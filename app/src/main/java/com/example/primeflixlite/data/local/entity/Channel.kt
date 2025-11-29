package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streams",
    indices = [
        Index(value = ["playlist_url"]),
        Index(value = ["type"]),
        Index(value = ["is_favorite"]) // Optimization for Favorites screen
    ]
)
data class Channel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "group")
    val group: String = "Uncategorized",

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "cover")
    val cover: String? = null,

    @ColumnInfo(name = "type")
    val type: String = "LIVE", // Default string value

    @ColumnInfo(name = "relation_id")
    val relationId: String? = null,

    @ColumnInfo(name = "stream_id")
    val streamId: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
) {
    val category: String get() = group

    val streamType: StreamType
        @Ignore get() = try {
            StreamType.valueOf(type)
        } catch (e: Exception) {
            StreamType.LIVE
        }
}