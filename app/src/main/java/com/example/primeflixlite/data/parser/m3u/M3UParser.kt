package com.example.primeflixlite.data.parser.m3u

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

/**
 * Interface for parsing M3U/M3U8 playlists.
 */
interface M3UParser {
    fun parse(input: InputStream): Flow<M3UData>
}