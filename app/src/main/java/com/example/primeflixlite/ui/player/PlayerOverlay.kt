package com.example.primeflixlite.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // FIX: Import added
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.NeonYellow
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White
import com.example.primeflixlite.util.TimeUtils

@Composable
fun PlayerOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    currentTime: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    // Channel List Support
    isChannelListVisible: Boolean,
    channelList: List<Channel>,
    onChannelClick: (Channel) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. BOTTOM CONTROLS (Standard OSD)
        if (isVisible && !isChannelListVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 300f
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonYellow,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        if (duration > 0) {
                            val progress = currentTime.toFloat() / duration.toFloat()
                            Text(
                                text = TimeUtils.formatDuration(currentTime),
                                color = Color.LightGray
                            )
                            Slider(
                                value = progress,
                                onValueChange = onSeek,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonBlue,
                                    activeTrackColor = NeonBlue,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                            Text(
                                text = TimeUtils.formatDuration(duration),
                                color = Color.LightGray
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color.Red, MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("LIVE", color = White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
            }
        }

        // 2. CHANNEL DRAWER
        AnimatedVisibility(
            visible = isChannelListVisible,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(350.dp)
                    .background(VoidBlack.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "CHANNELS",
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(channelList) { channel ->
                            ChannelListRow(
                                channel = channel,
                                isCurrent = channel.title == title,
                                onClick = { onChannelClick(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelListRow(channel: Channel, isCurrent: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) White else if (isCurrent) Color(0xFF222222) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent && !isFocused) {
            Text("▶", color = NeonYellow, modifier = Modifier.padding(end = 8.dp))
        }

        Text(
            text = channel.title,
            color = if (isFocused) VoidBlack else if (isCurrent) NeonYellow else Color.LightGray,
            maxLines = 1,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}