package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.ViewModelFactory
import com.example.primeflixlite.ui.home.HomeScreen
import com.example.primeflixlite.ui.player.PlayerScreen
import com.example.primeflixlite.ui.player.PlayerViewModel
import com.example.primeflixlite.ui.search.SearchScreen
import com.example.primeflixlite.ui.search.SearchViewModel
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBlack
                ) {
                    // --- NAVIGATION STATE MACHINE ---
                    var showSplash by remember { mutableStateOf(true) }
                    var showSearch by remember { mutableStateOf(false) }
                    var currentChannel by remember { mutableStateOf<Channel?>(null) }

                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else if (currentChannel != null) {
                        // --- PLAYER SCREEN ---
                        val playerViewModel: PlayerViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        // Handle Back in Player
                        BackHandler {
                            currentChannel = null
                        }

                        PlayerScreen(
                            initialChannel = currentChannel!!,
                            viewModel = playerViewModel,
                            onBack = { currentChannel = null }
                        )

                    } else if (showSearch) {
                        // --- SEARCH SCREEN ---
                        val searchViewModel: SearchViewModel = viewModel(
                            factory = ViewModelFactory(appModule.repository)
                        )

                        // Handle Back in Search
                        BackHandler {
                            showSearch = false
                        }

                        SearchScreen(
                            viewModel = searchViewModel,
                            imageLoader = appModule.imageLoader,
                            onChannelClick = { channel ->
                                // Playing a result closes search and opens player
                                showSearch = false
                                currentChannel = channel
                            },
                            onBack = { showSearch = false }
                        )

                    } else {
                        // --- HOME SCREEN ---
                        HomeScreen(
                            viewModel = viewModel(
                                factory = ViewModelFactory(appModule.repository)
                            ),
                            imageLoader = appModule.imageLoader,
                            onChannelClick = { channel ->
                                currentChannel = channel
                            },
                            onSearchClick = {
                                showSearch = true
                            }
                        )
                    }
                }
            }
        }
    }
}