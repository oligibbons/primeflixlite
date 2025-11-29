package com.example.primeflixlite.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.primeflixlite.data.local.PrimeFlixDatabase
import com.example.primeflixlite.data.local.dao.ChannelDao
import com.example.primeflixlite.data.local.dao.PlaylistDao
import com.example.primeflixlite.data.local.dao.ProgrammeDao
import com.example.primeflixlite.data.local.dao.WatchProgressDao
import com.example.primeflixlite.data.parser.m3u.M3UParser
import com.example.primeflixlite.data.parser.m3u.M3UParserImpl
import com.example.primeflixlite.data.parser.xmltv.XmltvParser
import com.example.primeflixlite.data.parser.xtream.XtreamParser
import com.example.primeflixlite.data.parser.xtream.XtreamParserImpl
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
            .fallbackToDestructiveMigration() // Safety for dev
            .build()
    }

    @Provides
    fun providePlaylistDao(db: PrimeFlixDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideChannelDao(db: PrimeFlixDatabase): ChannelDao = db.channelDao()

    @Provides
    fun provideProgrammeDao(db: PrimeFlixDatabase): ProgrammeDao = db.programmeDao()

    @Provides
    fun provideWatchProgressDao(db: PrimeFlixDatabase): WatchProgressDao = db.watchProgressDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build() // Add logging interceptor here if debugging needed
    }

    @Provides
    @Singleton
    fun provideXtreamParser(client: OkHttpClient): XtreamParser {
        return XtreamParserImpl(client)
    }

    @Provides
    @Singleton
    fun provideM3UParser(): M3UParser {
        return M3UParserImpl()
    }

    @Provides
    @Singleton
    fun provideXmltvParser(client: OkHttpClient): XmltvParser {
        // Assuming XmltvParser implementation handles the client or fetching internally
        // If your XmltvParser is just a class, instantiate it here.
        // Based on previous context, likely:
        return XmltvParser(client)
    }

    @Provides
    @Singleton
    fun provideRepository(
        playlistDao: PlaylistDao,
        channelDao: ChannelDao,
        programmeDao: ProgrammeDao,
        watchProgressDao: WatchProgressDao,
        xtreamParser: XtreamParser,
        m3uParser: M3UParser,
        xmltvParser: XmltvParser,
        okHttpClient: OkHttpClient
    ): PrimeFlixRepository {
        return PrimeFlixRepository(
            playlistDao,
            channelDao,
            programmeDao,
            watchProgressDao,
            xtreamParser,
            m3uParser,
            xmltvParser,
            okHttpClient
        )
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, client: OkHttpClient): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}