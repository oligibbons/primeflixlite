package com.example.primeflixlite.data.parser.m3u

import android.util.Log
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class M3UParserImpl : M3UParser {

    override fun parse(inputStream: InputStream): Flow<M3UData> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var currentData = M3UData(url = "")

        while (reader.readLine().also { line = it } != null) {
            val cleanLine = line?.trim() ?: continue

            if (cleanLine.startsWith("#EXTINF:")) {
                // Parse metadata
                val info = parseExtInf(cleanLine)
                currentData = info
            } else if (cleanLine.isNotEmpty() && !cleanLine.startsWith("#")) {
                // It's a URL
                if (currentData.name != null) {
                    emit(currentData.copy(url = cleanLine))
                }
                // Reset for next entry
                currentData = M3UData(url = "")
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseExtInf(line: String): M3UData {
        try {
            val parts = line.split(",", limit = 2)
            val attributes = parts.getOrElse(0) { "" }
            val name = parts.getOrElse(1) { "Unknown Channel" }

            val logo = extractAttribute(attributes, "tvg-logo")
            val id = extractAttribute(attributes, "tvg-id")
            val group = extractAttribute(attributes, "group-title")

            return M3UData(
                name = name,
                logo = logo,
                tvgId = id,
                group = group,
                url = ""
            )
        } catch (e: Exception) {
            Log.e("M3UParser", "Error parsing line: $line", e)
            return M3UData(url = "")
        }
    }

    private fun extractAttribute(line: String, key: String): String? {
        val pattern = "$key=\""
        val startIndex = line.indexOf(pattern)
        if (startIndex == -1) return null

        val endIndex = line.indexOf("\"", startIndex + pattern.length)
        if (endIndex == -1) return null

        return line.substring(startIndex + pattern.length, endIndex)
    }
}

// Extension function to map parser data to database entity
fun M3UData.toChannel(playlistUrl: String): Channel {
    val isVod = url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".avi")
    return Channel(
        playlistUrl = playlistUrl,
        title = name ?: "Unknown",
        group = group ?: "Uncategorized",
        url = url,
        cover = logo,
        // FIX: Convert Enum to String
        type = if (isVod) StreamType.MOVIE.name else StreamType.LIVE.name,
        relationId = tvgId
    )
}