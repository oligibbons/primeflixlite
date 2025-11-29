package com.example.primeflixlite.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
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
    imageLoader: ImageLoader,
    onChannelClick: (Channel) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
            }
            Text("TV Guide", style = MaterialTheme.typography.titleLarge, color = White)
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else {
            // EPG Grid
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.channels) { item ->
                    GuideChannelRow(
                        channel = item.channel,
                        programs = item.programs,
                        imageLoader = imageLoader,
                        onClick = { onChannelClick(item.channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun GuideChannelRow(
    channel: Channel,
    programs: List<Programme>,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(0.5.dp, Color.DarkGray)
            .clickable { onClick() }
    ) {
        // Channel Icon/Name
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(channel.cover).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
            } else {
                Text(
                    text = channel.title.take(3),
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Programs Timeline
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(programs) { program ->
                GuideProgramCard(program)
            }
            if (programs.isEmpty()) {
                item {
                    Box(modifier = Modifier.width(300.dp).fillMaxHeight().padding(16.dp)) {
                        Text("No Information", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GuideProgramCard(program: Programme) {
    // 1 hour = 200.dp width roughly
    val durationMillis = program.end - program.start
    val durationHours = durationMillis / (1000.0 * 60 * 60)
    // FIX: Convert to Float for dp calculation
    val width = (durationHours * 200).toFloat().coerceAtLeast(50f).dp

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xFF2A2A2A))
            .padding(4.dp)
            .border(0.5.dp, Color.Black)
    ) {
        Column {
            Text(
                text = program.title,
                color = White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${TimeUtils.formatTime(program.start)} - ${TimeUtils.formatTime(program.end)}",
                color = NeonBlue,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}