package com.example.primeflixlite.data.local.entity

import androidx.compose.runtime.Immutable
import com.example.primeflixlite.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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