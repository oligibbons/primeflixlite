package com.example.primeflixlite.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// We force Dark Theme for the "Neon & Void" aesthetic
private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonBlueDim,
    tertiary = White,
    background = VoidBlack,
    surface = OffBlack,
    onPrimary = VoidBlack,
    onSecondary = White,
    onTertiary = VoidBlack,
    onBackground = White,
    onSurface = LightGray,
)

// We reuse the dark scheme even for "light" mode to enforce the app's style
private val LightColorScheme = DarkColorScheme

@Composable
fun PrimeFlixLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but we DISABLE it
    // to maintain the strict Neon/Black aesthetic and save performance.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses default typography for now
        content = content
    )
}