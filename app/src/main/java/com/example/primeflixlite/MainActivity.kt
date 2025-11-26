package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
import com.example.primeflixlite.ui.splash.SplashScreen
import com.example.primeflixlite.ui.theme.PrimeFlixLiteTheme
import com.example.primeflixlite.ui.theme.VoidBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as PrimeFlixApplication).container

        setContent {
            PrimeFlixLiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBlack
                ) {
                    // Navigation States: Splash -> Home <-> Player
                    var showSplash by remember { mutableStateOf(true) }
                    var currentChannel by remember { mutableStateOf<Channel?>(null) }

                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        if (currentChannel == null) {
                            HomeScreen(
                                viewModel = viewModel(
                                    factory = ViewModelFactory(appContainer.repository)
                                ),
                                onChannelClick = { channel ->
                                    currentChannel = channel
                                }
                            )
                        } else {
                            PlayerScreen(
                                url = currentChannel!!.url,
                                onBack = {
                                    currentChannel = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}