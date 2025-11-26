package com.example.primeflixlite.data.parser.m3u

import android.net.Uri
import com.example.primeflixlite.data.local.entity.Channel

// Changed from 'internal' to 'public' (default) to fix visibility error
data class M3UData(
    val id: String = "",
    val name: String = "",
    val cover: String = "",
    val group: String = "",
    val title: String = "",
    val url: String = "",
    val duration: Double = -1.0,
    val licenseType: String? = null,
    val licenseKey: String? = null,
)

fun M3UData.toChannel(
    playlistUrl: String,
    seen: Long = 0L
): Channel {
    val fileScheme = "file:///"

    // Resolve absolute URL
    val absoluteUrl = if (!url.startsWith(fileScheme)) {
        url
    } else {
        val uri = Uri.parse(playlistUrl)
        val path = uri.path ?: ""
        val parentPath = path.substringBeforeLast('/', "")
        val filename = url.removePrefix(fileScheme)

        uri.buildUpon()
            .path(if (parentPath.isNotEmpty()) "$parentPath/$filename" else filename)
            .build()
            .toString()
    }

    val relationId = id.ifEmpty { name }

    return Channel(
        url = absoluteUrl,
        category = group,
        title = title,
        cover = cover,
        playlistUrl = playlistUrl,
        seen = seen,
        licenseType = licenseType,
        licenseKey = licenseKey,
        relationId = relationId
    )
}