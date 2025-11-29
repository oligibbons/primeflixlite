package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.ui.ViewModelFactory
import com.example.primeflixlite.ui.details.DetailsScreen
import com.example.primeflixlite.ui.details.DetailsViewModel
import com.example.primeflixlite.ui.guide.GuideScreen
import com.example.primeflixlite.ui.guide.GuideViewModel
import com.example.primeflixlite.ui.home.HomeScreen
import com.example.primeflixlite.ui.player.PlayerScreen
import com.example.primeflixlite.ui.player.PlayerViewModel
import com.example.primeflixlite.ui.search.SearchScreen
import com.example.primeflixlite.ui.search.SearchViewModel
import com.example.primeflixlite.ui.settings.AddXtreamScreen
import com.example.primeflixlite.ui.settings.AddXtreamViewModel
import com.example.primeflixlite.ui.settings.SettingsScreen
import com.example.primeflixlite.ui.settings.SettingsViewModel
import com.example.primeflixlite.ui.splash.SplashScreen
import com.example.primeflixlite.ui.theme.PrimeFlixLiteTheme
import com.example.primeflixlite.ui.theme.VoidBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appModule = (application as PrimeFlixApplication).appModule

        setContent {
            PrimeFlixLiteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VoidBlack) {

                    // --- GLOBAL NAVIGATION STATE ---
                    var showSplash by remember { mutableStateOf(true) }

                    // Route Flags
                    var showSearch by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }
                    var showAddPlaylist by remember { mutableStateOf(false) }
                    var showGuide by remember { mutableStateOf(false) } // NEW: Guide Flag

                    // Content Objects
                    var playingChannel by remember { mutableStateOf<Channel?>(null) }
                    var detailsChannel by remember { mutableStateOf<Channel?>(null) }

                    // --- 1. SPLASH SCREEN ---
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    }
                    // --- 2. PLAYER (Top Priority) ---
                    else if (playingChannel != null) {
                        val playerViewModel: PlayerViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        BackHandler { playingChannel = null }

                        PlayerScreen(
                            initialChannel = playingChannel!!,
                            viewModel = playerViewModel,
                            onBack = { playingChannel = null }
                        )
                    }
                    // --- 3. DETAILS SCREEN (Movies/Series) ---
                    else if (detailsChannel != null) {
                        val detailsViewModel: DetailsViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        DetailsScreen(
                            channel = detailsChannel!!,
                            viewModel = detailsViewModel,
                            imageLoader = appModule.imageLoader,
                            onPlayClick = { url ->
                                val toPlay = detailsChannel!!.copy(url = url)
                                playingChannel = toPlay
                            },
                            onBack = { detailsChannel = null }
                        )
                    }
                    // --- 4. TV GUIDE (NEW) ---
                    else if (showGuide) {
                        val guideViewModel: GuideViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        BackHandler { showGuide = false }

                        GuideScreen(
                            viewModel = guideViewModel,
                            imageLoader = appModule.imageLoader,
                            onChannelClick = { channel ->
                                // Clicking a channel in guide plays it immediately
                                playingChannel = channel
                            },
                            onBack = { showGuide = false }
                        )
                    }
                    // --- 5. SEARCH SCREEN ---
                    else if (showSearch) {
                        val searchViewModel: SearchViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        BackHandler { showSearch = false }

                        SearchScreen(
                            viewModel = searchViewModel,
                            imageLoader = appModule.imageLoader,
                            onChannelClick = { channel ->
                                if (channel.type == StreamType.LIVE) {
                                    playingChannel = channel
                                } else {
                                    detailsChannel = channel
                                }
                                showSearch = false
                            },
                            onBack = { showSearch = false }
                        )
                    }
                    // --- 6. SETTINGS SCREEN ---
                    else if (showSettings) {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        BackHandler { showSettings = false }

                        SettingsScreen(
                            onBack = { showSettings = false },
                            viewModel = settingsViewModel
                        )
                    }
                    // --- 7. ADD PLAYLIST SCREEN ---
                    else if (showAddPlaylist) {
                        val addXtreamViewModel: AddXtreamViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        BackHandler { showAddPlaylist = false }

                        AddXtreamScreen(
                            onPlaylistAdded = { showAddPlaylist = false },
                            viewModel = addXtreamViewModel
                        )
                    }
                    // --- 8. HOME SCREEN (Default) ---
                    else {
                        HomeScreen(
                            viewModel = viewModel(factory = ViewModelFactory(appModule.repository)),
                            imageLoader = appModule.imageLoader,
                            onChannelClick = { channel ->
                                if (channel.type == StreamType.LIVE) {
                                    playingChannel = channel
                                } else {
                                    detailsChannel = channel
                                }
                            },
                            onSearchClick = { showSearch = true },
                            onAddAccountClick = { showAddPlaylist = true },
                            onGuideClick = { showGuide = true }, // Trigger Guide
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}