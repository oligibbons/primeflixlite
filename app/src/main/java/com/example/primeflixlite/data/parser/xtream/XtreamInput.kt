package com.example.primeflixlite.data.parser.xtream

import android.net.Uri
import com.example.primeflixlite.data.local.entity.DataSource

/**
 * Helper to parse and hold Xtream Codes credentials derived from a URL.
 */
data class XtreamInput(
    val basicUrl: String,
    val username: String,
    val password: String,
    val type: String = DataSource.Xtream.TYPE_LIVE
) {
    companion object {
        fun decodeFromPlaylistUrl(url: String): XtreamInput {
            val uri = Uri.parse(url)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: ""
            val port = uri.port

            // Reconstruct the base host URL (e.g., http://server.com:8080)
            val portStr = if (port != -1) ":$port" else ""
            val basicUrl = "$scheme://$host$portStr"

            val username = uri.getQueryParameter("username").orEmpty()
            val password = uri.getQueryParameter("password").orEmpty()

            // simple heuristic to determine default type if an action is present
            val action = uri.getQueryParameter("action")
            val type = when(action) {
                "get_series" -> DataSource.Xtream.TYPE_SERIES
                "get_vod_streams" -> DataSource.Xtream.TYPE_VOD
                else -> DataSource.Xtream.TYPE_LIVE
            }

            return XtreamInput(
                basicUrl = basicUrl,
                username = username,
                password = password,
                type = type
            )
        }
    }
}