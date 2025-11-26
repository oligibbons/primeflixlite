package com.m3u.data.parser.xtream

data class XtreamOutput(
    val liveCategories: List<`XtreamCategory.kt`> = emptyList(),
    val vodCategories: List<`XtreamCategory.kt`> = emptyList(),
    val serialCategories: List<`XtreamCategory.kt`> = emptyList(),
    val allowedOutputFormats: List<String> = emptyList(),
    val serverProtocol: String = "http",
    val port: Int? = null
)