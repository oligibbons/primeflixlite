package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StreamType {
    LIVE,
    MOVIE,
    SERIES
}

// CRITICAL: Force table name to 'streams' to match your existing DAO queries
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

    // Core Data
    @ColumnInfo(name = "title")
    val title: String,

    // 'group' is a reserved SQL keyword, so we map it to a safe column name if needed,
    // but legacy DB uses "group", so we keep the field name 'group'.
    @ColumnInfo(name = "group")
    val group: String = "Uncategorized",

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "cover")
    val cover: String? = null,

    // New VOD fields
    @ColumnInfo(name = "type")
    val type: StreamType = StreamType.LIVE,

    @ColumnInfo(name = "relation_id")
    val relationId: String? = null, // EPG ID

    @ColumnInfo(name = "stream_id")
    val streamId: String? = null
) {
    // Helper for UI compatibility
    val category: String get() = group
}