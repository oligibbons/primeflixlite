package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.ui.components.NotificationOverlay
import com.example.primeflixlite.ui.details.DetailsScreen
import com.example.primeflixlite.ui.details.DetailsViewModel
import com.example.primeflixlite.ui.guide.GuideScreen
import com.example.primeflixlite.ui.guide.GuideViewModel
import com.example.primeflixlite.ui.home.HomeScreen
import com.example.primeflixlite.ui.home.HomeViewModel
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
import com.example.primeflixlite.util.FeedbackManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inject ImageLoader directly via Hilt
    @Inject
    lateinit var imageLoader: ImageLoader

    // NEW: Inject FeedbackManager to display global notifications
    @Inject
    lateinit var feedbackManager: FeedbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PrimeFlixLiteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VoidBlack) {

                    // USE A BOX TO STACK NOTIFICATIONS ON TOP OF SCREENS
                    Box(modifier = Modifier.fillMaxSize()) {

                        // --- GLOBAL NAVIGATION STATE ---
                        var showSplash by remember { mutableStateOf(true) }

                        // Route Flags
                        var showSearch by remember { mutableStateOf(false) }
                        var showSettings by remember { mutableStateOf(false) }
                        var showAddPlaylist by remember { mutableStateOf(false) }
                        var showGuide by remember { mutableStateOf(false) }

                        // Content Objects
                        var playingChannel by remember { mutableStateOf<Channel?>(null) }
                        var detailsChannel by remember { mutableStateOf<Channel?>(null) }

                        // --- 1. SPLASH SCREEN ---
                        if (showSplash) {
                            SplashScreen(onSplashFinished = { showSplash = false })
                        }
                        // --- 2. PLAYER (Top Priority) ---
                        else if (playingChannel != null) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()

                            BackHandler { playingChannel = null }

                            PlayerScreen(
                                initialChannel = playingChannel!!,
                                viewModel = playerViewModel,
                                onBack = { playingChannel = null }
                            )
                        }
                        // --- 3. DETAILS SCREEN (Movies/Series) ---
                        else if (detailsChannel != null) {
                            val detailsViewModel: DetailsViewModel = hiltViewModel()

                            DetailsScreen(
                                channel = detailsChannel!!,
                                viewModel = detailsViewModel,
                                imageLoader = imageLoader,
                                onPlayClick = { url ->
                                    val toPlay = detailsChannel!!.copy(url = url)
                                    playingChannel = toPlay
                                },
                                onBack = { detailsChannel = null }
                            )
                        }
                        // --- 4. TV GUIDE ---
                        else if (showGuide) {
                            val guideViewModel: GuideViewModel = hiltViewModel()

                            BackHandler { showGuide = false }

                            GuideScreen(
                                viewModel = guideViewModel,
                                imageLoader = imageLoader,
                                onChannelClick = { channel ->
                                    playingChannel = channel
                                },
                                onBack = { showGuide = false }
                            )
                        }
                        // --- 5. SEARCH SCREEN ---
                        else if (showSearch) {
                            val searchViewModel: SearchViewModel = hiltViewModel()

                            BackHandler { showSearch = false }

                            SearchScreen(
                                viewModel = searchViewModel,
                                imageLoader = imageLoader,
                                onChannelClick = { channel ->
                                    if (channel.type == StreamType.LIVE.name) {
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
                            val settingsViewModel: SettingsViewModel = hiltViewModel()

                            BackHandler { showSettings = false }

                            SettingsScreen(
                                onBack = { showSettings = false },
                                viewModel = settingsViewModel
                            )
                        }
                        // --- 7. ADD PLAYLIST SCREEN ---
                        else if (showAddPlaylist) {
                            val addXtreamViewModel: AddXtreamViewModel = hiltViewModel()

                            BackHandler { showAddPlaylist = false }

                            AddXtreamScreen(
                                onPlaylistAdded = { showAddPlaylist = false },
                                viewModel = addXtreamViewModel
                            )
                        }
                        // --- 8. HOME SCREEN (Default) ---
                        else {
                            val homeViewModel: HomeViewModel = hiltViewModel()

                            HomeScreen(
                                viewModel = homeViewModel,
                                imageLoader = imageLoader,
                                onChannelClick = { channel ->
                                    if (channel.type == StreamType.LIVE.name) {
                                        playingChannel = channel
                                    } else {
                                        detailsChannel = channel
                                    }
                                },
                                onSearchClick = { showSearch = true },
                                onAddAccountClick = { showAddPlaylist = true },
                                onGuideClick = { showGuide = true },
                                onSettingsClick = { showSettings = true }
                            )
                        }

                        // --- 9. NOTIFICATION OVERLAY (Always on Top) ---
                        NotificationOverlay(feedbackManager)
                    }
                }
            }
        }
    }
}