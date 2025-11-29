package com.example.primeflixlite.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @ColumnInfo(name = "title")
    val title: String,

    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "pinned_groups", defaultValue = "[]")
    val pinnedCategories: List<String> = emptyList(),

    @ColumnInfo(name = "hidden_groups", defaultValue = "[]")
    val hiddenCategories: List<String> = emptyList(),

    @ColumnInfo(name = "source", defaultValue = "m3u")
    val source: String = "m3u",

    @ColumnInfo(name = "user_agent", defaultValue = "NULL")
    val userAgent: String? = null,

    @ColumnInfo(name = "epg_urls", defaultValue = "[]")
    val epgUrls: List<String> = emptyList(),

    @ColumnInfo(name = "auto_refresh_programmes", defaultValue = "0")
    val autoRefreshProgrammes: Boolean = false
) {
    companion object {
        const val URL_IMPORTED = "imported"
    }
}