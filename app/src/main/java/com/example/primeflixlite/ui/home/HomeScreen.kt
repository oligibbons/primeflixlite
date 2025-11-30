package com.example.primeflixlite.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.ui.theme.BurntYellow
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.util.TimeUtils

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
    val initialFocusRequester = remember { FocusRequester() }
    var hasFocused by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !hasFocused) {
            if (uiState.categories.isNotEmpty()) {
                initialFocusRequester.requestFocus()
                hasFocused = true
            }
        }
    }

    BackHandler(enabled = uiState.selectedPlaylist != null) {
        viewModel.backToPlaylists()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp)
    ) {
        if (uiState.selectedPlaylist == null) {
            // --- PLAYLIST SELECTION MODE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Profile", style = MaterialTheme.typography.headlineMedium, color = Color.White)

                Button(
                    onClick = onAddAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Connect Account", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(top = 24.dp)) {
                items(uiState.playlists) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        isSelected = false,
                        onClick = { viewModel.selectPlaylist(playlist) }
                    )
                }
            }

            if (uiState.playlists.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Welcome to PrimeFlix+",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Connect your Xtream Codes account to start.",
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onAddAccountClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            modifier = Modifier.scale(1.2f)
                        ) {
                            Text("Connect Now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

        } else {
            // --- CONTENT DASHBOARD MODE ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Tabs + Guide Button
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    StreamType.values().forEach { type ->
                        NavTab(
                            title = type.name,
                            isSelected = uiState.selectedTab == type,
                            onClick = { viewModel.selectTab(type) }
                        )
                    }

                    if (uiState.selectedTab == StreamType.LIVE) {
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = onGuideClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BurntYellow),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            // FIXED: Use AutoMirrored List Icon
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("TV GUIDE", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Right Side: Search + Settings Icons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    var searchFocused by remember { mutableStateOf(false) }
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if(searchFocused) NeonBlue else Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(if(searchFocused) 1.2f else 1f)
                            .clickable { onSearchClick() }
                            .onFocusChanged { searchFocused = it.isFocused }
                            .focusable()
                    )

                    var settingsFocused by remember { mutableStateOf(false) }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if(settingsFocused) NeonBlue else Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(if(settingsFocused) 1.2f else 1f)
                            .clickable { onSettingsClick() }
                            .onFocusChanged { settingsFocused = it.isFocused }
                            .focusable()
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    if (uiState.favorites.isNotEmpty()) {
                        ContinueWatchingLane(
                            title = "My List",
                            items = uiState.favorites,
                            imageLoader = imageLoader,
                            onItemClick = { url ->
                                val channel = uiState.favorites.find { it.url == url }
                                if (channel != null) onChannelClick(channel)
                            }
                        )
                    }

                    if (uiState.continueWatching.isNotEmpty()) {
                        ContinueWatchingLane(
                            title = "Continue Watching",
                            items = uiState.continueWatching,
                            imageLoader = imageLoader,
                            onItemClick = { url ->
                                val channel = uiState.displayedChannels.find { it.channel.url == url }?.channel
                                    ?: uiState.continueWatching.find { it.url == url }
                                if (channel != null) onChannelClick(channel)
                            }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 24.dp).focusRequester(initialFocusRequester)
                    ) {
                        items(uiState.categories) { category ->
                            CategoryPill(
                                title = category,
                                isSelected = category == uiState.selectedCategory,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }

                    val isVod = uiState.selectedTab == StreamType.MOVIE || uiState.selectedTab == StreamType.SERIES
                    val minSize = if (isVod) 140.dp else 180.dp

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = minSize),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(uiState.displayedChannels, key = { it.channel.id }) { item ->
                            if (isVod) {
                                MovieCard(
                                    channel = item.channel,
                                    imageLoader = imageLoader,
                                    onClick = { onChannelClick(item.channel) }
                                )
                            } else {
                                ChannelCard(
                                    channelWithProgram = item,
                                    imageLoader = imageLoader,
                                    onClick = { onChannelClick(item.channel) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val color = if (isSelected) NeonBlue else if (isFocused) Color.White else Color.Gray
    val fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal

    Text(
        text = title,
        color = color,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(8.dp)
    )
}

@Composable
fun PlaylistRow(playlist: Playlist, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NeonBlue else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = playlist.title, color = if (isFocused) Color.White else Color.Gray)
    }
}

@Composable
fun ChannelCard(channelWithProgram: ChannelWithProgram, imageLoader: coil.ImageLoader, onClick: () -> Unit) {
    val channel = channelWithProgram.channel
    val program = channelWithProgram.program
    var isFocused by remember { mutableStateOf(false) }
    val scale = if (isFocused) 1.05f else 1f
    val borderColor = if (isFocused) NeonBlue else Color.Transparent

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Box(modifier = Modifier.aspectRatio(16f / 9f).background(Color.Black)) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(channel.cover).crossfade(true).size(320, 180).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = channel.title.take(1), color = Color.DarkGray)
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = channel.title, color = if (isFocused) Color.White else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (program != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = program.title, style = MaterialTheme.typography.bodySmall, color = NeonBlue, maxLines = 1)

                // FIXED: Use lambda syntax for progress
                LinearProgressIndicator(
                    progress = { TimeUtils.getProgress(program.start, program.end) },
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top=4.dp),
                    color = NeonBlue,
                    trackColor = Color.DarkGray,
                )
            }
        }
    }
}

@Composable
fun CategoryPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bg = if (isSelected) NeonBlue else if (isFocused) Color.White else Color(0xFF333333)
    val text = if (isSelected || isFocused) Color.Black else Color.White
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = title, color = text, fontWeight = FontWeight.SemiBold)
    }
}