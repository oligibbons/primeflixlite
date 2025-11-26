package com.example.primeflixlite.data.parser.xtream

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
// ADDED THESE MISSING IMPORTS:
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

    override suspend fun getSeriesEpisodes(input: XtreamInput, seriesId: Int): List<XtreamChannelInfo.Episode> {
        // Simplified implementation to prevent crash
        return emptyList()
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
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })

            continuation.invokeOnCancellation { call.cancel() }
        }
    }
}