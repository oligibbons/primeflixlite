package com.example.primeflixlite.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.util.TimeUtils

@Composable
fun PlayerOverlay(
    channel: Channel?,
    program: Programme?,
    isVisible: Boolean,
    isBuffering: Boolean,
    error: String?,
    videoAspectRatio: String
) {
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(50.dp))
                Spacer(Modifier.height(16.dp))
                Text("Stream Unavailable", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(error, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else if (isBuffering) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonBlue)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text("LIVE", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(channel?.group ?: "Unknown", color = Color.LightGray)
                }
                Text("Aspect: $videoAspectRatio", color = NeonBlue, fontSize = 12.sp)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 48.dp, bottom = 48.dp)
            ) {
                Text(
                    text = channel?.title ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                if (program != null) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NeonBlue
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        // FIXED: Uses 'start' and 'end' instead of 'startTime'/'endTime'
                        Text(
                            text = TimeUtils.getDurationString(program.start, program.end),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.width(100.dp)
                        )

                        LinearProgressIndicator(
                            progress = { TimeUtils.getProgress(program.start, program.end) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .padding(horizontal = 12.dp)
                                .background(Color.DarkGray, RoundedCornerShape(2.dp)),
                            color = NeonBlue,
                            trackColor = Color.DarkGray,
                        )
                    }

                    if (!program.description.isNullOrEmpty()) {
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray,
                            maxLines = 2
                        )
                    }
                } else {
                    Text(
                        text = "No Program Information",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}