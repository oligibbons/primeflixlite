package com.example.primeflixlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    // 1. All Playlists (Sidebar)
    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Selected Playlist Selection
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()

    // 3. Channels for Selected Playlist (Main Grid)
    val currentChannels: StateFlow<List<Channel>> = _selectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist != null) {
                repository.getChannels(playlist.url)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
    }

    fun addSamplePlaylist() {
        viewModelScope.launch {
            repository.addPlaylist(
                title = "Test Playlist (Samsung TV)",
                url = "https://i.mjh.nz/SamsungTVPlus/us.m3u8",
                source = DataSource.M3U
            )
        }
    }

    fun syncCurrentPlaylist() {
        val current = _selectedPlaylist.value ?: return
        viewModelScope.launch {
            repository.syncPlaylist(current)
        }
    }
}