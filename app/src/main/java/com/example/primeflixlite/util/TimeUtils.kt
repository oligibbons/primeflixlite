package com.example.primeflixlite.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    /**
     * Returns current system time formatted as "HH:mm" (e.g. 20:30)
     */
    fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Formats a millisecond timestamp to "HH:mm"
     */
    fun formatTime(millis: Long): String {
        if (millis <= 0) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Formats duration ms to "1h 30m" or "45m"
     */
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0m"

        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val remainingMinutes = minutes % 60

        return if (hours > 0) {
            "${hours}h ${remainingMinutes}m"
        } else {
            "${remainingMinutes}m"
        }
    }

    /**
     * Calculates progress percentage (0.0 to 1.0) for a program
     * based on current system time.
     */
    fun getProgress(start: Long, end: Long): Float {
        val now = System.currentTimeMillis()
        if (now < start) return 0f
        if (now > end) return 1f
        if (end <= start) return 0f

        val totalDuration = end - start
        val elapsed = now - start

        return (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }
}