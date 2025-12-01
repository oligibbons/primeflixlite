package com.example.primeflixlite.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White
import com.example.primeflixlite.util.TimeUtils

// Pre-defined colors to avoid allocation during render
private val OverlayBackground = Color(0xCC000000) // 80% Black
private val DrawerBackground = Color(0xF2000000) // 95% Black
private val ActiveText = NeonBlue
private val InactiveText = Color.LightGray

@Composable
fun PlayerOverlay(
    isVisible: Boolean,
    isDrawerVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    epgTitle: String?,
    currentTime: Long,
    duration: Long,
    bufferedPercentage: Int,
    playlist: List<Channel>,
    currentChannelId: String,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onChannelSelect: (Channel) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // --- BOTTOM OSD ---
        AnimatedVisibility(
            visible = isVisible && !isDrawerVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayBackground) // Flat color, no blur
                    .padding(24.dp)
            ) {
                // Info Section
                Text(
                    text = title,
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (epgTitle != null) {
                    Text(
                        text = epgTitle,
                        color = ActiveText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                if (duration > 0) {
                    NeonProgressBar(
                        currentTime = currentTime,
                        duration = duration,
                        buffered = bufferedPercentage
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ControlBtn(Icons.Default.SkipPrevious, onClick = onPrev)
                    Spacer(modifier = Modifier.width(16.dp))

                    ControlBtn(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = onPlayPause
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    ControlBtn(Icons.Default.SkipNext, onClick = onNext)
                }
            }
        }

        // --- CHANNEL DRAWER (LEFT) ---
        AnimatedVisibility(
            visible = isDrawerVisible,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val listState = rememberLazyListState()

            // Auto-scroll to current channel when drawer opens
            LaunchedEffect(isDrawerVisible) {
                val index = playlist.indexOfFirst { it.url == currentChannelId }
                if (index != -1) listState.scrollToItem(index)
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .background(DrawerBackground)
                    .padding(16.dp)
            ) {
                Text(
                    "CHANNELS",
                    color = ActiveText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(playlist, key = { it.url }) { channel ->
                        ChannelItem(
                            channel = channel,
                            isSelected = channel.url == currentChannelId,
                            onClick = { onChannelSelect(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeonProgressBar(currentTime: Long, duration: Long, buffered: Int) {
    val progress = currentTime.toFloat() / duration.coerceAtLeast(1)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(TimeUtils.formatDuration(currentTime), color = Color.LightGray, fontSize = 12.sp)
            Text(TimeUtils.formatDuration(duration), color = Color.LightGray, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.DarkGray, RoundedCornerShape(2.dp))
                .padding(vertical = 1.dp) // padding
        ) {
            // Buffer
            Box(
                modifier = Modifier
                    .fillMaxWidth(buffered / 100f)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            // Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(ActiveText) // Neon Blue
            )
        }
    }
}

@Composable
fun ControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(if (isPrimary) 56.dp else 40.dp)
            .background(
                color = if (isFocused) ActiveText else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = if (isFocused) 0.dp else 1.dp,
                color = if (isPrimary) ActiveText else Color.Gray,
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) VoidBlack else White,
            modifier = Modifier.size(if (isPrimary) 32.dp else 24.dp)
        )
    }
}

@Composable
fun ChannelItem(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isFocused) White else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected && !isFocused) 1.dp else 0.dp,
                color = if (isSelected) ActiveText else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected && !isFocused) {
            // Tiny indicator for currently playing item when not focused
            Box(modifier = Modifier.size(6.dp).background(ActiveText, RoundedCornerShape(50)))
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = channel.title,
            color = if (isFocused) VoidBlack else if (isSelected) ActiveText else InactiveText,
            fontSize = 14.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            overflow = TextOverflow.Ellipsis
        )
    }
}