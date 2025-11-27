package com.example.primeflixlite.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue

@Composable
fun MovieCard(
    channel: Channel,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    // Visual Feedback (Scale + Border)
    val scale = if (isFocused) 1.08f else 1f
    val border = if (isFocused) NeonBlue else Color.Transparent

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(2f / 3f) // Standard Movie Poster Ratio
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(2.dp, border), RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        if (!channel.cover.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(channel.cover)
                    .crossfade(true)
                    .size(300, 450) // Resize to save RAM
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback for missing covers
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.title.take(1),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.DarkGray
                )
            }
        }

        // Title Overlay (Only visible if focused or if no cover)
        if (isFocused || channel.cover.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 100f
                        )
                    )
            ) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
    }
}