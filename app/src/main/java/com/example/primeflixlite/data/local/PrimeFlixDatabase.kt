// file: app/src/main/java/com/example/primeflixlite/data/local/PrimeFlixDatabase.kt
package com.example.primeflixlite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.MediaMetadataDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.dao.ProgrammeDao
import com.example.primeflixlite.data.local.dao.WatchProgressDao
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.MediaMetadata
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.local.entity.WatchProgress

@Database(
    entities = [
        Playlist::class,
        Channel::class,
        Programme::class,
        WatchProgress::class,
        MediaMetadata::class // [New] Registered Metadata Table
    ],
    version = 2, // Increment version if you are migrating, or uninstall app to reset
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PrimeFlixDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun programmeDao(): ProgrammeDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun mediaMetadataDao(): MediaMetadataDao // [New] Accessor
}