package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.entity.WatchProgress

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)

    @Query("SELECT * FROM WatchProgress WHERE channelUrl = :url")
    suspend fun getProgress(url: String): WatchProgress?

    // Fetches the most recently watched items first
    @Query("SELECT * FROM WatchProgress ORDER BY lastPlayed DESC LIMIT 20")
    fun getRecentChannels(): Flow<List<WatchProgress>>

    // Joins with Channel table to filter by StreamType (Live, Movie, Series)
    // This ensures your "Movies" tab only shows Movie progress.
    @Query("""
        SELECT wp.* FROM WatchProgress wp
        INNER JOIN Channel c ON wp.channelUrl = c.url
        WHERE c.type = :type
        ORDER BY wp.lastPlayed DESC
        LIMIT 20
    """)
    suspend fun getContinueWatching(type: StreamType): List<WatchProgress>
}