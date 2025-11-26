package com.example.primeflixlite

import android.app.Application
import com.example.primeflixlite.di.AppModule

class PrimeFlixApplication : Application() {

    // Expose the container to the rest of the app
    lateinit var container: AppModule

    override fun onCreate() {
        super.onCreate()
        container = AppModule(this)
    }
}