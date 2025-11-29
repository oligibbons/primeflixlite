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
        Index(value = ["type"])
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

    // FIX: Store as String to prevent KSP recursion
    @ColumnInfo(name = "type")
    val type: String = StreamType.LIVE.name,

    @ColumnInfo(name = "relation_id")
    val relationId: String? = null,

    @ColumnInfo(name = "stream_id")
    val streamId: String? = null
) {
    val category: String get() = group

    // Helper to get the Enum when needed
    val streamType: StreamType
        @Ignore get() = try {
            StreamType.valueOf(type)
        } catch (e: Exception) {
            StreamType.LIVE
        }
}