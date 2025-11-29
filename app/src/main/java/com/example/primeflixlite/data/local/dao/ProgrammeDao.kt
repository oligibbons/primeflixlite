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

    // FIX: Table "programmes", Column "channel_id", Quoted `end`
    @Query("""
        SELECT * FROM programmes 
        WHERE channel_id = :channelId 
        AND start <= :now 
        AND `end` > :now 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(channelId: String, now: Long): Programme?

    // FIX: Table "programmes", Columns "channel_id", Quoted `end`
    @Query("""
        SELECT * FROM programmes 
        WHERE channel_id = :channelId 
        AND `end` > :startTime 
        AND start < :endTime
        ORDER BY start ASC
    """)
    suspend fun getProgrammesForChannel(channelId: String, startTime: Long, endTime: Long): List<Programme>

    // FIX: Table "programmes", Quoted `end`
    @Query("DELETE FROM programmes WHERE `end` < (:now - 7200000)")
    suspend fun deleteOldProgrammes(now: Long)

    // FIX: Table "programmes", Column "playlist_url"
    @Query("DELETE FROM programmes WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)
}