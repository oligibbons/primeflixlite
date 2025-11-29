package com.example.primeflixlite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.dao.ProgrammeDao
import com.example.primeflixlite.data.local.dao.WatchProgressDao
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.local.entity.WatchProgress

@Database(
    entities = [
        Playlist::class,
        Channel::class,
        Programme::class,      // NEW: EPG Data
        WatchProgress::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // Ensures StreamType enum works
abstract class PrimeFlixDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun programmeDao(): ProgrammeDao
    abstract fun watchProgressDao(): WatchProgressDao
}