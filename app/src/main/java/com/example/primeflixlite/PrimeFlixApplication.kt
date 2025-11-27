package com.example.primeflixlite

import android.app.Application
import com.example.primeflixlite.di.AppModule

class PrimeFlixApplication : Application() {

    // Renamed 'container' to 'appModule' for consistency with UI calls
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()
        // Initialize the Dependency Injection container
        appModule = AppModule(this)
    }
}