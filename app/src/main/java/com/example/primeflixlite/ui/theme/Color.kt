package com.example.primeflixlite.ui.theme

import androidx.compose.ui.graphics.Color

// --- NEON & VOID PALETTE ---
// Constraint: Deep Black background for projector contrast
val VoidBlack = Color(0xFF000000)
val OffBlack = Color(0xFF121212) // For cards/surfaces to stand out slightly from the void

// Constraint: Neon Blue for "Glow" simulation without expensive RenderEffect
val NeonBlue = Color(0xFF0099FF)
val NeonBlueDim = Color(0xFF004C80) // Unfocused state

val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFB0B0B0)

// Standard Material Colors mapped to our Palette
val Purple80 = NeonBlue
val PurpleGrey80 = NeonBlueDim
val Pink80 = NeonBlue

val Purple40 = NeonBlue
val PurpleGrey40 = NeonBlueDim
val Pink40 = NeonBlue