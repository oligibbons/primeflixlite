package com.example.primeflixlite.di

import android.content.Context
import androidx.room.Room
import com.example.primeflixlite.data.local.PrimeFlixDatabase
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.MediaMetadataDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.dao.ProgrammeDao
import com.example.primeflixlite.data.local.dao.WatchProgressDao
import com.example.primeflixlite.data.parser.m3u.M3UParser
import com.example.primeflixlite.data.parser.m3u.M3UParserImpl
import com.example.primeflixlite.data.parser.xmltv.XmltvParser
import com.example.primeflixlite.data.parser.xtream.XtreamParser
import com.example.primeflixlite.data.parser.xtream.XtreamParserImpl
import com.example.primeflixlite.data.remote.TmdbService
import com.example.primeflixlite.util.FeedbackManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrimeFlixDatabase {
        return Room.databaseBuilder(
            context,
            PrimeFlixDatabase::class.java,
            "primeflix_db"
        )
            .fallbackToDestructiveMigration() // Useful during dev/schema changes
            .build()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(db: PrimeFlixDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideChannelDao(db: PrimeFlixDatabase): ChannelDao = db.channelDao()

    @Provides
    @Singleton
    fun provideProgrammeDao(db: PrimeFlixDatabase): ProgrammeDao = db.programmeDao()

    @Provides
    @Singleton
    fun provideWatchProgressDao(db: PrimeFlixDatabase): WatchProgressDao = db.watchProgressDao()

    @Provides
    @Singleton
    fun provideMediaMetadataDao(db: PrimeFlixDatabase): MediaMetadataDao = db.mediaMetadataDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideFeedbackManager(@ApplicationContext context: Context): FeedbackManager {
        return FeedbackManager(context)
    }

    @Provides
    @Singleton
    fun provideXtreamParser(client: OkHttpClient, feedback: FeedbackManager): XtreamParser {
        return XtreamParserImpl(client, feedback)
    }

    @Provides
    @Singleton
    fun provideM3UParser(): M3UParser = M3UParserImpl()

    @Provides
    @Singleton
    fun provideXmltvParser(client: OkHttpClient): XmltvParser = XmltvParser(client)

    @Provides
    @Singleton
    fun provideTmdbService(client: OkHttpClient): TmdbService {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client) // Reuse OkHttp for pooling
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(TmdbService::class.java)
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}