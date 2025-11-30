package com.example.primeflixlite.data.parser.m3u

import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.util.TitleNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

class M3UParserImpl @Inject constructor() : M3UParser {

    override fun parse(inputStream: InputStream): Flow<M3UData> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var currentData: M3UData? = null

        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: continue

            if (l.startsWith("#EXTINF:")) {
                // Simple parsing logic
                val title = l.substringAfterLast(",").trim()
                val group = if (l.contains("group-title=\"")) {
                    l.substringAfter("group-title=\"").substringBefore("\"")
                } else "Uncategorized"
                val logo = if (l.contains("tvg-logo=\"")) {
                    l.substringAfter("tvg-logo=\"").substringBefore("\"")
                } else null
                val id = if (l.contains("tvg-id=\"")) {
                    l.substringAfter("tvg-id=\"").substringBefore("\"")
                } else null

                currentData = M3UData(title, group, logo, "", id)
            } else if (!l.startsWith("#") && l.isNotEmpty() && currentData != null) {
                emit(currentData.copy(url = l))
                currentData = null
            }
        }
    }.flowOn(Dispatchers.IO)
}

// Extension to map M3UData to Channel
fun M3UData.toChannel(playlistUrl: String): Channel {
    // Generate a pseudo-ID for M3U items because M3U doesn't ensure unique IDs
    val generatedId = this.url.hashCode().toString()

    // Use Normalizer to fill new metadata fields (Canonical Title, Quality)
    val info = TitleNormalizer.parse(this.title)

    // Guess type based on URL extension or path
    val type = if (this.url.endsWith(".m3u8") || this.url.contains("/live/")) {
        StreamType.LIVE
    } else if (this.url.endsWith(".mp4") || this.url.endsWith(".mkv")) {
        StreamType.MOVIE
    } else {
        StreamType.LIVE
    }

    return Channel(
        playlistUrl = playlistUrl,
        streamId = generatedId, // REQUIRED: Fixes "No value passed" error
        title = this.title,
        canonicalTitle = info.normalizedTitle, // NEW: For deduplication
        quality = info.quality, // NEW: For version selection
        group = this.group,
        url = this.url,
        cover = this.logo,
        type = type.name,
        relationId = this.tvgId
    )
}