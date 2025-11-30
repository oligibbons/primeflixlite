package com.example.primeflixlite.ui.theme

import androidx.compose.ui.graphics.Color

// --- NEON & VOID PALETTE (OPTIMIZED) ---

// Backgrounds - Absolute Zero for OLED/Performance
val VoidBlack = Color(0xFF000000)      // Main Background (No render cost)
val OffBlack = Color(0xFF151515)       // Card placeholders / Off-state
val PanelBlack = Color(0xFF0A0A0A)     // Sidebars / Overlays

// The "Neon" Brand
val NeonBlue = Color(0xFF0099FF)       // Primary Brand (Samsung TV Plus vibe)
val NeonBlueDim = Color(0xFF004488)    // Inactive/Background elements
val NeonYellow = Color(0xFFFFD600)     // FOCUS STATE (High Contrast / Cyberpunk)

// Text & functional
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFCCCCCC)
val MidGray = Color(0xFF808080)
val DarkGray = Color(0xFF404040)

// Semantic
val ErrorRed = Color(0xFFFF4444)
val SuccessGreen = Color(0xFF00C853)

// Scrims (Pre-calculated alphas to save GPU blending)
// usage: background(ScrimBlack) instead of Black.copy(alpha=0.8f)
val ScrimBlack = Color(0xB3000000)     // ~70% Black for text readability
val ScrimWeak = Color(0x66000000)      // ~40% Black for subtle dimming