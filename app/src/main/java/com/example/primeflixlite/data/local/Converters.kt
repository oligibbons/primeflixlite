package com.example.primeflixlite.data.local

import androidx.room.TypeConverter
import com.example.primeflixlite.data.local.entity.StreamType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {

    // --- StreamType Enum ---
    @TypeConverter
    fun fromStreamType(value: StreamType): String = value.name

    @TypeConverter
    fun toStreamType(value: String): StreamType = try {
        StreamType.valueOf(value)
    } catch (e: Exception) {
        StreamType.LIVE // Fallback
    }

    // --- Date/Long ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // --- List<String> (for generic lists) ---
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}