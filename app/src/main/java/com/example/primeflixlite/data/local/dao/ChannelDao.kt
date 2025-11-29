package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM Channel WHERE playlistUrl = :playlistUrl")
    fun getChannelsByPlaylist(playlistUrl: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    // FIXED: Now implemented for SettingsViewModel
    @Query("DELETE FROM Channel WHERE playlistUrl = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)

    @Transaction
    suspend fun replacePlaylistChannels(playlistUrl: String, channels: List<Channel>) {
        deleteByPlaylist(playlistUrl)
        insertAll(channels)
    }

    @Query("SELECT * FROM Channel WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchChannels(query: String): Flow<List<Channel>>

    @Transaction
    @Query("""
        SELECT c.*, p.title as programTitle, p.start as programStart, p.end as programEnd 
        FROM Channel c 
        LEFT JOIN Programme p ON c.relationId = p.channelId 
        AND p.start <= :now AND p.end > :now
        WHERE c.playlistUrl = :playlistUrl
    """)
    fun getChannelsWithEpg(playlistUrl: String, now: Long): Flow<List<ChannelWithProgram>>

    // --- NEW: FAVORITES ---
    @Query("SELECT * FROM Channel WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Channel>>

    @Query("UPDATE Channel SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun setFavorite(url: String, isFavorite: Boolean)
}