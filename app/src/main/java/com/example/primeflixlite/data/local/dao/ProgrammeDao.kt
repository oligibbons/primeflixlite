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
    suspend fun insertOrReplace(programme: Programme)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<Programme>)

    @Query("DELETE FROM programmes WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylistUrl(playlistUrl: String)

    // NEW: Used by Repository to clean up old EPG data
    @Query("DELETE FROM programmes WHERE `end` < :currentTime")
    suspend fun deleteOldProgrammes(currentTime: Long)

    @Query("""
        SELECT * FROM programmes 
        WHERE playlist_url = :playlistUrl 
        AND channel_id = :channelId 
        AND `end` > :currentTime
        ORDER BY start ASC
    """)
    fun observeProgrammes(playlistUrl: String, channelId: String, currentTime: Long): Flow<List<Programme>>

    // NEW: Used by Player UI for instant lookup (ignores playlistUrl for simplicity)
    @Query("""
        SELECT * FROM programmes 
        WHERE channel_id = :channelId 
        AND start <= :currentTime 
        AND `end` > :currentTime
        LIMIT 1
    """)
    suspend fun getCurrentProgram(channelId: String, currentTime: Long): Programme?
}