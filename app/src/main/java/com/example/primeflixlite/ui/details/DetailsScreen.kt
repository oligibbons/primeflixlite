package com.example.primeflixlite.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack

@Composable
fun DetailsScreen(
    channel: Channel, // Placeholder passed from nav
    viewModel: DetailsViewModel,
    imageLoader: coil.ImageLoader,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playButtonFocus = remember { FocusRequester() }

    // Auto-focus Play button when data loads
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            playButtonFocus.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else {
            val currentChannel = uiState.channel ?: channel
            val metadata = uiState.metadata

            // Background Backdrop
            if (!metadata?.backdrop.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(metadata?.backdrop)
                        .crossfade(true)
                        .size(1280, 720) // Limit size for memory
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.3f) // Dimmed
                )
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            // Content Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left: Poster
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .width(200.dp)
                        .aspectRatio(2f / 3f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentChannel.cover)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right: Info & Actions
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = currentChannel.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = metadata?.year ?: "", color = Color.Gray)
                        Text(text = metadata?.rating ?: "", color = NeonBlue)
                        Text(text = currentChannel.quality ?: "HD", color = Color.Gray)
                    }

                    Text(
                        text = metadata?.plot ?: "No description available.",
                        color = Color.LightGray,
                        maxLines = 4,
                        lineHeight = 24.sp,
                        modifier = Modifier.width(600.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionButton(
                            text = "PLAY",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            focusRequester = playButtonFocus,
                            onClick = { onPlayClick(currentChannel.url) }
                        )

                        ActionButton(
                            text = if (uiState.isFavorite) "SAVED" else "FAVORITE",
                            icon = Icons.Default.Star,
                            isPrimary = false,
                            onClick = { viewModel.toggleFavorite() }
                        )

                        ActionButton(
                            text = "BACK",
                            icon = Icons.Default.ArrowBack,
                            isPrimary = false,
                            onClick = onBack
                        )
                    }

                    // Versions / Episodes (if available)
                    if (uiState.relatedVersions.size > 1) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("VERSIONS", color = Color.Gray, fontSize = 14.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.relatedVersions) { version ->
                                VersionChip(
                                    text = version.quality ?: "UNK",
                                    onClick = { onPlayClick(version.url) }
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
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = if (isFocused) Color.White else if (isPrimary) NeonBlue else Color(0xFF333333)
    val contentColor = if (isFocused) Color.Black else if (isPrimary) Color.Black else Color.White

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Icon(icon, contentDescription = null, tint = contentColor)
        Spacer(Modifier.width(8.dp))
        Text(text, color = contentColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VersionChip(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val border = if (isFocused) NeonBlue else Color.Gray

    Box(
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White)
    }
}

// Helper Modifier for Alpha
fun Modifier.alpha(alpha: Float) = this.then(
    Modifier.draw.drawWithContent {
        drawContent()
        drawRect(Color.Black, alpha = 1f - alpha, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }
)