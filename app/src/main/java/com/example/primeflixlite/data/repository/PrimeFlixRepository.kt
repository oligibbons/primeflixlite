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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val feedbackManager: FeedbackManager
) {

    // --- PLAYLISTS ---
    val playlists = playlistDao.getAllPlaylists()

    suspend fun addPlaylist(title: String, url: String, source: DataSource) {
        val playlist = Playlist(title = title, url = url, source = source.value)
        playlistDao.insert(playlist)
        syncPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        playlistDao.delete(playlist)
        channelDao.deleteByPlaylist(playlist.url)
        programmeDao.deleteByPlaylist(playlist.url)
    }

    // --- SYNC ENGINE (STAGED) ---
    suspend fun syncPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        try {
            val dataSource = DataSource.of(playlist.source)

            feedbackManager.showLoading("Preparing...", "Removing Old Channels")
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
                feedbackManager.showError("No channels found in playlist.")
            }

        } catch (e: Exception) {
            Log.e("PrimeFlixRepo", "Error syncing playlist", e)
            feedbackManager.showError("Sync Failed: ${e.message}")
        }
    }

    private suspend fun syncXtreamStaged(playlist: Playlist): Boolean {
        val input = XtreamInput.decodeFromPlaylistUrl(playlist.url)
        var totalSaved = 0

        // Live
        try {
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
                saveBatch(channels, "Live Channels")
                totalSaved += channels.size
            }
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream Live Failed", e) }

        // Movies
        try {
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
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream VOD Failed", e) }

        // Series
        try {
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
        } catch (e: Exception) { Log.w("PrimeFlixRepo", "Xtream Series Failed", e) }

        return totalSaved > 0
    }

    private suspend fun saveBatch(channels: List<Channel>, typeLabel: String) {
        if (channels.isEmpty()) return
        feedbackManager.showLoading("Processing...", "Saving $typeLabel")

        val batchSize = 1000
        val chunks = channels.chunked(batchSize)
        val totalItems = channels.size
        var itemsSaved = 0

        database.withTransaction {
            chunks.forEach { batch ->
                channelDao.insertAll(batch)
                itemsSaved += batch.size
                feedbackManager.updateCount("Saving $typeLabel...", "Database", itemsSaved, totalItems)
            }
        }
    }

    private suspend fun syncM3U(playlist: Playlist): Boolean {
        feedbackManager.showLoading("Downloading Playlist...", "M3U File")
        try {
            val request = Request.Builder().url(playlist.url).build()
            val response = okHttpClient.newCall(request).execute()
            val inputStream = response.body?.byteStream() ?: return false

            val items = mutableListOf<Channel>()
            m3uParser.parse(inputStream).collect { m3u -> items.add(m3u.toChannel(playlist.url)) }

            if (items.isNotEmpty()) {
                saveBatch(items, "M3U Channels")
                return true
            }
        } catch (e: Exception) { Log.e("PrimeFlixRepo", "M3U Fetch Failed", e) }
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
        } catch (e: Exception) { Log.e("PrimeFlixRepo", "EPG Sync Failed", e) }
    }

    // --- DATA ACCESSORS (OPTIMIZED) ---
    fun getGroups(playlistUrl: String, type: String) = channelDao.getGroups(playlistUrl, type)

    fun getLiveChannels(playlistUrl: String, group: String) =
        channelDao.getLiveChannels(playlistUrl, group, System.currentTimeMillis())

    fun getVodChannels(playlistUrl: String, type: String, group: String) =
        channelDao.getVodChannels(playlistUrl, type, group)

    fun searchChannels(query: String) = channelDao.searchChannels(query)

    // --- FAVORITES & HISTORY ---
    val favorites: Flow<List<Channel>> = channelDao.getFavorites()

    suspend fun toggleFavorite(channel: Channel) {
        channelDao.setFavorite(channel.url, !channel.isFavorite)
    }

    // Pass-through: ViewModel maps this
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

    suspend fun getCurrentProgram(channelId: String) =
        programmeDao.getCurrentProgram(channelId, System.currentTimeMillis())

    suspend fun getProgrammesForChannel(channelId: String, start: Long, end: Long) =
        programmeDao.getProgrammesForChannel(channelId, start, end)

    suspend fun getSeriesEpisodes(playlistUrl: String, seriesId: Int) =
        xtreamParser.getSeriesEpisodes(XtreamInput.decodeFromPlaylistUrl(playlistUrl), seriesId)
}