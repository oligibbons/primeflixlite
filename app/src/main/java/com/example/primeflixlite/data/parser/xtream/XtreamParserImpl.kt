package com.example.primeflixlite.data.parser.xtream

import com.example.primeflixlite.util.FeedbackManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalSerializationApi::class)
class XtreamParserImpl(
    private val client: OkHttpClient,
    private val feedbackManager: FeedbackManager
) : XtreamParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Serializable
    private data class SeriesInfoContainer(
        @SerialName("episodes") val episodes: Map<String, List<XtreamChannelInfo.Episode>> = emptyMap()
    )

    override suspend fun getLiveStreams(input: XtreamInput): List<XtreamChannelInfo.LiveStream> {
        val url = buildUrl(input, "get_live_streams")
        feedbackManager.showLoading("Downloading Live TV...", "Channels")
        return fetchAndParse(url)
    }

    override suspend fun getVodStreams(input: XtreamInput): List<XtreamChannelInfo.VodStream> {
        val url = buildUrl(input, "get_vod_streams")
        feedbackManager.showLoading("Downloading Movies...", "VOD Library")
        return fetchAndParse(url)
    }

    override suspend fun getSeries(input: XtreamInput): List<XtreamChannelInfo.Series> {
        val url = buildUrl(input, "get_series")
        feedbackManager.showLoading("Downloading Series...", "TV Shows")
        return fetchAndParse(url)
    }

    override suspend fun getSeriesEpisodes(input: XtreamInput, seriesId: Int): List<XtreamChannelInfo.Episode> {
        val url = "${input.basicUrl}/player_api.php?username=${input.username}&password=${input.password}&action=get_series_info&series_id=$seriesId"

        // No feedback needed for small individual series requests to avoid UI spam
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { resp ->
                        val stream = resp.body?.byteStream()
                        if (!resp.isSuccessful || stream == null) {
                            if (continuation.isActive) continuation.resume(emptyList())
                            return
                        }
                        try {
                            val container = json.decodeFromStream<SeriesInfoContainer>(stream)
                            val allEpisodes = container.episodes.flatMap { entry ->
                                entry.value
                            }.sortedWith(compareBy({ it.season }, { it.episodeNum }))

                            if (continuation.isActive) continuation.resume(allEpisodes)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                }
            })
        }
    }

    private fun buildUrl(input: XtreamInput, action: String): String {
        return "${input.basicUrl}/player_api.php?username=${input.username}&password=${input.password}&action=$action"
    }

    private suspend inline fun <reified T> fetchAndParse(url: String): T {
        val serializer = serializer<T>()

        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)

            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    feedbackManager.showError("Network Error: ${e.message}")
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            val errorMsg = "API Error: ${resp.code}"
                            feedbackManager.showError(errorMsg)
                            if (continuation.isActive) continuation.resumeWithException(IOException(errorMsg))
                            return
                        }

                        val body = resp.body
                        if (body == null) {
                            if (continuation.isActive) continuation.resumeWithException(IOException("Empty Body"))
                            return
                        }

                        // WRAP THE STREAM TO TRACK PROGRESS
                        val progressSource = ProgressResponseBody(body) { progress ->
                            feedbackManager.updateProgress(progress)
                        }.source()

                        try {
                            // Decode directly from the progress-tracking source
                            val result = json.decodeFromStream(serializer, progressSource.inputStream())
                            if (continuation.isActive) continuation.resume(result)
                        } catch (e: Exception) {
                            feedbackManager.showError("Parse Error: ${e.message}")
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }
                }
            })
            continuation.invokeOnCancellation { call.cancel() }
        }
    }

    // --- Helper Class to Intercept Bytes ---
    private class ProgressResponseBody(
        private val responseBody: ResponseBody,
        private val onProgress: (Float) -> Unit
    ) : ForwardingSource(responseBody.source()) {
        private var totalBytesRead = 0L
        private val contentLength = responseBody.contentLength()

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) {
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val percent = totalBytesRead.toFloat() / contentLength.toFloat()
                    onProgress(percent)
                }
            }
            return bytesRead
        }
    }
}