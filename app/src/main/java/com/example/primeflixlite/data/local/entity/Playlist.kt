package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.primeflixlite.Exclude
import com.example.primeflixlite.Likable
import com.example.primeflixlite.R
import com.example.primeflixlite.data.parser.xtream.XtreamInput
import com.example.primeflixlite.startsWithAny
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Entity(tableName = "playlists")
@Immutable
@Serializable
@Likable
data class Playlist(
    @ColumnInfo(name = "title")
    val title: String,
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    // extra fields
    @ColumnInfo(name = "pinned_groups", defaultValue = "[]")
    @Exclude
    val pinnedCategories: List<String> = emptyList(),
    @ColumnInfo(name = "hidden_groups", defaultValue = "[]")
    @Exclude
    val hiddenCategories: List<String> = emptyList(),
    @ColumnInfo(name = "source", defaultValue = "0")
    @Serializable(with = DataSourceSerializer::class)
    val source: DataSource = DataSource.M3U,
    @ColumnInfo(name = "user_agent", defaultValue = "NULL")
    @Exclude
    val userAgent: String? = null,
    // epg playlist urls
    @ColumnInfo(name = "epg_urls", defaultValue = "[]")
    @Exclude
    val epgUrls: List<String> = emptyList(),
    @ColumnInfo(name = "auto_refresh_programmes", defaultValue = "0")
    @Exclude
    val autoRefreshProgrammes: Boolean = false
) {
    companion object {
        const val URL_IMPORTED = "imported"
        val SERIES_TYPES = arrayOf(DataSource.Xtream.TYPE_SERIES)
        val VOD_TYPES = arrayOf(DataSource.Xtream.TYPE_VOD)
    }
}

val Playlist.isSeries: Boolean get() = type in Playlist.SERIES_TYPES
val Playlist.isVod: Boolean get() = type in Playlist.VOD_TYPES

val Playlist.refreshable: Boolean
    get() = source == DataSource.M3U && url != Playlist.URL_IMPORTED && !url.startsWithAny(
        "file://", "content://", ignoreCase = true
    )

val Playlist.type: String?
    get() = when (source) {
        DataSource.Xtream -> XtreamInput.decodeFromPlaylistUrl(url).type
        else -> null
    }

fun Playlist.epgUrlsOrXtreamXmlUrl(): List<String> = when (source) {
    DataSource.Xtream -> {
        when (type) {
            DataSource.Xtream.TYPE_LIVE -> {
                val input = XtreamInput.decodeFromPlaylistUrl(url)
                // Manually construct the XMLTV URL to avoid circular dependencies or missing parsers
                val epgUrl = "${input.basicUrl}/xmltv.php?username=${input.username}&password=${input.password}"
                listOf(epgUrl)
            }
            else -> emptyList()
        }
    }
    else -> epgUrls
}

@Immutable
sealed class DataSource(
    val resId: Int,
    val value: String,
    val supported: Boolean = false
) {
    object M3U : DataSource(R.string.feat_setting_data_source_m3u, "m3u", true)
    object EPG : DataSource(R.string.feat_setting_data_source_epg, "epg", true)
    object Xtream : DataSource(R.string.feat_setting_data_source_xtream, "xtream", true) {
        const val TYPE_LIVE = "live"
        const val TYPE_VOD = "vod"
        const val TYPE_SERIES = "series"
    }
    object Emby : DataSource(R.string.feat_setting_data_source_emby, "emby")
    object Dropbox : DataSource(R.string.feat_setting_data_source_dropbox, "dropbox")

    override fun toString(): String = value

    companion object {
        fun of(value: String): DataSource = when (value) {
            "m3u" -> M3U
            "epg" -> EPG
            "xtream" -> Xtream
            "emby" -> Emby
            "dropbox" -> Dropbox
            else -> M3U // Fallback
        }
    }
}

object DataSourceSerializer : KSerializer<DataSource> {
    override fun deserialize(decoder: Decoder): DataSource {
        return DataSource.of(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.example.primeflixlite.data.local.entity.DataSource",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: DataSource) {
        encoder.encodeString(value.value)
    }
}