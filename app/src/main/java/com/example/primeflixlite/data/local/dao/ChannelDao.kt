package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.primeflixlite.data.local.entity.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(channel: Channel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(vararg channels: Channel)

    @Delete
    suspend fun delete(channel: Channel)

    @Query("DELETE FROM streams WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylistUrl(playlistUrl: String)

    @Query("SELECT * FROM streams WHERE id = :id")
    suspend fun get(id: Int): Channel?

    // Main feed for the TV Grid
    @Query("SELECT * FROM streams WHERE playlist_url = :playlistUrl")
    fun observeAllByPlaylistUrl(playlistUrl: String): Flow<List<Channel>>

    // Filtering by Category (e.g., "Action Movies")
    @Query("SELECT * FROM streams WHERE playlist_url = :playlistUrl AND `group` = :category")
    fun observeByCategory(playlistUrl: String, category: String): Flow<List<Channel>>

    // Favorites for the "Dashboard"
    @Query("SELECT * FROM streams WHERE favourite = 1")
    fun observeAllFavorites(): Flow<List<Channel>>

    // Get all unique categories to build the sidebar
    @Query("SELECT DISTINCT `group` FROM streams WHERE playlist_url = :playlistUrl ORDER BY `group` ASC")
    fun observeCategories(playlistUrl: String): Flow<List<String>>

    @Query("UPDATE streams SET favourite = :target WHERE id = :id")
    suspend fun setFavorite(id: Int, target: Boolean)

    @Query("SELECT COUNT(*) FROM streams WHERE playlist_url = :playlistUrl")
    fun observeCount(playlistUrl: String): Flow<Int>
}