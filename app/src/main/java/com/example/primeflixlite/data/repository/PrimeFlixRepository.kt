package com.example.primeflixlite.data.repository

import android.util.Log
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.dao.ProgrammeDao
import com.example.primeflixlite.data.local.dao.WatchProgressDao
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.entity.WatchProgress
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.parser.m3u.M3UParser
import com.example.primeflixlite.data.parser.m3u.toChannel
import com.example.primeflixlite.data.parser.xmltv.XmltvParser
import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.data.parser.xtream.XtreamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrimeFlixRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val programmeDao: ProgrammeDao,
    private val watchProgressDao: WatchProgressDao,
    private val xtreamParser: XtreamParser,
    private val m3uParser: M3UParser,
    private val xmltvParser: XmltvParser,
    private val okHttpClient: OkHttpClient
) {

    // --- PLAYLISTS & CHANNELS ---
    val playlists = playlistDao.getAllPlaylists()

    suspend fun addPlaylist(title: String, url: String, source: DataSource) {
        val playlist = Playlist(title = title, url = url, source = source.value)
        playlistDao.insert(playlist) // FIX: Correct DAO method name
        syncPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.delete(playlist) // FIX: Correct DAO method name and logic
        channelDao.deleteByPlaylist(playlist.url)
        programmeDao.deleteByPlaylist(playlist.url)
    }

    suspend fun syncPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        try {
            val dataSource = DataSource.of(playlist.source)

            val channels = when (dataSource) {
                DataSource.Xtream -> fetchXtreamData(playlist)
                DataSource.M3U -> fetchM3UData(playlist)
                else -> emptyList()
            }

            if (channels.isNotEmpty()) {
                channelDao.replacePlaylistChannels(playlist.url, channels)
                Log.d("PrimeFlixRepo", "Synced ${channels.size} items for ${playlist.title}")

                // FIX: Compare against String name
                if (channels.any { it.type == StreamType.LIVE.name }) {
                    syncEpg(playlist, dataSource)
                }
            }
        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "Error syncing playlist", e)
        }
    }

    fun getChannels(playlistUrl: String) = channelDao.getChannelsByPlaylist(playlistUrl)

    fun getChannelsWithEpg(playlistUrl: String): Flow<List<ChannelWithProgram>> {
        return channelDao.getChannelsWithEpg(playlistUrl, System.currentTimeMillis())
    }

    fun searchChannels(query: String) = channelDao.searchChannels(query)

    // --- FAVORITES ---
    val favorites: Flow<List<Channel>> = channelDao.getFavorites()

    suspend fun toggleFavorite(channel: Channel) {
        channelDao.setFavorite(channel.url, !channel.isFavorite)
    }

    // --- WATCH HISTORY ---
    fun getContinueWatching(type: StreamType) = watchProgressDao.getContinueWatching(type.name)

    fun getRecentChannels() = watchProgressDao.getRecentChannels()

    suspend fun saveProgress(url: String, position: Long, duration: Long) {
        if (position < 5000) return
        val progress = WatchProgress(
            channelUrl = url,
            position = position,
            duration = duration,
            lastPlayed = System.currentTimeMillis()
        )
        watchProgressDao.saveProgress(progress)
    }

    // --- EPG & METADATA ---
    suspend fun getCurrentProgram(channelId: String) =
        programmeDao.getCurrentProgram(channelId, System.currentTimeMillis())

    suspend fun getProgrammesForChannel(channelId: String, start: Long, end: Long) =
        programmeDao.getProgrammesForChannel(channelId, start, end)

    suspend fun getSeriesEpisodes(playlistUrl: String, seriesId: Int) =
        xtreamParser.getSeriesEpisodes(XtreamInput.decodeFromPlaylistUrl(playlistUrl), seriesId)

    // --- PRIVATE FETCHERS ---
    private suspend fun fetchXtreamData(playlist: Playlist): List<Channel> {
        val input = XtreamInput.decodeFromPlaylistUrl(playlist.url)
        val items = mutableListOf<Channel>()

        try {
            val live = xtreamParser.getLiveStreams(input)
            items.addAll(live.map {
                Channel(
                    playlistUrl = playlist.url,
                    title = it.name.orEmpty(),
                    group = it.categoryId ?: "Uncategorized",
                    url = "${input.basicUrl}/live/${input.username}/${input.password}/${it.streamId}.ts",
                    cover = it.streamIcon,
                    type = StreamType.LIVE.name, // FIX: Use .name
                    relationId = it.epgChannelId,
                    streamId = it.streamId.toString()
                )
            })
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream Live Failed", e) }

        try {
            val vods = xtreamParser.getVodStreams(input)
            items.addAll(vods.map {
                Channel(
                    playlistUrl = playlist.url,
                    title = it.name.orEmpty(),
                    group = "Movies",
                    url = "${input.basicUrl}/movie/${input.username}/${input.password}/${it.streamId}.${it.containerExtension}",
                    cover = it.streamIcon,
                    type = StreamType.MOVIE.name, // FIX: Use .name
                    streamId = it.streamId.toString()
                )
            })
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream VOD Failed", e) }

        try {
            val series = xtreamParser.getSeries(input)
            items.addAll(series.map {
                Channel(
                    playlistUrl = playlist.url,
                    title = it.name.orEmpty(),
                    group = "Series",
                    url = "",
                    cover = it.cover,
                    type = StreamType.SERIES.name, // FIX: Use .name
                    streamId = it.seriesId.toString()
                )
            })
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream Series Failed", e) }

        return items
    }

    private suspend fun fetchM3UData(playlist: Playlist): List<Channel> {
        val request = Request.Builder().url(playlist.url).build()
        val response = okHttpClient.newCall(request).execute()
        val inputStream = response.body?.byteStream() ?: return emptyList()

        val items = mutableListOf<Channel>()
        m3uParser.parse(inputStream).collect { m3u ->
            items.add(m3u.toChannel(playlist.url))
        }
        return items
    }

    private suspend fun syncEpg(playlist: Playlist, dataSource: DataSource) {
        try {
            val epgUrl = when(dataSource) {
                DataSource.Xtream -> {
                    val input = XtreamInput.decodeFromPlaylistUrl(playlist.url)
                    "${input.basicUrl}/xmltv.php?username=${input.username}&password=${input.password}"
                }
                DataSource.M3U -> {
                    if (playlist.url.contains("SamsungTVPlus")) "https://i.mjh.nz/SamsungTVPlus/us.xml" else null
                }
                else -> null
            }

            if (epgUrl != null) {
                programmeDao.deleteOldProgrammes(System.currentTimeMillis())
                xmltvParser.parse(epgUrl).collect { batch ->
                    val validBatch = batch.map { it.copy(playlistUrl = playlist.url) }
                    programmeDao.insertAll(validBatch)
                }
            }
        } catch (e: Exception) { Log.e("PrimeFlixRepo", "EPG Sync Failed", e) }
    }
}