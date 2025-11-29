package com.example.primeflixlite.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Formats milliseconds into "mm:ss" or "HH:mm:ss"
     */
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "00:00"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats timestamp to HH:mm (e.g., "14:30")
     */
    fun formatTime(millis: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    /**
     * Calculates progress (0.0 to 1.0) for EPG bars
     */
    fun getProgress(start: Long, end: Long): Float {
        val now = System.currentTimeMillis()
        if (now < start) return 0f
        if (now > end) return 1f
        val duration = end - start
        if (duration <= 0) return 0f
        return (now - start).toFloat() / duration.toFloat()
    }
}