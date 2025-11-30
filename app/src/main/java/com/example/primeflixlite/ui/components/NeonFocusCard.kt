package com.example.primeflixlite.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.NeonYellow
import com.example.primeflixlite.ui.theme.OffBlack
import com.example.primeflixlite.ui.theme.ScrimBlack
import com.example.primeflixlite.ui.theme.White

// Hoisted for performance (avoids re-allocation on scroll)
private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun NeonFocusCard(
    channel: Channel,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Optional: Pass specific aspect ratio (Movie 2:3 vs TV 16:9)
    aspectRatio: Float = 2f / 3f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val context = LocalContext.current

    // --- ANIMATION ---
    // Spring is too expensive for low-end lists; use FastOutSlowIn Tween
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    // --- GLOW BRUSH ---
    // A pre-calculated gradient that "fakes" a neon light behind the card
    val glowBrush = remember {
        Brush.verticalGradient(
            colors = listOf(NeonYellow.copy(alpha = 0.6f), Color.Transparent)
        )
    }

    // --- IMAGE REQUEST ---
    val imageRequest = remember(channel.cover) {
        ImageRequest.Builder(context)
            .data(channel.cover)
            // CRITICAL: Downsample. 300px is max needed for a grid card.
            // Saves MBs of RAM on the Allwinner chip.
            .size(300, 450)
            .crossfade(false) // Disable crossfade to speed up scroll
            .build()
    }

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(aspectRatio)
            // Fake Glow Effect (Only draws when focused)
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        drawRect(brush = glowBrush, alpha = 0.5f)
                    }
                } else Modifier
            )
            .clip(CardShape)
            // The "Hard" Neon Border
            .border(
                border = if (isFocused) BorderStroke(3.dp, NeonYellow) else BorderStroke(0.dp, Color.Transparent),
                shape = CardShape
            )
            .background(OffBlack)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable ripple, we use scale
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
    ) {
        // 1. POSTER IMAGE
        if (!channel.cover.isNullOrEmpty()) {
            AsyncImage(
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback for missing images
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = channel.title.take(1),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.DarkGray
                )
            }
        }

        // 2. TEXT OVERLAY (Only shows on Focus or if missing image)
        // We use a "Scrim" background to ensure text is readable
        if (isFocused || channel.cover.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, ScrimBlack),
                            startY = 100f
                        )
                    )
            ) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            blurRadius = 2f
                        )
                    ),
                    color = if(isFocused) NeonYellow else White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
        }

        // 3. WATCHED INDICATOR (Optional Overlay)
        // You can add a progress bar here later if needed
    }
}