package com.example.primeflixlite.data.repository

import android.util.Log
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.data.parser.xtream.XtreamLive
import com.example.primeflixlite.data.parser.xtream.XtreamParser
import com.example.primeflixlite.data.parser.xtream.XtreamSerial
import com.example.primeflixlite.data.parser.xtream.XtreamVod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PrimeFlixRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val xtreamParser: XtreamParser
) {

    // Observe all playlists to show on the dashboard
    val playlists: Flow<List<Playlist>> = playlistDao.observeAll()

    // Observe channels for a specific playlist (e.g., "Live TV")
    fun observeChannels(playlistUrl: String): Flow<List<Channel>> {
        return channelDao.observeAllByPlaylistUrl(playlistUrl)
    }

    // The "Heavy Lifting" - Downloads and saves the playlist
    suspend fun syncXtreamPlaylist(title: String, input: XtreamInput) {
        val playlistUrl = XtreamInput.encodeToPlaylistUrl(input)

        // 1. Create or Update the Playlist entry
        val playlist = Playlist(
            title = title,
            url = playlistUrl,
            source = DataSource.Xtream
        )
        playlistDao.insertOrReplace(playlist)

        // 2. Download the streams
        try {
            xtreamParser.parse(input)
                .collect { data ->
                    // Convert API data to our Database Entity
                    val channel = when (data) {
                        is XtreamLive -> Channel(
                            url = XtreamParser.createActionUrl(
                                input.basicUrl, input.username, input.password,
                                XtreamParser.Action.GET_LIVE_STREAMS,
                                "stream_id" to data.streamId
                            ).replace("&action=get_live_streams", "/${data.streamId}.ts"), // Direct stream URL construction
                            category = data.categoryId ?: "Uncategorized",
                            title = data.name,
                            cover = data.streamIcon,
                            playlistUrl = playlistUrl
                        )
                        is XtreamVod -> Channel(
                            url = XtreamParser.createActionUrl(
                                input.basicUrl, input.username, input.password,
                                XtreamParser.Action.GET_VOD_STREAMS,
                                "stream_id" to data.streamId
                            ).replace("&action=get_vod_streams", "/movie/${input.username}/${input.password}/${data.streamId}.${data.containerExtension}"),
                            category = data.categoryId ?: "Movies",
                            title = data.name,
                            cover = data.streamIcon,
                            playlistUrl = playlistUrl
                        )
                        is XtreamSerial -> Channel(
                            url = "SERIES:${data.seriesId}", // Series are special, we handle them later
                            category = data.categoryId ?: "Series",
                            title = data.name,
                            cover = data.cover,
                            playlistUrl = playlistUrl
                        )
                        else -> null
                    }

                    if (channel != null) {
                        channelDao.insertOrReplace(channel)
                    }
                }
        } catch (e: Exception) {
            Log.e("Repository", "Error syncing playlist", e)
        }
    }
}