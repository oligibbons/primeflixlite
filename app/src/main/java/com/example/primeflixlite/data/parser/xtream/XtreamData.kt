package com.example.primeflixlite.data.parser.xtream

import com.example.primeflixlite.data.local.entity.Channel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface XtreamData

@Serializable
data class XtreamLive(
    @SerialName("category_id")
    val categoryId: String? = null, // Changed to String to match typical API responses
    @SerialName("epg_channel_id")
    val epgChannelId: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("stream_icon")
    val streamIcon: String? = null,
    @SerialName("stream_id")
    val streamId: Int? = null,
    @SerialName("stream_type")
    val streamType: String? = null,
) : XtreamData

@Serializable
data class XtreamVod(
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("container_extension")
    val containerExtension: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("stream_icon")
    val streamIcon: String? = null,
    @SerialName("stream_id")
    val streamId: Int? = null,
    @SerialName("stream_type")
    val streamType: String? = null
) : XtreamData

@Serializable
data class XtreamSerial(
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("cover")
    val cover: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("series_id")
    val seriesId: Int? = null,
) : XtreamData

fun XtreamLive.toChannel(
    basicUrl: String,
    username: String,
    password: String,
    playlistUrl: String,
    category: String,
    containerExtension: String
): Channel = Channel(
    url = "$basicUrl/live/$username/$password/$streamId.$containerExtension",
    category = category,
    title = name.orEmpty(),
    cover = streamIcon,
    playlistUrl = playlistUrl,
    relationId = epgChannelId
)

fun XtreamVod.toChannel(
    basicUrl: String,
    username: String,
    password: String,
    playlistUrl: String,
    category: String
): Channel = Channel(
    url = "$basicUrl/movie/$username/$password/$streamId.${containerExtension ?: "mp4"}",
    category = category,
    title = name.orEmpty(),
    cover = streamIcon,
    playlistUrl = playlistUrl,
    relationId = streamId?.toString()
)

fun XtreamSerial.asChannel(
    basicUrl: String,
    username: String,
    password: String,
    playlistUrl: String,
    category: String
): Channel = Channel(
    url = "$basicUrl/series/$username/$password/$seriesId",
    category = category,
    title = name.orEmpty(),
    cover = cover,
    playlistUrl = playlistUrl,
    relationId = seriesId?.toString()
)