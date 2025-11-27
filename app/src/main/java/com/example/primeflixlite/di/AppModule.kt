package com.example.primeflixlite.di

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.primeflixlite.data.local.PrimeFlixDatabase
import com.example.primeflixlite.data.parser.m3u.M3UParserImpl
import com.example.primeflixlite.data.parser.xmltv.XmltvParser
import com.example.primeflixlite.data.parser.xtream.XtreamParserImpl
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppModule(private val context: Context) {

    // 1. Network Client
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // 2. Database
    private val database: PrimeFlixDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            PrimeFlixDatabase::class.java,
            "primeflix-lite.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 3. Parsers
    private val xtreamParser by lazy { XtreamParserImpl(okHttpClient) }
    private val m3uParser by lazy { M3UParserImpl() }
    private val xmltvParser by lazy { XmltvParser(okHttpClient) }

    // 4. Image Loader (50MB Cap)
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15)
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(true)
            .build()
    }

    // 5. Repository
    val repository: PrimeFlixRepository by lazy {
        PrimeFlixRepository(
            playlistDao = database.playlistDao(),
            channelDao = database.channelDao(),
            programmeDao = database.programmeDao(),
            watchProgressDao = database.watchProgressDao(), // NEW: Pass the DAO
            xtreamParser = xtreamParser,
            m3uParser = m3uParser,
            xmltvParser = xmltvParser,
            okHttpClient = okHttpClient
        )
    }
}