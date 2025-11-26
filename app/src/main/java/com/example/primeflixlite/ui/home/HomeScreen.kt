package com.example.primeflixlite.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.R
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.NeonBlueDim
import com.example.primeflixlite.ui.theme.VoidBlack

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChannelClick: (Channel) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val channels by viewModel.currentChannels.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    LaunchedEffect(playlists) {
        if (selectedPlaylist == null && playlists.isNotEmpty()) {
            viewModel.selectPlaylist(playlists.first())
        }
    }

    // Main Layout with Vignette Background for "Cinema" feel
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF121212), VoidBlack),
                    radius = 1200f
                )
            )
    ) {
        // --- LEFT PANE: SIDEBAR ---
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF0A0A0A)) // Slightly lighter than void
                .padding(24.dp)
        ) {
            // BRANDING
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Using the White logo, tinted or just raw if it looks good against dark.
                // Assuming "logo_spotlight" is too big, using text + icon or just text.
                // Let's use the Spotlight logo cropped or just Text for cleanness.
                // Text is usually sharper on low-res projectors.
                Text(
                    text = "PRIMEFLIX",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = NeonBlue,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (playlists.isEmpty()) {
                Button(
                    onClick = { viewModel.addSamplePlaylist() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlueDim)
                ) {
                    Text("Load Demo")
                }
            } else {
                Text(
                    text = "PLAYLISTS",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlists) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            isSelected = playlist == selectedPlaylist,
                            onClick = { viewModel.selectPlaylist(playlist) }
                        )
                    }
                }
            }
        }

        // --- RIGHT PANE: CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
        ) {
            if (selectedPlaylist != null) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPlaylist?.title ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.syncCurrentPlaylist() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = NeonBlue
                        ),
                        border = BorderStroke(1.dp, NeonBlueDim)
                    ) {
                        Text("SYNC")
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // GRID
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(channels) { channel ->
                        ChannelCard(channel = channel, onClick = { onChannelClick(channel) })
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a playlist to begin", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    // Logic: If selected, it's Blue. If focused, it's White bg (high contrast) or Brighter Blue.
    // TV UX Rule: Focus state must be Obvious.

    val backgroundColor = when {
        isFocused -> Color.White
        isSelected -> NeonBlue.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> Color.Black
        isSelected -> NeonBlue
        else -> Color.LightGray
    }

    val fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Indicator Dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) Color.Black else NeonBlue)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = playlist.title,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = fontWeight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    // Glow Effect for TV Focus
    val glowColor = if (isFocused) NeonBlue else Color.Transparent
    val scale = if (isFocused) 1.1f else 1.0f
    val elevation = if (isFocused) 8.dp else 0.dp

    Card(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .shadow(elevation, RoundedCornerShape(12.dp), spotColor = glowColor),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Stylish Fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF333333), Color(0xFF111111))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.title.take(1).uppercase(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                        color = Color.DarkGray
                    )
                }
            }

            // Text Protection Gradient (Bottom Up)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
            )

            // Title
            Text(
                text = channel.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isFocused) NeonBlue else Color.White, // Text turns blue on focus
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}