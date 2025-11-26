package com.example.primeflixlite.data.local

import androidx.room.TypeConverter
import com.example.primeflixlite.data.local.entity.DataSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    @TypeConverter
    fun fromDataSource(source: DataSource): String {
        return source.value
    }

    @TypeConverter
    fun toDataSource(value: String): DataSource {
        return DataSource.of(value)
    }
}