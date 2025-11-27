package com.example.primeflixlite.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatTime(millis: Long): String {
        return if (millis > 0) timeFormat.format(Date(millis)) else ""
    }

    // Returns a float between 0.0 and 1.0 for the progress bar
    fun getProgress(start: Long, end: Long): Float {
        val now = System.currentTimeMillis()
        if (now < start) return 0f
        if (now > end) return 1f
        val total = end - start
        if (total <= 0) return 0f
        return (now - start).toFloat() / total
    }

    fun getDurationString(start: Long, end: Long): String {
        return "${formatTime(start)} - ${formatTime(end)}"
    }
}