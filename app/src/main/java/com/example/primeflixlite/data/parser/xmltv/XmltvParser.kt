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

class XmltvParser(
    private val client: OkHttpClient
) {
    // XMLTV dates are usually "yyyyMMddHHmmss Z"
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(url: String): Flow<List<Programme>> = flow {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        response.body?.byteStream()?.use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            val batch = mutableListOf<Programme>()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                    readProgramme(parser)?.let { batch.add(it) }

                    if (batch.size >= 500) {
                        emit(ArrayList(batch))
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) {
                emit(batch)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun readProgramme(parser: XmlPullParser): Programme? {
        val channelId = parser.getAttributeValue(null, "channel") ?: return null
        val startStr = parser.getAttributeValue(null, "start")
        val stopStr = parser.getAttributeValue(null, "stop")

        var title = ""
        var desc: String? = null

        val start = parseDate(startStr) ?: 0L
        val end = parseDate(stopStr) ?: 0L

        while (parser.next() != XmlPullParser.END_TAG || parser.name != "programme") {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    // FIX: Renamed function calls to match the new method name
                    "title" -> title = extractText(parser)
                    "desc" -> desc = extractText(parser)
                    else -> skip(parser)
                }
            }
        }

        if (title.isEmpty() || start == 0L || end == 0L) return null

        return Programme(
            channelId = channelId,
            playlistUrl = "",
            title = title,
            description = desc,
            start = start,
            end = end
        )
    }

    // FIX: Renamed from readText to extractText to force cache invalidation
    private fun extractText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            // Explicitly capture nullable text into a variable first
            val rawText: String? = parser.text
            result = rawText ?: ""
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

    private fun parseDate(dateStr: String?): Long? {
        return dateStr?.let {
            try {
                dateFormat.parse(it)?.time
            } catch (e: Exception) {
                null
            }
        }
    }
}