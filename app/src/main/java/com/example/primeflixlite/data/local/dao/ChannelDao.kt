package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.primeflixlite.data.local.entity.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM streams WHERE playlist_url = :playlistUrl ORDER BY `group`, title")
    fun getChannelsByPlaylist(playlistUrl: String): Flow<List<Channel>>

    @Query("SELECT * FROM streams WHERE playlist_url = :playlistUrl")
    suspend fun getChannelsByPlaylistSync(playlistUrl: String): List<Channel>

    @Query("SELECT * FROM streams WHERE favourite = 1 ORDER BY title")
    fun getFavouriteChannels(): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Query("DELETE FROM streams WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)

    @Query("SELECT COUNT(*) FROM streams WHERE playlist_url = :playlistUrl")
    suspend fun countByPlaylist(playlistUrl: String): Int

    @Transaction
    suspend fun replacePlaylistChannels(playlistUrl: String, newChannels: List<Channel>) {
        deleteByPlaylist(playlistUrl)
        insertAll(newChannels)
    }
}