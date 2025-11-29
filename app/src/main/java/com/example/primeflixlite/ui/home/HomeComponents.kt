package com.example.primeflixlite.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.White

@Composable
fun ContinueWatchingLane(
    title: String,
    items: List<Channel>,
    imageLoader: coil.ImageLoader,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp) // Space for focus glow
        ) {
            items(items) { channel ->
                ContinueWatchingCard(
                    channel = channel,
                    imageLoader = imageLoader,
                    onClick = { onItemClick(channel.url) }
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    channel: Channel,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale = if (isFocused) 1.1f else 1f
    val borderColor = if (isFocused) NeonBlue else Color.Transparent

    Column(
        modifier = Modifier
            .width(200.dp) // Wider card for Resume
            .scale(scale)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Box(modifier = Modifier.height(110.dp).fillMaxWidth().background(Color.Black)) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(channel.cover).crossfade(true).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(channel.title.take(1), color = Color.Gray)
                }
            }
        }

        LinearProgressIndicator(
            progress = 0.5f,
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = NeonBlue,
            trackColor = Color.DarkGray
        )

        Text(
            text = channel.title,
            color = if(isFocused) White else Color.LightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Removed Duplicate MovieCard