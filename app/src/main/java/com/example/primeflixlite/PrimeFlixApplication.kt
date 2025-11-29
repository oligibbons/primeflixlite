package com.example.primeflixlite

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PrimeFlixApplication : Application() {
    // Hilt handles injection automatically.
    // No manual module instantiation needed here.
}