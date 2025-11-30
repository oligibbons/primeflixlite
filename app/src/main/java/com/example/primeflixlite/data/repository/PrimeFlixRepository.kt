package com.example.primeflixlite.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.primeflixlite.data.local.PrimeFlixDatabase
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
import com.example.primeflixlite.util.FeedbackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrimeFlixRepository @Inject constructor(
    private val database: PrimeFlixDatabase,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val programmeDao: ProgrammeDao,
    private val watchProgressDao: WatchProgressDao,
    private val xtreamParser: XtreamParser,
    private val m3uParser: M3UParser,
    private val xmltvParser: XmltvParser,
    private val okHttpClient: OkHttpClient,
    private val feedbackManager: FeedbackManager,
    private val externalScope: CoroutineScope // Application Scope
) {

    val playlists = playlistDao.getAllPlaylists()

    // Now returns immediately, work happens in background
    fun addPlaylist(title: String, url: String, source: DataSource) {
        externalScope.launch {
            val playlist = Playlist(title = title, url = url, source = source.value)
            playlistDao.insert(playlist)
            doSyncWork(playlist)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.delete(playlist)
        channelDao.deleteByPlaylist(playlist.url)
        programmeDao.deleteByPlaylist(playlist.url)
    }

    // FIRE AND FORGET: Launches on App Scope
    fun syncPlaylist(playlist: Playlist) {
        externalScope.launch {
            doSyncWork(playlist)
        }
    }

    // INTERNAL WORKER
    private suspend fun doSyncWork(playlist: Playlist) = withContext(Dispatchers.IO) {
        try {
            feedbackManager.showLoading("Connecting...", "Initializing")

            val dataSource = DataSource.of(playlist.source)

            feedbackManager.showLoading("Preparing...", "Removing Old Data")
            database.withTransaction {
                channelDao.deleteByPlaylist(playlist.url)
            }

            var hasChannels = false

            if (dataSource == DataSource.Xtream) {
                hasChannels = syncXtreamStaged(playlist)
            } else if (dataSource == DataSource.M3U) {
                hasChannels = syncM3U(playlist)
            }

            if (hasChannels) {
                syncEpg(playlist, dataSource)
                feedbackManager.showSuccess("Playlist Synced Successfully!")
            } else {
                feedbackManager.showError("No channels found.")
            }

        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "Sync Error", e)
            feedbackManager.showError("Sync Failed: ${e.message}")
        }
    }

    private suspend fun syncXtreamStaged(playlist: Playlist): Boolean {
        val input = XtreamInput.decodeFromPlaylistUrl(playlist.url)
        var totalSaved = 0

        try {
            feedbackManager.showLoading("Downloading...", "Live Channels")
            val liveItems = xtreamParser.getLiveStreams(input)

            if (liveItems.isNotEmpty()) {
                val channels = liveItems.map {
                    Channel(
                        playlistUrl = playlist.url,
                        title = it.name.orEmpty(),
                        group = it.categoryId ?: "Uncategorized",
                        url = "${input.basicUrl}/live/${input.username}/${input.password}/${it.streamId}.ts",
                        cover = it.streamIcon,
                        type = StreamType.LIVE.name,
                        relationId = it.epgChannelId,
                        streamId = it.streamId.toString()
                    )
                }
                saveBatch(channels, "Live TV")
                totalSaved += channels.size
            }
        } catch (e: Exception) {
            Log.w("PrimeFlixRepo", "Xtream Live Failed", e)
        }

        try {
            feedbackManager.showLoading("Downloading...", "Movies")
            val vodItems = xtreamParser.getVodStreams(input)

            if (vodItems.isNotEmpty()) {
                val channels = vodItems.map {
                    Channel(
                        playlistUrl = playlist.url,
                        title = it.name.orEmpty(),
                        group = "Movies",
                        url = "${input.basicUrl}/movie/${input.username}/${input.password}/${it.streamId}.${it.containerExtension}",
                        cover = it.streamIcon,
                        type = StreamType.MOVIE.name,
                        streamId = it.streamId.toString()
                    )
                }
                saveBatch(channels, "Movies")
                totalSaved += channels.size
            }
        } catch (e: Exception) {
            Log.w("PrimeFlixRepo", "Xtream VOD Failed", e)
        }

        try {
            feedbackManager.showLoading("Downloading...", "Series")
            val seriesItems = xtreamParser.getSeries(input)

            if (seriesItems.isNotEmpty()) {
                val channels = seriesItems.map {
                    Channel(
                        playlistUrl = playlist.url,
                        title = it.name.orEmpty(),
                        group = "Series",
                        url = "",
                        cover = it.cover,
                        type = StreamType.SERIES.name,
                        streamId = it.seriesId.toString()
                    )
                }
                saveBatch(channels, "Series")
                totalSaved += channels.size
            }
        } catch (e: Exception) {
            Log.w("PrimeFlixRepo", "Xtream Series Failed", e)
        }

        return totalSaved > 0
    }

    private suspend fun saveBatch(channels: List<Channel>, label: String) {
        if (channels.isEmpty()) return

        val batchSize = 1000
        val chunks = channels.chunked(batchSize)
        val total = channels.size
        var current = 0

        feedbackManager.showLoading("Saving...", label)

        database.withTransaction {
            chunks.forEach { batch ->
                channelDao.insertAll(batch)
                current += batch.size
                feedbackManager.updateCount("Saving $label...", "Database", current, total)
            }
        }
    }

    private suspend fun syncM3U(playlist: Playlist): Boolean {
        feedbackManager.showLoading("Downloading...", "M3U Playlist")
        try {
            val request = Request.Builder().url(playlist.url).build()
            val response = okHttpClient.newCall(request).execute()
            val inputStream = response.body?.byteStream() ?: return false

            val items = mutableListOf<Channel>()
            m3uParser.parse(inputStream).collect { m3u ->
                items.add(m3u.toChannel(playlist.url))
            }

            if (items.isNotEmpty()) {
                saveBatch(items, "Channels")
                return true
            }
        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "M3U Failed", e)
        }
        return false
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
                feedbackManager.showLoading("Updating Guide...", "EPG")
                programmeDao.deleteOldProgrammes(System.currentTimeMillis())

                xmltvParser.parse(epgUrl).collect { batch ->
                    val validBatch = batch.map { it.copy(playlistUrl = playlist.url) }
                    programmeDao.insertAll(validBatch)
                }
            }
        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "EPG Sync Failed", e)
        }
    }

    // --- Accessors ---
    fun getChannels(playlistUrl: String) = channelDao.getChannelsByPlaylist(playlistUrl)
    fun getChannelsWithEpg(playlistUrl: String) = channelDao.getChannelsWithEpg(playlistUrl, System.currentTimeMillis())
    fun searchChannels(query: String) = channelDao.searchChannels(query)
    val favorites = channelDao.getFavorites()

    suspend fun toggleFavorite(channel: Channel) = channelDao.setFavorite(channel.url, !channel.isFavorite)
    suspend fun getChannelById(id: Long) = channelDao.getChannelById(id)

    fun getContinueWatching(type: StreamType) = watchProgressDao.getContinueWatching(type.name)
    fun getRecentChannels() = watchProgressDao.getRecentChannels()

    suspend fun saveProgress(url: String, pos: Long, dur: Long) {
        if (pos < 5000) return
        watchProgressDao.saveProgress(WatchProgress(url, pos, dur, System.currentTimeMillis()))
    }

    suspend fun getCurrentProgram(id: String) = programmeDao.getCurrentProgram(id, System.currentTimeMillis())
    suspend fun getProgrammesForChannel(id: String, start: Long, end: Long) = programmeDao.getProgrammesForChannel(id, start, end)
    suspend fun getSeriesEpisodes(url: String, id: Int) = xtreamParser.getSeriesEpisodes(XtreamInput.decodeFromPlaylistUrl(url), id)

    fun getGroups(url: String, type: String) = channelDao.getGroups(url, type)
    fun getLiveChannels(url: String, group: String) = channelDao.getLiveChannels(url, group, System.currentTimeMillis())
    fun getVodChannels(url: String, type: String, group: String) = channelDao.getVodChannels(url, type, group)
}