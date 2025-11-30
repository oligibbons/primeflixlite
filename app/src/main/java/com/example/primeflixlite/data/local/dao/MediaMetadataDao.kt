// file: app/src/main/java/com/example/primeflixlite/data/local/dao/MediaMetadataDao.kt
package com.example.primeflixlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.primeflixlite.data.local.entity.MediaMetadata

@Dao
interface MediaMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: MediaMetadata)

    @Query("SELECT * FROM media_metadata WHERE tmdb_id = :tmdbId LIMIT 1")
    suspend fun getByTmdbId(tmdbId: Int): MediaMetadata?

    // Fast lookup using the "cleaned" title hash from our TitleNormalizer
    @Query("SELECT * FROM media_metadata WHERE normalized_title_hash = :hash LIMIT 1")
    suspend fun getByTitleHash(hash: String): MediaMetadata?

    @Query("SELECT * FROM media_metadata WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MediaMetadata>

    // Useful for cleaning up old cache if space is tight
    @Query("DELETE FROM media_metadata WHERE last_updated < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}