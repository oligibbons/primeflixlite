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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgress
import com.example.primeflixlite.ui.theme.NeonBlue

@Composable
fun TopNavBar(
    selectedTab: StreamType,
    onTabSelected: (StreamType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamType.values().forEach { type ->
            NavTab(
                title = type.name, // LIVE, MOVIE, SERIES
                isSelected = selectedTab == type,
                onClick = { onTabSelected(type) }
            )
        }
    }
}

@Composable
fun NavTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    val color = if (isSelected) NeonBlue else if (isFocused) Color.White else Color.Gray
    val scale = if (isFocused) 1.1f else 1f

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .scale(scale)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ContinueWatchingLane(
    title: String,
    items: List<ChannelWithProgress>,
    imageLoader: coil.ImageLoader,
    onItemClick: (String) -> Unit // Pass URL or ID
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items) { item ->
                HistoryCard(item = item, imageLoader = imageLoader, onClick = { onItemClick(item.channel.url) })
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: ChannelWithProgress,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale = if (isFocused) 1.05f else 1f
    val border = if (isFocused) NeonBlue else Color.Transparent

    Column(
        modifier = Modifier
            .width(200.dp) // Wider card for history
            .scale(scale)
            .border(BorderStroke(2.dp, border), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        // Thumbnail
        Box(modifier = Modifier.aspectRatio(16f / 9f)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.channel.cover)
                    .crossfade(true)
                    .size(400, 225)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Progress Bar Overlay
            LinearProgressIndicator(
                progress = { item.getProgressFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                color = NeonBlue,
                trackColor = Color.Black.copy(alpha = 0.5f)
            )
        }

        // Title
        Text(
            text = item.channel.title,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(8.dp)
        )
    }
}