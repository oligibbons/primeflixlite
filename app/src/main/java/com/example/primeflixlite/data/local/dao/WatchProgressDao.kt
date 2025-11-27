package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.entity.WatchProgress
import com.example.primeflixlite.data.local.model.ChannelWithProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)

    @Query("SELECT * FROM watch_progress WHERE channelUrl = :url")
    suspend fun getProgress(url: String): WatchProgress?

    // FIX: Changed 'FROM channel' to 'FROM streams'
    @Transaction
    @Query("""
        SELECT c.*, w.position as prog_position, w.duration as prog_duration, w.lastPlayed as prog_lastPlayed
        FROM streams c
        INNER JOIN watch_progress w ON c.url = w.channelUrl
        WHERE c.type = :type
        ORDER BY w.lastPlayed DESC
        LIMIT 20
    """)
    fun getContinueWatching(type: StreamType): Flow<List<ChannelWithProgress>>

    // FIX: Changed 'FROM channel' to 'FROM streams'
    @Transaction
    @Query("""
        SELECT c.*, w.position as prog_position, w.duration as prog_duration, w.lastPlayed as prog_lastPlayed
        FROM streams c
        INNER JOIN watch_progress w ON c.url = w.channelUrl
        WHERE c.type = 'LIVE'
        ORDER BY w.lastPlayed DESC
        LIMIT 10
    """)
    fun getRecentChannels(): Flow<List<ChannelWithProgress>>
}