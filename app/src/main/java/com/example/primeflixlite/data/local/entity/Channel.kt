// file: app/src/main/java/com/example/primeflixlite/data/local/entity/Channel.kt
package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlist_url", "group"]), // For category browsing
        Index(value = ["canonical_title"]), // For grouping duplicates
        Index(value = ["stream_id"])
    ]
)
data class Channel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_url")
    val playlistUrl: String,

    @ColumnInfo(name = "stream_id")
    val streamId: String,

    @ColumnInfo(name = "title")
    val title: String, // The raw original title

    // --- NEW FIELDS FOR SMART GROUPING ---
    @ColumnInfo(name = "canonical_title")
    val canonicalTitle: String? = null, // "Avengers Endgame" (Normalized)

    @ColumnInfo(name = "quality")
    val quality: String = "SD", // "4K", "1080p", "SD"

    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int? = null, // Link to rich metadata
    // -------------------------------------

    @ColumnInfo(name = "group")
    val group: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "cover")
    val cover: String?,

    @ColumnInfo(name = "type")
    val type: String, // StreamType.name (LIVE, MOVIE, SERIES)

    @ColumnInfo(name = "relation_id")
    val relationId: String? = null, // EPG ID

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)