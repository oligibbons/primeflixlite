package com.example.primeflixlite.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse<TmdbMovieResult>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: String? = null,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse<TmdbTvResult>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits",
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetails

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits",
        @Query("language") language: String = "en-US"
    ): TmdbTvDetails
}

// --- DTOs ---

@Serializable
data class TmdbSearchResponse<T>(
    @SerialName("results") val results: List<T> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbMovieResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("overview") val overview: String? = null
)

@Serializable
data class TmdbTvResult(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("overview") val overview: String? = null
)

@Serializable
data class TmdbMovieDetails(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("overview") val overview: String? = null,
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerialName("credits") val credits: TmdbCredits? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null
)

@Serializable
data class TmdbTvDetails(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("overview") val overview: String? = null,
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerialName("credits") val credits: TmdbCredits? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null
)

@Serializable
data class TmdbGenre(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String
)

@Serializable
data class TmdbCredits(
    @SerialName("cast") val cast: List<TmdbCast> = emptyList()
)

@Serializable
data class TmdbCast(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("character") val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)