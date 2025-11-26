package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamCategory(
    @SerialName("category_id")
    val categoryId: String?, // Changed to String to be safe, API sometimes returns ints or strings
    @SerialName("category_name")
    val categoryName: String?,
    @SerialName("parent_id")
    val parentId: Int?
)