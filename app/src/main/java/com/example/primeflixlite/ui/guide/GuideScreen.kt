package com.example.primeflixlite.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.primeflixlite.data.local.model.ChannelWithProgram
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
    val listFocusRequester = remember { FocusRequester() }

    BackHandler { onBack() }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.channels.isNotEmpty()) {
            listFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp)
    ) {
        // --- HEADER ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            // FIXED: Use AutoMirrored icon
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Text("TV Guide", style = MaterialTheme.typography.headlineMedium, color = White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(TimeUtils.getCurrentTimeFormatted(), color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = "Group: ${uiState.currentGroup}",
            color = NeonBlue,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.focusRequester(listFocusRequester)
            ) {
                items(
                    items = uiState.channels,
                    key = { it.channel.id }
                ) { item ->
                    GuideRow(
                        item = item,
                        imageLoader = imageLoader,
                        onClick = { onChannelClick(item.channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun GuideRow(
    item: ChannelWithProgram,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NeonBlue else Color.Transparent
    val backgroundColor = if (isFocused) Color(0xFF252525) else Color(0xFF151515)

    val channel = item.channel
    val program = item.program

    val context = LocalContext.current
    val imageRequest = remember(channel.cover) {
        ImageRequest.Builder(context)
            .data(channel.cover)
            .size(100, 100)
            .crossfade(false)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(channel.title.take(1), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = channel.title,
            color = if (isFocused) White else Color.LightGray,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(180.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (program != null) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = program.title,
                        color = White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = TimeUtils.formatTime(program.start),
                        color = NeonBlue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                // FIXED: Use lambda for progress
                LinearProgressIndicator(
                    progress = { TimeUtils.getProgress(program.start, program.end) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = NeonBlue,
                    trackColor = Color.DarkGray,
                )
            } else {
                Text("No Program Information", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}