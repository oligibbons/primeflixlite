package com.example.primeflixlite.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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

// Hoist shape to avoid allocation on every recomposition
private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun MovieCard(
    channel: Channel,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Visual Feedback (Scale) - Reduced scale slightly for smoother animation on low-end
    val scale = if (isFocused) 1.05f else 1f

    // OPTIMIZATION: Memoize the gradient to prevent object creation during scroll
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
            startY = 100f
        )
    }

    // OPTIMIZATION: Memoize the ImageRequest
    val imageRequest = remember(channel.cover) {
        ImageRequest.Builder(context)
            .data(channel.cover)
            // Resize is CRITICAL for low RAM devices.
            // 300x450 is plenty for a grid card.
            .size(300, 450)
            .crossfade(false) // Disable crossfade for performance
            .build()
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(2f / 3f)
            .clip(CardShape)
            // OPTIMIZATION: Only apply border logic if focused
            .then(if (isFocused) Modifier.border(BorderStroke(2.dp, NeonBlue), CardShape) else Modifier)
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        if (!channel.cover.isNullOrEmpty()) {
            AsyncImage(
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
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

        // Title Overlay
        if (isFocused || channel.cover.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
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