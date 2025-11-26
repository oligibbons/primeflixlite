package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.primeflixlite.Exclude
import com.example.primeflixlite.Likable
import com.example.primeflixlite.data.parser.xtream.XtreamChannelInfo
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.path
import kotlinx.serialization.Serializable

@Entity(tableName = "streams")
@Immutable
@Serializable
@Likable
data class Channel(
    @ColumnInfo(name = "url")
    // playable url
    val url: String,
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
    val urlObj = Url(url)
    // Construct the URL: basicUrl + /series/ + username/password/ + episodeId.extension
    // The original URL is usually http://host:port/player_api.php...
    // This logic assumes the 'url' field passed in is the base domain or playlist URL.
    // Note: We might need to adjust this logic later depending on how we store playlist URLs.
    // For now, we keep the logic generic using Ktor's URL builder.

    val newUrl = URLBuilder(urlObj)
        .apply {
            // Remove 'player_api.php' or other segments if necessary,
            // but usually we build this from the base domain.
            // This is a simplified version of the original logic.
            path(*urlObj.rawSegments.dropLast(1).toTypedArray())
        }
        .appendPathSegments("${episode.id}.${episode.containerExtension}")
        .build()

    return copy(
        url = newUrl.toString(),
        title = episode.title.orEmpty()
    )
}