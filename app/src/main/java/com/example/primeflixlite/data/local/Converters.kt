package com.example.primeflixlite.data.local

import androidx.room.TypeConverter
import com.example.primeflixlite.data.local.entity.DataSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    // JSON parser for converting Lists to Strings
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromDataSource(value: String): DataSource {
        return DataSource.of(value)
    }

    @TypeConverter
    fun toDataSource(dataSource: DataSource): String {
        return dataSource.value
    }
}