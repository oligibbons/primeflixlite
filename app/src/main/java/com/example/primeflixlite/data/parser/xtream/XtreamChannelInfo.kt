package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models matching the JSON response from Xtream Codes API actions.
 * Used by XtreamParserImpl.
 */
class XtreamChannelInfo {

    @Serializable
    data class LiveStream(
        @SerialName("stream_id") val streamId: Int,
        @SerialName("name") val name: String? = null,
        @SerialName("stream_icon") val streamIcon: String? = null,
        @SerialName("category_id") val categoryId: String? = null,
        @SerialName("epg_channel_id") val epgChannelId: String? = null
    )

    @Serializable
    data class VodStream(
        @SerialName("stream_id") val streamId: Int,
        @SerialName("name") val name: String? = null,
        @SerialName("stream_icon") val streamIcon: String? = null,
        @SerialName("category_id") val categoryId: String? = null,
        @SerialName("container_extension") val containerExtension: String = "mp4",
        @SerialName("rating") val rating: String? = null
    )

    @Serializable
    data class Series(
        @SerialName("series_id") val seriesId: Int,
        @SerialName("name") val name: String? = null,
        @SerialName("cover") val cover: String? = null,
        @SerialName("category_id") val categoryId: String? = null,
        @SerialName("rating") val rating: String? = null
    )

    @Serializable
    data class Episode(
        @SerialName("id") val id: String,
        @SerialName("title") val title: String?,
        @SerialName("container_extension") val containerExtension: String = "mp4",
        @SerialName("season") val season: Int = 0,
        @SerialName("episode_num") val episodeNum: Int = 0
    )
}