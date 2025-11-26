package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.PlaylistWithChannels
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(playlist: Playlist): Long

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT * FROM playlists")
    fun observeAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE url = :url")
    suspend fun get(url: String): Playlist?

    @Query("SELECT * FROM playlists WHERE url = :url")
    fun observe(url: String): Flow<Playlist?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE url = :url")
    suspend fun getWithChannels(url: String): PlaylistWithChannels?

    @Query("UPDATE playlists SET title = :title WHERE url = :url")
    suspend fun updateTitle(url: String, title: String)
}