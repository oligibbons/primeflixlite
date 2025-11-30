package com.example.primeflixlite.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.util.FeedbackManager
import com.example.primeflixlite.util.FeedbackState

@Composable
fun NotificationOverlay(
    feedbackManager: FeedbackManager
) {
    val state by feedbackManager.state.collectAsState()

    // Auto-dismiss Success/Error
    LaunchedEffect(state) {
        if (state is FeedbackState.Success || state is FeedbackState.Error) {
            kotlinx.coroutines.delay(4000)
            feedbackManager.dismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = state !is FeedbackState.Idle,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .width(350.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VoidBlack.copy(alpha = 0.95f))
                    .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                when (val currentState = state) {
                    is FeedbackState.Loading -> LoadingProgress(currentState)
                    is FeedbackState.ImportingCount -> LoadingCount(currentState)
                    is FeedbackState.Success -> StatusContent(Icons.Default.CheckCircle, Color.Green, currentState.message)
                    is FeedbackState.Error -> StatusContent(Icons.Default.Error, Color.Red, currentState.message)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun LoadingProgress(state: FeedbackState.Loading) {
    Column {
        Header(state.task, state.type)
        Spacer(modifier = Modifier.height(12.dp))

        if (state.progress >= 0f) {
            val animatedProgress by animateFloatAsState(targetValue = state.progress)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = NeonBlue,
                    trackColor = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${(animatedProgress * 100).toInt()}%", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            // Indeterminate
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = NeonBlue,
                trackColor = Color.DarkGray
            )
        }
    }
}

@Composable
private fun LoadingCount(state: FeedbackState.ImportingCount) {
    val percentage = if (state.total > 0) state.current.toFloat() / state.total.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = percentage)

    Column {
        Header(state.task, state.type)
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = NeonBlue,
                trackColor = Color.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${state.current} / ${state.total}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Header(task: String, type: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonBlue, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = task, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(text = type, color = NeonBlue, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatusContent(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}