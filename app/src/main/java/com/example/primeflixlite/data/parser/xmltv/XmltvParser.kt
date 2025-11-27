package com.example.primeflixlite.data.parser.xmltv

import android.util.Xml
import com.example.primeflixlite.data.local.entity.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class XmltvParser(private val okHttpClient: OkHttpClient) {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun parse(url: String): Flow<List<Programme>> = flow {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return@flow

        response.body?.byteStream()?.use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            val batch = mutableListOf<Programme>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                    parseProgramme(parser)?.let {
                        batch.add(it)
                        if (batch.size >= 500) {
                            emit(batch.toList())
                            batch.clear()
                        }
                    }
                }
                eventType = parser.next()
            }
            if (batch.isNotEmpty()) {
                emit(batch)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseProgramme(parser: XmlPullParser): Programme? {
        val startStr = parser.getAttributeValue(null, "start")
        val stopStr = parser.getAttributeValue(null, "stop")
        val channelId = parser.getAttributeValue(null, "channel")

        var title = ""
        var desc = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "desc" -> desc = readText(parser)
                else -> skip(parser)
            }
        }

        return try {
            val startTime = dateFormat.parse(startStr)?.time ?: 0L
            val endTime = dateFormat.parse(stopStr)?.time ?: 0L

            Programme(
                channelId = channelId ?: "",
                title = title,
                description = desc,
                start = startTime, // Fixed: startTime -> start
                end = endTime,     // Fixed: endTime -> end
                playlistUrl = ""   // Will be filled by Repository
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}