package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.primeflixlite.Exclude
import com.example.primeflixlite.Likable
import kotlinx.serialization.Serializable

@Entity(tableName = "playlists")
@Immutable
@Serializable
@Likable
data class Playlist(
    @ColumnInfo(name = "title")
    val title: String,
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    // extra fields
    @ColumnInfo(name = "pinned_groups", defaultValue = "[]")
    @Exclude
    val pinnedCategories: List<String> = emptyList(),
    @ColumnInfo(name = "hidden_groups", defaultValue = "[]")
    @Exclude
    val hiddenCategories: List<String> = emptyList(),

    // FIX: Store as String to prevent KSP recursion
    @ColumnInfo(name = "source", defaultValue = "m3u")
    val source: String = DataSource.M3U.value,

    @ColumnInfo(name = "user_agent", defaultValue = "NULL")
    @Exclude
    val userAgent: String? = null,
    // epg playlist urls
    @ColumnInfo(name = "epg_urls", defaultValue = "[]")
    @Exclude
    val epgUrls: List<String> = emptyList(),
    @ColumnInfo(name = "auto_refresh_programmes", defaultValue = "0")
    @Exclude
    val autoRefreshProgrammes: Boolean = false
) {
    // Helper to get the typed object when needed in UI/Logic
    val dataSource: DataSource
        @Ignore get() = DataSource.of(source)

    companion object {
        const val URL_IMPORTED = "imported"
    }
}