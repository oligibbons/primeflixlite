package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamLiveStream(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null,
    // FIX: Added category field to satisfy parser instantiation
    @SerialName("category_name") val category: String? = null
)

@Serializable
data class XtreamVodStream(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Double? = null,
    // FIX: Added category field
    @SerialName("category_name") val category: String? = null
)

@Serializable
data class XtreamSeries(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("series_id") val seriesId: Int? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Double? = null,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    // FIX: Added category field
    @SerialName("category_name") val category: String? = null
)