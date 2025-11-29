package com.example.primeflixlite.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White
import com.example.primeflixlite.util.TimeUtils

@Composable
fun PlayerOverlay(
    channel: Channel,
    program: Programme?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onResize: () -> Unit,
    onFavorite: () -> Unit, // NEW callback
    onBack: () -> Unit
) {
    var areControlsVisible by remember { mutableStateOf(false) }

    // Auto-hide controls
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            kotlinx.coroutines.delay(4000)
            areControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { areControlsVisible = !areControlsVisible }
    ) {

        // Massive Pause Icon
        val iconAlpha by animateFloatAsState(targetValue = if (!isPlaying) 1f else 0f)
        if (iconAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = NeonBlue.copy(alpha = iconAlpha),
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        // Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
            ) {
                // TOP BAR
                Row(modifier = Modifier.fillMaxWidth().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton(icon = Icons.Default.ArrowBack, onClick = onBack)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = channel.title, style = MaterialTheme.typography.headlineSmall, color = White, fontWeight = FontWeight.Bold)
                        // Display EPG if available
                        if (program != null) {
                            Text(
                                text = "${TimeUtils.formatTime(program.start)} - ${TimeUtils.formatTime(program.end)}: ${program.title}",
                                color = NeonBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (channel.group.isNotEmpty()) {
                            Text(text = channel.group, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Resize Button
                    PlayerButton(icon = Icons.Default.AspectRatio, onClick = onResize)
                }

                // BOTTOM BAR
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(32.dp)) {
                    // Seek Bar
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = TimeUtils.formatDuration(currentPosition), color = NeonBlue)
                        Text(text = TimeUtils.formatDuration(duration), color = Color.Gray)
                    }
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() else 0f,
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, // Spread items out
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Favorite Button
                        PlayerButton(
                            icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            onClick = onFavorite,
                            iconColorOverride = if (channel.isFavorite) Color.Red else White
                        )

                        // Center: Playback Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerButton(icon = Icons.Default.SkipPrevious, onClick = onPrev)
                            Spacer(Modifier.width(24.dp))
                            PlayerButton(icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, onClick = onPlayPause, isPrimary = true)
                            Spacer(Modifier.width(24.dp))
                            PlayerButton(icon = Icons.Default.SkipNext, onClick = onNext)
                        }

                        // Right: Spacer to balance layout (empty box of same size as favorite button)
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    iconColorOverride: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val size = if (isPrimary) 64.dp else 48.dp
    val iconSize = if (isPrimary) 32.dp else 24.dp
    val bgColor = if (isFocused) NeonBlue else if (isPrimary) White else Color.Transparent

    // Determine icon color: Focused -> White, Primary -> Black, Override -> Custom, Default -> White
    val iconColor = if (isFocused) White
    else if (isPrimary) VoidBlack
    else iconColorOverride ?: White

    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(iconSize))
    }
}