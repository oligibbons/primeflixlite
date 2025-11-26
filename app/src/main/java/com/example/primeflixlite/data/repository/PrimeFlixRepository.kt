package com.example.primeflixlite.data.repository

import android.util.Log
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.parser.m3u.M3UParser
import com.example.primeflixlite.data.parser.m3u.toChannel
import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.data.parser.xtream.XtreamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// Removed @Inject for Manual DI
class PrimeFlixRepository(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val xtreamParser: XtreamParser,
    private val m3uParser: M3UParser,
    private val okHttpClient: OkHttpClient
) {

    val playlists = playlistDao.getAllPlaylists()

    fun getChannels(playlistUrl: String) = channelDao.getChannelsByPlaylist(playlistUrl)

    suspend fun addPlaylist(title: String, url: String, source: DataSource) {
        val playlist = Playlist(title = title, url = url, source = source)
        playlistDao.insert(playlist)
        syncPlaylist(playlist)
    }

    suspend fun syncPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        try {
            val channels = when (playlist.source) {
                DataSource.Xtream -> fetchXtreamChannels(playlist)
                DataSource.M3U -> fetchM3UChannels(playlist)
                else -> emptyList()
            }

            if (channels.isNotEmpty()) {
                channelDao.replacePlaylistChannels(playlist.url, channels)
                Log.d("PrimeFlixRepo", "Synced ${channels.size} channels for ${playlist.title}")
            }
        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "Error syncing playlist: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun fetchXtreamChannels(playlist: Playlist): List<Channel> {
        val input = XtreamInput.decodeFromPlaylistUrl(playlist.url)
        val channels = mutableListOf<Channel>()

        // 1. Live Streams
        try {
            val liveStreams = xtreamParser.getLiveStreams(input)
            channels.addAll(liveStreams.map {
                Channel(
                    url = "${input.basicUrl}/live/${input.username}/${input.password}/${it.streamId}.ts",
                    category = it.categoryId ?: "Uncategorized",
                    title = it.name.orEmpty(),
                    cover = it.streamIcon,
                    playlistUrl = playlist.url,
                    relationId = it.epgChannelId
                )
            })
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Failed to fetch Live: ${e.message}") }

        // 2. VOD Streams
        try {
            val vodStreams = xtreamParser.getVodStreams(input)
            channels.addAll(vodStreams.map {
                Channel(
                    url = "${input.basicUrl}/movie/${input.username}/${input.password}/${it.streamId}.${it.containerExtension}",
                    category = "VOD",
                    title = it.name.orEmpty(),
                    cover = it.streamIcon,
                    playlistUrl = playlist.url
                )
            })
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Failed to fetch VOD: ${e.message}") }

        return channels
    }

    private suspend fun fetchM3UChannels(playlist: Playlist): List<Channel> {
        return try {
            val request = Request.Builder().url(playlist.url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val inputStream = response.body?.byteStream() ?: return emptyList()

            // Parse the stream and map to Channel entities
            // Note: toList() is a terminal operator for Flow
            val m3uDataList = mutableListOf<com.example.primeflixlite.data.parser.m3u.M3UData>()
            m3uParser.parse(inputStream).collect {
                m3uDataList.add(it)
            }

            m3uDataList.map { it.toChannel(playlist.url) }
        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "M3U Parse Error", e)
            emptyList()
        }
    }
}