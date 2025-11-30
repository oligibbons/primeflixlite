package com.example.primeflixlite.data.repository

import android.util.Log
import com.example.primeflixlite.data.local.dao.MediaMetadataDao
import com.example.primeflixlite.data.local.entity.MediaMetadata
import com.example.primeflixlite.data.remote.TmdbService
import com.example.primeflixlite.util.TitleNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepository @Inject constructor(
    private val tmdbService: TmdbService,
    private val metadataDao: MediaMetadataDao
) {
    // API Key integrated
    private val apiKey = "586e2ebad885fee1da018af1bb91adaf"

    /**
     * Tries to find metadata for a title.
     * 1. Checks Local DB (fast).
     * 2. If missing, Searches TMDB API.
     * 3. Fetches Details (Cast, etc.).
     * 4. Caches result.
     */
    suspend fun getOrFetchMetadata(
        rawTitle: String,
        type: String // "movie" or "tv"
    ): MediaMetadata? = withContext(Dispatchers.IO) {
        // 1. Clean the title (e.g., "Spiderman.No.Way.Home.2021.mkv" -> "Spider-Man No Way Home")
        val normInfo = TitleNormalizer.parse(rawTitle)
        val hash = TitleNormalizer.generateGroupKey(normInfo.normalizedTitle)

        // 2. Check Cache
        val cached = metadataDao.getByTitleHash(hash)
        if (cached != null) return@withContext cached

        // 3. Fetch from Network
        try {
            // Search to get the ID
            val remoteId = searchTmdb(normInfo.normalizedTitle, normInfo.year, type)
                ?: return@withContext null

            // Get Full Details using that ID
            val metadata = fetchDetailsAndCache(remoteId, type, hash, normInfo.normalizedTitle)
            return@withContext metadata

        } catch (e: Exception) {
            Log.e("MetadataRepo", "Failed to fetch metadata for $rawTitle: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun searchTmdb(query: String, year: String?, type: String): Int? {
        return try {
            if (type == "movie") {
                val response = tmdbService.searchMovie(apiKey, query, year)
                response.results.firstOrNull()?.id
            } else {
                val response = tmdbService.searchTv(apiKey, query, year)
                response.results.firstOrNull()?.id
            }
        } catch (e: Exception) {
            Log.w("MetadataRepo", "Search failed for $query")
            null
        }
    }

    private suspend fun fetchDetailsAndCache(
        tmdbId: Int,
        type: String,
        hash: String,
        canonicalTitle: String
    ): MediaMetadata {
        val overview: String?
        val poster: String?
        val backdrop: String?
        val vote: Double
        val genres: String
        val cast: String?
        val releaseDate: String?

        if (type == "movie") {
            val details = tmdbService.getMovieDetails(tmdbId, apiKey)
            overview = details.overview
            poster = details.posterPath
            backdrop = details.backdropPath
            vote = details.voteAverage
            genres = details.genres.joinToString { it.name }
            cast = details.credits?.cast?.take(5)?.joinToString { it.name }
            releaseDate = null // Details endpoint doesn't always have date in root, safe to skip
        } else {
            val details = tmdbService.getTvDetails(tmdbId, apiKey)
            overview = details.overview
            poster = details.posterPath
            backdrop = details.backdropPath
            vote = details.voteAverage
            genres = details.genres.joinToString { it.name }
            cast = details.credits?.cast?.take(5)?.joinToString { it.name }
            releaseDate = null
        }

        val entity = MediaMetadata(
            tmdbId = tmdbId,
            mediaType = type,
            normalizedTitleHash = hash, // Key for future lookups
            title = canonicalTitle,
            originalTitle = null,
            overview = overview,
            posterPath = poster,
            backdropPath = backdrop,
            voteAverage = vote,
            releaseDate = releaseDate,
            genres = genres,
            castPreview = cast,
            lastUpdated = System.currentTimeMillis()
        )

        metadataDao.insert(entity)
        return entity
    }
}