package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.primeflixlite.data.local.entity.Programme

@Dao
interface ProgrammeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<Programme>)

    @Query("DELETE FROM programmes WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)

    // Delete programs that have ended before the current time
    @Query("DELETE FROM programmes WHERE `end` < :currentTimeMillis")
    suspend fun deleteOldProgrammes(currentTimeMillis: Long)

    // Get the currently playing program
    @Query("SELECT * FROM programmes WHERE channel_id = :channelId AND start <= :now AND `end` > :now LIMIT 1")
    suspend fun getCurrentProgram(channelId: String, now: Long): Programme?

    // FIX: Added "AND start < :end" to use the unused parameter and correctly filter the timeline
    @Query("SELECT * FROM programmes WHERE channel_id = :channelId AND `end` > :start AND start < :end ORDER BY start ASC")
    suspend fun getProgrammesForChannel(channelId: String, start: Long, end: Long): List<Programme>
}