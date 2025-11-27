package com.example.primeflixlite.data.parser.m3u

data class M3UData(
    val duration: Long = -1,
    val name: String? = null,
    val url: String,
    val group: String? = null,
    val logo: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    // NEW: Added fields to satisfy the parser
    val category: String? = null,
    val seen: Boolean = false,
    val licenseType: String? = null,
    val licenseKey: String? = null
)