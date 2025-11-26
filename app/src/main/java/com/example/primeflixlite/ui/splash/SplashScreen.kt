package com.example.primeflixlite.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.primeflixlite.R
import com.example.primeflixlite.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation states
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        // 1. Fade In & Scale Up (Entry)
        alpha.animateTo(1f, animationSpec = tween(1000))
        scale.animateTo(1.05f, animationSpec = tween(2500)) // Slow zoom

        // 2. Hold
        delay(500)

        // 3. Exit
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow (Subtle Radial behind logo)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1A1A), VoidBlack),
                        radius = 800f
                    )
                )
        )

        // The Spotlight Logo
        Image(
            painter = painterResource(id = R.drawable.logo_spotlight),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit, // Ensure entire logo is visible
            modifier = Modifier
                .size(400.dp) // Large on TV
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}