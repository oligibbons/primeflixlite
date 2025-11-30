package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.components.NotificationOverlay
import com.example.primeflixlite.ui.details.DetailsScreen
import com.example.primeflixlite.ui.guide.GuideScreen
import com.example.primeflixlite.ui.guide.GuideViewModel
import com.example.primeflixlite.ui.home.HomeScreen
import com.example.primeflixlite.ui.home.HomeViewModel
import com.example.primeflixlite.ui.player.PlayerScreen
import com.example.primeflixlite.ui.player.PlayerViewModel
import com.example.primeflixlite.ui.search.SearchScreen
import com.example.primeflixlite.ui.search.SearchViewModel
import com.example.primeflixlite.ui.settings.AddXtreamScreen
import com.example.primeflixlite.ui.settings.SettingsScreen
import com.example.primeflixlite.ui.splash.SplashScreen
import com.example.primeflixlite.ui.theme.PrimeFlixLiteTheme
import com.example.primeflixlite.util.FeedbackManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var feedbackManager: FeedbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable system bars for immersive TV experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PrimeFlixLiteTheme {
                val navController = rememberNavController()

                // Global Notification Overlay
                NotificationOverlay(feedbackManager)

                NavHost(navController = navController, startDestination = "splash") {

                    composable("splash") {
                        SplashScreen(onSplashFinished = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }

                    composable("home") {
                        val viewModel = hiltViewModel<HomeViewModel>()
                        HomeScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onChannelClick = { channel ->
                                val encodedUrl = URLEncoder.encode(channel.url, StandardCharsets.UTF_8.toString())

                                // IMPORTANT: Since our Channel entity has a generated ID, we use it.
                                if (channel.type == "LIVE") {
                                    navController.navigate("player/${channel.id}")
                                } else {
                                    navController.navigate("details/${channel.id}")
                                }
                            },
                            onSearchClick = { navController.navigate("search") },
                            onAddAccountClick = { navController.navigate("add_xtream") },
                            onGuideClick = {
                                // Pass current group info?
                                val group = viewModel.uiState.value.selectedCategory
                                val encodedGroup = URLEncoder.encode(group, StandardCharsets.UTF_8.toString())
                                navController.navigate("guide/$encodedGroup")
                            },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }

                    composable("player/{channelId}") { backStackEntry ->
                        val viewModel = hiltViewModel<PlayerViewModel>()
                        // FIX: Use toLongOrNull() because Channel.id is Long
                        val channelId = backStackEntry.arguments?.getString("channelId")?.toLongOrNull()

                        // Using placeholder for initial render
                        PlayerScreen(
                            initialChannel = Channel(id = channelId ?: 0, title = "Loading...", url = "", playlistUrl = "", type = "", group = ""),
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )

                        // Trigger actual load in VM
                        LaunchedEffect(channelId) {
                            if (channelId != null) viewModel.loadChannelById(channelId)
                        }
                    }

                    composable("details/{channelId}") { backStackEntry ->
                        val viewModel = hiltViewModel<com.example.primeflixlite.ui.details.DetailsViewModel>()
                        // FIX: Use toLongOrNull() because Channel.id is Long
                        val channelId = backStackEntry.arguments?.getString("channelId")?.toLongOrNull()

                        val placeholder = Channel(id = channelId ?: 0, title = "Loading...", url = "", playlistUrl = "", type = "", group = "")

                        DetailsScreen(
                            channel = placeholder, // VM will replace this
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onPlayClick = { url ->
                                // Play VOD directly
                                val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                                navController.navigate("player_vod/$encoded")
                            },
                            onBack = { navController.popBackStack() }
                        )

                        LaunchedEffect(channelId) {
                            if (channelId != null) viewModel.loadChannelById(channelId)
                        }
                    }

                    // Special Route for VOD URL playback
                    composable("player_vod/{videoUrl}") { backStackEntry ->
                        val url = backStackEntry.arguments?.getString("videoUrl") ?: ""
                        val viewModel = hiltViewModel<PlayerViewModel>()

                        PlayerScreen(
                            initialChannel = Channel(title = "VOD", url = url, playlistUrl = "", type = "MOVIE", group = ""),
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("search") {
                        val viewModel = hiltViewModel<SearchViewModel>()
                        SearchScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onChannelClick = { channel ->
                                if (channel.type == "LIVE") navController.navigate("player/${channel.id}")
                                else navController.navigate("details/${channel.id}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("guide/{group}") { backStackEntry ->
                        val group = backStackEntry.arguments?.getString("group") ?: "All"
                        val viewModel = hiltViewModel<GuideViewModel>()
                        val homeVM = hiltViewModel<HomeViewModel>() // Need playlist context

                        // Load Guide
                        LaunchedEffect(group) {
                            val playlist = homeVM.uiState.value.selectedPlaylist
                            if (playlist != null) {
                                viewModel.loadGuide(playlist, group)
                            }
                        }

                        GuideScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onChannelClick = { channel -> navController.navigate("player/${channel.id}") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("add_xtream") {
                        AddXtreamScreen(
                            onPlaylistAdded = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}