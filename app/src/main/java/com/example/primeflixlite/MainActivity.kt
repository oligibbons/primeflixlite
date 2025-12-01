package com.example.primeflixlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.ImageLoader
import com.example.primeflixlite.data.local.entity.Channel
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
import com.example.primeflixlite.ui.settings.SettingsScreen
import com.example.primeflixlite.ui.splash.SplashScreen
import com.example.primeflixlite.ui.theme.PrimeFlixLiteTheme
import com.example.primeflixlite.util.FeedbackManager
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

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

                // Global Notification Overlay (Toasts/Errors)
                NotificationOverlay(feedbackManager)

                NavHost(navController = navController, startDestination = "splash") {

                    // 1. SPLASH
                    composable("splash") {
                        SplashScreen(onSplashFinished = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }

                    // 2. HOME DASHBOARD
                    composable("home") {
                        val viewModel = hiltViewModel<HomeViewModel>()
                        HomeScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader, // FIXED: Passed injected imageLoader
                            onChannelClick = { channel ->
                                if (channel.type == "LIVE") {
                                    // Navigate to Player with Encoded URL
                                    val encodedUrl = URLEncoder.encode(channel.url, StandardCharsets.UTF_8.toString())
                                    navController.navigate("player/$encodedUrl")
                                } else {
                                    // Navigate to Details by ID
                                    navController.navigate("details/${channel.id}")
                                }
                            },
                            onSearchClick = { navController.navigate("search") },
                            onAddAccountClick = { navController.navigate("add_xtream") },
                            onGuideClick = {
                                // FIXED: Added parameter. Navigates to Guide with default "All" group
                                navController.navigate("guide/All")
                            },
                            onSettingsClick = { // FIXED: Renamed from onNavigateToSettings
                                navController.navigate("settings")
                            }
                        )
                    }

                    // 3. PLAYER SCREEN
                    // Accepts an encoded URL string
                    composable(
                        route = "player/{videoUrl}",
                        arguments = listOf(navArgument("videoUrl") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val viewModel = hiltViewModel<PlayerViewModel>()
                        val encodedUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                        val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

                        PlayerScreen(
                            url = decodedUrl,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 4. DETAILS SCREEN (VOD/Series)
                    composable(
                        route = "details/{channelId}",
                        arguments = listOf(navArgument("channelId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val viewModel = hiltViewModel<DetailsViewModel>()
                        val channelId = backStackEntry.arguments?.getLong("channelId") ?: 0L

                        // We create a temporary placeholder while VM loads real data
                        val placeholder = Channel(id = channelId, title = "Loading...", url = "", playlistUrl = "", type = "MOVIE", group = "")

                        DetailsScreen(
                            channel = placeholder,
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onPlayClick = { url ->
                                val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                                navController.navigate("player/$encoded")
                            },
                            onBack = { navController.popBackStack() }
                        )

                        LaunchedEffect(channelId) {
                            viewModel.loadChannelById(channelId)
                        }
                    }

                    // 5. SEARCH
                    composable("search") {
                        val viewModel = hiltViewModel<SearchViewModel>()
                        SearchScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onChannelClick = { channel ->
                                if (channel.type == "LIVE") {
                                    val encoded = URLEncoder.encode(channel.url, StandardCharsets.UTF_8.toString())
                                    navController.navigate("player/$encoded")
                                } else {
                                    navController.navigate("details/${channel.id}")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 6. GUIDE (Optional - kept for future linking)
                    composable("guide/{group}") { backStackEntry ->
                        val groupEncoded = backStackEntry.arguments?.getString("group") ?: "All"
                        val group = URLDecoder.decode(groupEncoded, StandardCharsets.UTF_8.toString())

                        val viewModel = hiltViewModel<GuideViewModel>()
                        val homeVM = hiltViewModel<HomeViewModel>()

                        LaunchedEffect(group) {
                            val playlist = homeVM.uiState.value.selectedPlaylist
                            if (playlist != null) {
                                viewModel.loadGuide(playlist, group)
                            }
                        }

                        GuideScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onChannelClick = { channel ->
                                val encoded = URLEncoder.encode(channel.url, StandardCharsets.UTF_8.toString())
                                navController.navigate("player/$encoded")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 7. SETTINGS & SETUP
                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }

                    composable("add_xtream") {
                        AddXtreamScreen(onPlaylistAdded = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}