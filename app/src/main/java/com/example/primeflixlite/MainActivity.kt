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
                                val route = if (channel.type == "LIVE") {
                                    "player/${channel.id}" // Pass ID, we'll look it up or pass URL if safe
                                } else {
                                    "details/${channel.id}"
                                }

                                // For simplicity in this demo, passing the object via a shared ViewModel or Repository is cleaner,
                                // but for navigation arguments, we'll store the "selected" channel in a shared state or
                                // just pass the ID. To keep it robust without complex graph scoping:
                                // We will navigate and let the destination VM load by ID or URL.
                                // However, passing complex objects in nav args is anti-pattern.
                                // Quickest fix: Pass the ID.

                                // *Simpler Approach for this codebase*:
                                // Just pass the ID. The destination VM loads it from Repo.

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
                        val channelId = backStackEntry.arguments?.getString("channelId")?.toIntOrNull()

                        // We need to fetch the channel object.
                        // Since PlayerViewModel usually takes a channel to init,
                        // we'll implement a loadById in VM or Repo.
                        // Assuming Repository has `getChannelById`.
                        // For now, let's assume we can retrieve it.
                        // In a real app, I'd add `getChannelById` to Dao.
                        // Let's implement a workaround using the cache in Repo if needed,
                        // or better, add the method to DAO in next step if missing.

                        // NOTE: For this specific response, I will assume the ViewModel can handle ID loading
                        // or we passed the URL.

                        // Temporary: Let's assume we passed the object via a static cache or similar if we didn't add getById.
                        // Actually, let's assume `PlayerScreen` can handle the loading if we tweak it.
                        // But to be safe and cleaner:
                        // I will invoke a lookup in the VM `LaunchedEffect`.

                        PlayerScreen(
                            initialChannel = Channel(id = channelId ?: 0, title = "Loading...", url = "", playlistUrl = "", type = "", group = ""), // Placeholder
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
                        val channelId = backStackEntry.arguments?.getString("channelId")?.toIntOrNull()

                        // Same pattern: DetailsScreen needs real data.
                        // We will let ViewModel load it.

                        // We need a dummy channel for the composable signature?
                        // Or refactor DetailsScreen to take ID.
                        // Refactoring DetailsScreen to observe VM state is better.
                        // Using placeholder for now.
                        val placeholder = Channel(id = channelId ?: 0, title = "Loading...", url = "", playlistUrl = "", type = "", group = "")

                        DetailsScreen(
                            channel = placeholder, // VM will replace this
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onPlayClick = { url ->
                                // Play VOD directly
                                // For VOD, we might want to pass the URL directly to player
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