package com.example.primeflixlite.data.parser.m3u

data class M3UData(
    val duration: Long = -1,
    // Renamed 'name' to 'title' to fix Unresolved Reference in Parser extension
    val title: String? = null,
    val url: String,
    val group: String? = null,
    val logo: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val category: String? = null,
    val seen: Boolean = false,
    val licenseType: String? = null,
    val licenseKey: String? = null
)