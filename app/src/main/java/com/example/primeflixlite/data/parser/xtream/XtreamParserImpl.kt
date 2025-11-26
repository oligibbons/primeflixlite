package com.example.primeflixlite.data.parser.xtream

import android.util.Log
import com.example.primeflixlite.data.local.entity.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class XtreamParserImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : XtreamParser {

    @OptIn(ExperimentalSerializationApi::class)
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    // Helper to replace missing ParserUtils
    private inline fun <reified T> newCall(url: String): T? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) json.decodeFromString<T>(body) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("XtreamParser", "Error fetching $url", e)
            null
        }
    }

    // Helper for streaming lists (Sequence)
    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> newSequenceCall(url: String): Sequence<T> {
        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val stream: InputStream = response.body?.byteStream() ?: return emptySequence()
                json.decodeToSequence<T>(stream)
            } else {
                emptySequence()
            }
        } catch (e: Exception) {
            Log.e("XtreamParser", "Error fetching sequence $url", e)
            emptySequence()
        }
    }

    private inline fun <reified T> newCallOrThrow(url: String): T {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty body")
        return json.decodeFromString<T>(body)
    }

    override suspend fun getInfo(input: XtreamInput): XtreamInfo {
        val (basicUrl, username, password, _) = input
        val infoUrl = XtreamParser.createInfoUrl(basicUrl, username, password)
        return checkNotNull(newCall<XtreamInfo>(infoUrl))
    }

    override fun parse(input: XtreamInput): Flow<XtreamData> = channelFlow {
        val (basicUrl, username, password, type) = input
        val requiredLives = type == null || type == DataSource.Xtream.TYPE_LIVE
        val requiredVods = type == null || type == DataSource.Xtream.TYPE_VOD
        val requiredSeries = type == null || type == DataSource.Xtream.TYPE_SERIES

        val liveStreamsUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_LIVE_STREAMS
        )
        val vodStreamsUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_VOD_STREAMS
        )
        val seriesStreamsUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_SERIES_STREAMS
        )

        if (requiredLives) launch {
            newSequenceCall<XtreamLive>(liveStreamsUrl)
                .asFlow()
                .collect { live -> send(live) }
        }
        if (requiredVods) launch {
            newSequenceCall<XtreamVod>(vodStreamsUrl)
                .asFlow()
                .collect { vod -> send(vod) }
        }
        if (requiredSeries) launch {
            newSequenceCall<XtreamSerial>(seriesStreamsUrl)
                .asFlow()
                .collect { serial -> send(serial) }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getXtreamOutput(input: XtreamInput): XtreamOutput {
        val (basicUrl, username, password, type) = input
        val requiredLives = type == null || type == DataSource.Xtream.TYPE_LIVE
        val requiredVods = type == null || type == DataSource.Xtream.TYPE_VOD
        val requiredSeries = type == null || type == DataSource.Xtream.TYPE_SERIES

        val infoUrl = XtreamParser.createInfoUrl(basicUrl, username, password)
        val liveCategoriesUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_LIVE_CATEGORIES
        )
        val vodCategoriesUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_VOD_CATEGORIES
        )
        val serialCategoriesUrl = XtreamParser.createActionUrl(
            basicUrl, username, password, XtreamParser.Action.GET_SERIES_CATEGORIES
        )

        val info: XtreamInfo = newCall(infoUrl) ?: return XtreamOutput()
        val allowedOutputFormats = info.userInfo.allowedOutputFormats
        val serverProtocol = info.serverInfo.serverProtocol ?: "http"
        val port = info.serverInfo.port?.toIntOrNull()
        val httpsPort = info.serverInfo.httpsPort?.toIntOrNull()

        val liveCategories: List<XtreamCategory> =
            if (requiredLives) newCall(liveCategoriesUrl) ?: emptyList() else emptyList()
        val vodCategories: List<XtreamCategory> =
            if (requiredVods) newCall(vodCategoriesUrl) ?: emptyList() else emptyList()
        val serialCategories: List<XtreamCategory> =
            if (requiredSeries) newCall(serialCategoriesUrl) ?: emptyList() else emptyList()

        return XtreamOutput(
            liveCategories = liveCategories,
            vodCategories = vodCategories,
            serialCategories = serialCategories,
            allowedOutputFormats = allowedOutputFormats,
            serverProtocol = serverProtocol,
            port = if (serverProtocol == "http") port else httpsPort
        )
    }

    override suspend fun getSeriesInfoOrThrow(
        input: XtreamInput,
        seriesId: Int
    ): XtreamChannelInfo {
        val (basicUrl, username, password, type) = input
        check(type == DataSource.Xtream.TYPE_SERIES) { "xtream input type must be `series`" }
        return newCallOrThrow(
            XtreamParser.createActionUrl(
                basicUrl,
                username,
                password,
                XtreamParser.Action.GET_SERIES_INFO,
                XtreamParser.GET_SERIES_INFO_PARAM_ID to seriesId
            )
        )
    }
}