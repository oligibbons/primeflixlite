package com.example.primeflixlite.data.local.entity

import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.startsWithAny

val Playlist.isSeries: Boolean get() = source == DataSource.Xtream && type == DataSource.Xtream.TYPE_SERIES
val Playlist.isVod: Boolean get() = source == DataSource.Xtream && type == DataSource.Xtream.TYPE_VOD

val Playlist.refreshable: Boolean
    get() = source == DataSource.M3U && url != Playlist.URL_IMPORTED && !url.startsWithAny(
        "file://", "content://", ignoreCase = true
    )

val Playlist.type: String?
    get() = when (source) {
        DataSource.Xtream -> XtreamInput.decodeFromPlaylistUrl(url).type
        else -> null
    }

fun Playlist.epgUrlsOrXtreamXmlUrl(): List<String> = when (source) {
    DataSource.Xtream -> {
        when (type) {
            DataSource.Xtream.TYPE_LIVE -> {
                val input = XtreamInput.decodeFromPlaylistUrl(url)
                // Manually construct the XMLTV URL to avoid circular dependencies or missing parsers
                val epgUrl = "${input.basicUrl}/xmltv.php?username=${input.username}&password=${input.password}"
                listOf(epgUrl)
            }
            else -> emptyList()
        }
    }
    else -> epgUrls
}