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
    // --- LEGACY / UTILS ---
    @Query("SELECT * FROM streams WHERE playlist_url = :playlistUrl ORDER BY `group`, title")
    fun getChannelsByPlaylist(playlistUrl: String): Flow<List<Channel>>

    @Query("""
        SELECT * FROM streams 
        WHERE title LIKE '%' || :query || '%' 
        OR `group` LIKE '%' || :query || '%'
        LIMIT 50
    """)
    fun searchChannels(query: String): Flow<List<Channel>>

    @Query("SELECT * FROM streams WHERE is_favorite = 1")
    fun getFavorites(): Flow<List<Channel>>

    @Query("UPDATE streams SET is_favorite = :isFav WHERE url = :url")
    suspend fun setFavorite(url: String, isFav: Boolean)

    // --- OPTIMIZED QUERIES FOR HOME SCREEN ---

    // 1. Get Categories (Groups) dynamically based on selected Tab (Type)
    @Query("SELECT DISTINCT `group` FROM streams WHERE playlist_url = :url AND type = :type ORDER BY `group` ASC")
    fun getGroups(url: String, type: String): Flow<List<String>>

    // 2. LIVE TV: Fetches Channels + EPG (Joined)
    // Filters by Group at SQL level to avoid memory overhead
    @Transaction
    @Query("""
        SELECT c.*, 
               p.id as prog_id,
               p.title as prog_title,
               p.description as prog_description,
               p.start as prog_start,
               p.`end` as prog_end,
               p.icon as prog_icon,
               p.channel_id as prog_channel_id,
               p.playlist_url as prog_playlist_url
        FROM streams c
        LEFT JOIN programmes p ON c.relation_id = p.channel_id 
        AND :nowMillis >= p.start AND :nowMillis < p.`end`
        WHERE c.playlist_url = :url 
        AND c.type = 'LIVE'
        AND (:group = 'All' OR c.`group` = :group)
        ORDER BY c.title ASC
    """)
    fun getLiveChannels(url: String, group: String, nowMillis: Long): Flow<List<ChannelWithProgram>>

    // 3. VOD (Movies/Series): Fetches Channels ONLY (No EPG Join needed)
    // Much lighter query for large VOD libraries
    @Query("""
        SELECT * FROM streams 
        WHERE playlist_url = :url 
        AND type = :type 
        AND (:group = 'All' OR `group` = :group)
        ORDER BY title ASC
    """)
    fun getVodChannels(url: String, type: String, group: String): Flow<List<Channel>>

    // --- WRITE OPERATIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Query("DELETE FROM streams WHERE playlist_url = :playlistUrl")
    suspend fun deleteByPlaylist(playlistUrl: String)
}