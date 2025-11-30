// file: app/src/main/java/com/example/primeflixlite/data/local/entity/MediaMetadata.kt
package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_metadata",
    indices = [
        Index(value = ["tmdb_id"], unique = true),
        Index(value = ["normalized_title_hash"]) // For fast lookups from IPTV titles
    ]
)
data class MediaMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,

    @ColumnInfo(name = "media_type")
    val mediaType: String, // "movie" or "tv"

    // The key used to link this metadata to the messy IPTV titles
    // Derived from TitleNormalizer.generateGroupKey()
    @ColumnInfo(name = "normalized_title_hash")
    val normalizedTitleHash: String,

    @ColumnInfo(name = "canonical_title")
    val title: String,

    @ColumnInfo(name = "original_title")
    val originalTitle: String?,

    @ColumnInfo(name = "overview")
    val overview: String?,

    @ColumnInfo(name = "poster_path")
    val posterPath: String?,

    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String?,

    @ColumnInfo(name = "vote_average")
    val voteAverage: Double = 0.0,

    @ColumnInfo(name = "release_date")
    val releaseDate: String?, // YYYY-MM-DD

    @ColumnInfo(name = "genres")
    val genres: String, // Comma separated IDs or Names

    @ColumnInfo(name = "cast_preview")
    val castPreview: String?, // JSON string of top 3-5 actors for UI preview

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
