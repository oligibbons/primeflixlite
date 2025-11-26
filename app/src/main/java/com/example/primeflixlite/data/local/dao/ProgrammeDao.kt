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
    suspend fun insertOrReplaceAll(vararg programmes: Programme)

    @Query("DELETE FROM programmes WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylistUrl(playlistUrl: String)

    @Query("DELETE FROM programmes WHERE playlist_url = :playlistUrl AND channel_id = :channelId")
    suspend fun deleteByPlaylistUrlAndChannelId(playlistUrl: String, channelId: String)

    // Get programs for a specific channel, currently playing or future
    @Query("""
        SELECT * FROM programmes 
        WHERE playlist_url = :playlistUrl 
        AND channel_id = :channelId 
        AND `end` > :currentTime
        ORDER BY start ASC
    """)
    fun observeProgrammes(playlistUrl: String, channelId: String, currentTime: Long): Flow<List<Programme>>

    // Get the single program currently playing right now
    @Query("""
        SELECT * FROM programmes 
        WHERE playlist_url = :playlistUrl 
        AND channel_id = :channelId 
        AND start <= :currentTime 
        AND `end` > :currentTime
        LIMIT 1
    """)
    fun observeCurrentProgramme(playlistUrl: String, channelId: String, currentTime: Long): Flow<Programme?>

    @Query("SELECT * FROM programmes WHERE playlist_url = :playlistUrl AND channel_id = :channelId")
    suspend fun getByPlaylistUrlAndChannelId(playlistUrl: String, channelId: String): List<Programme>
}