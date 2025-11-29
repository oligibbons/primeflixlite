package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.primeflixlite.data.local.entity.Programme
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgrammeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<Programme>)

    // Used by Player & Home for "Now Playing"
    @Query("""
        SELECT * FROM Programme 
        WHERE channelId = :channelId 
        AND start <= :now 
        AND end > :now 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(channelId: String, now: Long): Programme?

    // NEW: Used by TV Guide to show the schedule
    // We limit to 24 hours to save memory on the 1GB device
    @Query("""
        SELECT * FROM Programme 
        WHERE channelId = :channelId 
        AND end > :startTime 
        AND start < :endTime
        ORDER BY start ASC
    """)
    suspend fun getProgrammesForChannel(channelId: String, startTime: Long, endTime: Long): List<Programme>

    // Clean up: Remove shows that ended > 2 hours ago
    @Query("DELETE FROM Programme WHERE end < (:now - 7200000)")
    suspend fun deleteOldProgrammes(now: Long)

    @Query("DELETE FROM Programme WHERE playlistUrl = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)
}