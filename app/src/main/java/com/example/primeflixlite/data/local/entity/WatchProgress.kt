package com.example.primeflixlite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_progress",
    indices = [Index(value = ["channelUrl"], unique = true)]
)
data class WatchProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelUrl: String, // Stable link to the content
    val position: Long,     // Playback position in ms
    val duration: Long,     // Total duration in ms
    val lastPlayed: Long    // Timestamp for sorting "Recently Watched"
) {
    // Helper to calculate percentage for the UI progress bar
    fun getProgressPercent(): Float {
        if (duration <= 0) return 0f
        return position.toFloat() / duration.toFloat()
    }

    // Helper to check if it's "finished" (e.g., > 95% watched)
    fun isFinished(): Boolean {
        return getProgressPercent() > 0.95f
    }
}