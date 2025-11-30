package com.example.primeflixlite.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.primeflixlite.R
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun LoadingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    // Pulse Animation for the Logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            // Trap clicks and focus so the user cannot interact with the UI behind
            .clickable(enabled = true, onClick = {})
            .focusable(true),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // 1. Logo (Breathing)
            Image(
                painter = painterResource(id = R.drawable.logo_transparent),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Neon Progress Line (Thin & Sleek)
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(2.dp),
                color = NeonBlue,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Friendly Text
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}