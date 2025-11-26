package com.example.primeflixlite.di

import android.content.Context
import androidx.room.Room
import com.example.primeflixlite.data.local.PrimeFlixDatabase
import com.example.primeflixlite.data.parser.m3u.M3UParserImpl
import com.example.primeflixlite.data.parser.xtream.XtreamParserImpl
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Manual Dependency Injection Container
class AppModule(private val context: Context) {

    // 1. Network Client (The Browser)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // 2. Database (The Memory)
    private val database: PrimeFlixDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            PrimeFlixDatabase::class.java,
            "primeflix-lite.db"
        )
            .fallbackToDestructiveMigration() // Wipes DB if you change the schema (good for dev)
            .build()
    }

    // 3. Parsers (The Brains)
    private val xtreamParser by lazy { XtreamParserImpl(okHttpClient) }
    private val m3uParser by lazy { M3UParserImpl() }

    // 4. Repository (The Manager)
    val repository: PrimeFlixRepository by lazy {
        PrimeFlixRepository(
            playlistDao = database.playlistDao(),
            channelDao = database.channelDao(),
            xtreamParser = xtreamParser
        )
    }
}