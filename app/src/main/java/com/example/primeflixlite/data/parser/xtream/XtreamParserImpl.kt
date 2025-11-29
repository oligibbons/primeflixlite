package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class XtreamParserImpl(
    private val client: OkHttpClient
) : XtreamParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    override suspend fun getLiveStreams(input: XtreamInput): List<XtreamChannelInfo.LiveStream> {
        val url = buildUrl(input, "get_live_streams")
        return fetchAndParse(url)
    }

    override suspend fun getVodStreams(input: XtreamInput): List<XtreamChannelInfo.VodStream> {
        val url = buildUrl(input, "get_vod_streams")
        return fetchAndParse(url)
    }

    override suspend fun getSeries(input: XtreamInput): List<XtreamChannelInfo.Series> {
        val url = buildUrl(input, "get_series")
        return fetchAndParse(url)
    }

    // FIXED: Real implementation to fetch episodes
    override suspend fun getSeriesEpisodes(input: XtreamInput, seriesId: Int): List<XtreamChannelInfo.Episode> {
        val url = "${input.basicUrl}/player_api.php?username=${input.username}&password=${input.password}&action=get_series_info&series_id=$seriesId"

        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val body = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        if (continuation.isActive) continuation.resume(emptyList()) // Fail gracefully
                        return
                    }
                    try {
                        // Xtream returns a complex object { "episodes": { "1": [...], "2": [...] }, "info": {...} }
                        // We need to parse this manually or use a specific data structure.
                        // For robustness in this "Lite" app, we'll try a flexible parsing approach:
                        // The 'episodes' field is a Map<String, List<Episode>> where key is Season Number.

                        val container = json.decodeFromString<XtreamChannelInfo.SeriesInfoContainer>(body)
                        // Flatten the map into a single list
                        val allEpisodes = container.episodes.flatMap { entry ->
                            entry.value
                        }.sortedWith(compareBy({ it.season }, { it.episode_num }))

                        if (continuation.isActive) continuation.resume(allEpisodes)
                    } catch (e: Exception) {
                        // Fallback: Log error and return empty to prevent crash
                        e.printStackTrace()
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                }
            })
        }
    }

    private fun buildUrl(input: XtreamInput, action: String): String {
        return "${input.basicUrl}/player_api.php?username=${input.username}&password=${input.password}&action=$action"
    }

    private suspend inline fun <reified T> fetchAndParse(url: String): T {
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)

            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val body = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        if (continuation.isActive) continuation.resumeWithException(IOException("API Error: ${response.code}"))
                        return
                    }
                    try {
                        val result = json.decodeFromString<T>(body)
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        // If parsing fails, try to return a simplified empty list if T is a List
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
            continuation.invokeOnCancellation { call.cancel() }
        }
    }
}