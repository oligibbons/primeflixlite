package com.example.primeflixlite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import com.example.primeflixlite.ui.home.HomeViewModel
import com.example.primeflixlite.ui.player.PlayerViewModel
import com.example.primeflixlite.ui.search.SearchViewModel

class ViewModelFactory(
    private val repository: PrimeFlixRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}