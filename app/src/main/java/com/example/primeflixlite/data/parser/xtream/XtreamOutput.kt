package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamOutput(
    @SerialName("live_categories")
    val liveCategories: List<XtreamCategory> = emptyList(),
    @SerialName("vod_categories")
    val vodCategories: List<XtreamCategory> = emptyList(),
    @SerialName("series_categories")
    val serialCategories: List<XtreamCategory> = emptyList(),
    @SerialName("allowed_output_formats")
    val allowedOutputFormats: List<String> = emptyList(),
    @SerialName("server_protocol")
    val serverProtocol: String = "http",
    @SerialName("port")
    val port: String? = null
)