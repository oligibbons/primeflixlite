package com.example.primeflixlite.data.local.entity

import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.startsWithAny

// --- Playlist Extensions ---

val Playlist.dataSource: DataSource
    get() = DataSource.of(source)

// Logic moved from Entity
val Playlist.isSeries: Boolean
    get() = source == DataSource.Xtream.value && type == DataSource.Xtream.TYPE_SERIES

val Playlist.isVod: Boolean
    get() = source == DataSource.Xtream.value && type == DataSource.Xtream.TYPE_VOD

val Playlist.refreshable: Boolean
    get() = source == DataSource.M3U.value && url != Playlist.URL_IMPORTED && !url.startsWithAny(
        "file://", "content://", ignoreCase = true
    )

val Playlist.type: String?
    get() = when (source) {
        DataSource.Xtream.value -> XtreamInput.decodeFromPlaylistUrl(url).type
        else -> null
    }

fun Playlist.epgUrlsOrXtreamXmlUrl(): List<String> = when (source) {
    DataSource.Xtream.value -> {
        when (type) {
            DataSource.Xtream.TYPE_LIVE -> {
                val input = XtreamInput.decodeFromPlaylistUrl(url)
                val epgUrl = "${input.basicUrl}/xmltv.php?username=${input.username}&password=${input.password}"
                listOf(epgUrl)
            }
            else -> emptyList()
        }
    }
    else -> epgUrls
}


// --- Channel Extensions ---

val Channel.category: String
    get() = group

val Channel.streamType: StreamType
    get() = try {
        StreamType.valueOf(type)
    } catch (e: Exception) {
        StreamType.LIVE
    }