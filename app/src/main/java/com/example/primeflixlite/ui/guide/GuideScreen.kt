package com.example.primeflixlite.ui.guide

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White
import com.example.primeflixlite.util.TimeUtils

@Composable
fun GuideScreen(
    viewModel: GuideViewModel,
    imageLoader: coil.ImageLoader,
    onChannelClick: (Channel) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // Header
        Text(
            "TV Guide",
            style = MaterialTheme.typography.headlineMedium,
            color = White,
            modifier = Modifier.padding(24.dp)
        )

        // The Guide List
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.channels, key = { it.channel.url }) { item ->
                GuideChannelRow(
                    item = item,
                    imageLoader = imageLoader,
                    onClick = { onChannelClick(item.channel) }
                )
            }
        }
    }
}

@Composable
fun GuideChannelRow(
    item: GuideChannelItem,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NeonBlue else Color.Transparent
    val bg = if (isFocused) Color(0xFF222222) else Color(0xFF111111)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(bg)
            .border(BorderStroke(2.dp, borderColor))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Icon
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!item.channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.channel.cover).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            } else {
                Text(item.channel.title.take(1), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        // Channel Name & Number
        Text(
            text = item.channel.title,
            color = White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(150.dp).padding(horizontal = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // EPG Timeline Row
        // We use a LazyRow here so we can scroll horizontally through time if needed
        LazyRow(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (item.programmes.isEmpty()) {
                item {
                    Text(
                        "No Information",
                        color = Color.DarkGray,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(16.dp)
                    )
                }
            } else {
                items(item.programmes) { prog ->
                    ProgramCell(prog)
                }
            }
        }
    }
}

@Composable
fun ProgramCell(programme: Programme) {
    // Calculate width based on duration (1 min = 4.dp approx)
    val durationMins = (programme.end - programme.start) / 60000
    val width = (durationMins * 3).coerceAtLeast(60).coerceAtMost(400).dp // Scale factor

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A))
            .padding(8.dp)
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
            .padding(4.dp), // Inner padding
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = programme.title,
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${TimeUtils.formatTime(programme.start)} - ${TimeUtils.formatTime(programme.end)}",
            color = NeonBlue,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}