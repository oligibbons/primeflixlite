package com.example.primeflixlite.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.ui.components.LoadingOverlay
import com.example.primeflixlite.ui.components.NeonFocusCard
import com.example.primeflixlite.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    imageLoader: coil.ImageLoader,
    onChannelClick: (Channel) -> Unit,
    onSearchClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onGuideClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Back Press
    BackHandler(enabled = uiState.selectedPlaylist != null) {
        viewModel.backToPlaylists()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        if (uiState.selectedPlaylist == null) {
            // --- PLAYLIST SELECTION MODE (Simple Neon Grid) ---
            PlaylistSelectionScreen(
                uiState = uiState,
                onAddAccountClick = onAddAccountClick,
                onPlaylistClick = { viewModel.selectPlaylist(it) }
            )
        } else {
            // --- MAIN DASHBOARD (Cinematic Split Layout) ---
            DashboardScreen(
                uiState = uiState,
                imageLoader = imageLoader,
                viewModel = viewModel,
                onChannelClick = onChannelClick,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }

        // --- GLOBAL LOADING OVERLAY ---
        // Sits on top of everything, blocking input when active
        if (uiState.loadingMessage != null) {
            LoadingOverlay(message = uiState.loadingMessage!!)
        }
    }
}

@Composable
fun DashboardScreen(
    uiState: HomeState,
    imageLoader: coil.ImageLoader,
    viewModel: HomeViewModel,
    onChannelClick: (Channel) -> Unit,
    onTabSelected: (StreamType) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // 1. TOP SECTION: SPOTLIGHT HERO (45% Height)
        // Shows details for the currently focused item
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxWidth()
        ) {
            SpotlightHero(
                channel = uiState.spotlightChannel,
                imageLoader = imageLoader,
                onPlayClick = { uiState.spotlightChannel?.let { onChannelClick(it) } }
            )

            // Tab Bar Overlay (Top Right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StreamType.values().forEach { type ->
                    val isSelected = uiState.selectedTab == type
                    TextButton(onClick = { onTabSelected(type) }) {
                        Text(
                            text = type.name,
                            color = if (isSelected) NeonBlue else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // 2. BOTTOM SECTION: CONTENT GRID (55% Height)
        // The scrollable area
        Box(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
                .background(
                    // Gradient fade from the hero image down to solid black
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, VoidBlack, VoidBlack),
                        startY = 0f
                    )
                )
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Main Content
                items(uiState.displayedChannels, key = { it.channel.id }) { item ->
                    NeonFocusCard(
                        channel = item.channel,
                        imageLoader = imageLoader,
                        onClick = { onChannelClick(item.channel) },
                        modifier = Modifier
                            // CRITICAL: Update spotlight on focus
                            .onFocusChanged {
                                if (it.isFocused) viewModel.updateSpotlight(item.channel)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun SpotlightHero(
    channel: Channel?,
    imageLoader: coil.ImageLoader,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current

    // Background Image Transition
    // We use AnimatedContent for smooth crossfades when focus changes
    AnimatedContent(
        targetState = channel,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "HeroBackground",
        modifier = Modifier.fillMaxSize()
    ) { currentChannel ->
        if (currentChannel != null) {
            val backdropUrl = currentChannel.cover // Ideally use a backdrop field if available
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .size(1280, 720) // Limit size for memory
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.4f) // Dimmed to let text pop
            )
        } else {
            Box(Modifier.fillMaxSize().background(VoidBlack))
        }
    }

    // Gradient Overlay for Text Readability
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(VoidBlack, Color.Transparent),
                    startX = 0f,
                    endX = 1000f
                )
            )
    )

    // Text & Actions
    if (channel != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = channel.title,
                style = MaterialTheme.typography.displayMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "Play" Button
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("WATCH NOW", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlaylistSelectionScreen(
    uiState: HomeState,
    onAddAccountClick: () -> Unit,
    onPlaylistClick: (com.example.primeflixlite.data.local.entity.Playlist) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WHO IS WATCHING?",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (uiState.playlists.isEmpty()) {
            Button(
                onClick = onAddAccountClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
            ) {
                Text("Connect Xtream Account", color = Color.Black)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 profiles per row
                contentPadding = PaddingValues(horizontal = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                items(uiState.playlists) { playlist ->
                    // Profile Card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onPlaylistClick(playlist) }
                            .padding(16.dp)
                    ) {
                        // Avatar Placeholder
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(NeonBlueDim, MaterialTheme.shapes.circle),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                playlist.title.take(1).uppercase(),
                                fontSize = 48.sp,
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(playlist.title, style = MaterialTheme.typography.titleMedium, color = White)
                    }
                }
            }
        }
    }
}

// Extension to help focus detection
fun Modifier.onFocusChanged(onFocus: (androidx.compose.ui.focus.FocusState) -> Unit) =
    androidx.compose.ui.focus.onFocusChanged(onFocus)