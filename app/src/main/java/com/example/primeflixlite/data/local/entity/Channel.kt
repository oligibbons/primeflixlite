package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.primeflixlite.Exclude
import com.example.primeflixlite.Likable
import com.example.primeflixlite.data.parser.xtream.XtreamChannelInfo
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Entity(tableName = "streams")
@Immutable
@Serializable
@Likable
data class Channel(
    @ColumnInfo(name = "url")
    val url: String, // playable url
    @ColumnInfo(name = "group")
    val category: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "cover")
    val cover: String? = null,
    @ColumnInfo(name = "playlist_url", index = true)
    val playlistUrl: String,
    @ColumnInfo(name = "license_type", defaultValue = "NULL")
    val licenseType: String? = null,
    @ColumnInfo(name = "license_key", defaultValue = "NULL")
    val licenseKey: String? = null,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Exclude
    val id: Int = 0,
    // extra fields
    @ColumnInfo(name = "favourite", index = true)
    @Exclude
    val favourite: Boolean = false,
    @ColumnInfo(name = "hidden", defaultValue = "0")
    @Exclude
    val hidden: Boolean = false,
    @ColumnInfo(name = "seen", defaultValue = "0")
    @Exclude
    val seen: Long = 0L,
    @ColumnInfo(name = "relation_id", defaultValue = "NULL")
    @Exclude
    val relationId: String? = null
) {
    companion object {
        const val LICENSE_TYPE_WIDEVINE = "com.widevine.alpha"
        const val LICENSE_TYPE_CLEAR_KEY = "clearkey"
        const val LICENSE_TYPE_CLEAR_KEY_2 = "org.w3.clearkey"
        const val LICENSE_TYPE_PLAY_READY = "com.microsoft.playready"
    }
}

// Helper to convert a Series Episode into a playable Channel object
fun Channel.copyXtreamEpisode(episode: XtreamChannelInfo.Episode): Channel {
    // Replaced Ktor logic with OkHttp to avoid extra dependencies
    val httpUrl = url.toHttpUrlOrNull()

    val newUrl = if (httpUrl != null) {
        // Xtream Codes logic: Base URL (player_api.php) -> Root URL + /series/ + ...
        // We usually drop the last segment (e.g. 'player_api.php') and append the stream ID
        httpUrl.newBuilder()
            .removePathSegment(httpUrl.pathSize - 1)
            .addPathSegment("${episode.id}.${episode.containerExtension}")
            .build()
            .toString()
    } else {
        // Fallback for non-standard URLs
        "$url/${episode.id}.${episode.containerExtension}"
    }

    return copy(
        url = newUrl,
        title = episode.title.orEmpty()
    )
}