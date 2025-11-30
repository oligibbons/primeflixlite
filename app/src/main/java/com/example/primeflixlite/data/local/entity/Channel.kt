package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streams", // Fixed table name
    indices = [
        Index(value = ["playlist_url", "group"]),
        Index(value = ["canonical_title"]),
        Index(value = ["stream_id"])
    ]
)
data class Channel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String,

    @ColumnInfo(name = "stream_id")
    val streamId: String = "", // Added Default

    @ColumnInfo(name = "title")
    val title: String,

    // --- NEW FIELDS (With Defaults to fix build errors) ---
    @ColumnInfo(name = "canonical_title")
    val canonicalTitle: String? = null,

    @ColumnInfo(name = "quality")
    val quality: String = "SD",

    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int? = null,
    // -----------------------------------------------------

    @ColumnInfo(name = "group")
    val group: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "cover")
    val cover: String? = null, // Added Default

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "relation_id")
    val relationId: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)