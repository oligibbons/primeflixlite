package com.example.primeflixlite.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val repository: PrimeFlixRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    init {
        loadPlaylists()
    }

    private fun setLoading(message: String?) {
        _uiState.update { it.copy(loadingMessage = message) }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            setLoading("Locating Profiles...")
            // Artificial delay for "feel" (users trust loading screens that last >0.5s)
            delay(800)

            repository.getPlaylists().collect { playlists ->
                _uiState.update {
                    it.copy(
                        playlists = playlists,
                        loadingMessage = null // Done
                    )
                }
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        loadContent(playlist)
    }

    fun backToPlaylists() {
        // Cancel any active loading
        currentJob?.cancel()
        _uiState.update {
            HomeState(
                playlists = it.playlists, // Keep loaded playlists
                loadingMessage = null
            )
        }
    }

    fun selectTab(tab: StreamType) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        // Reload content for the new tab
        _uiState.value.selectedPlaylist?.let { loadContent(it) }
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val allChannels = state.displayedChannels
            // In a real app, you'd filter the master list here.
            // For now, we just update the selection UI state.
            state.copy(selectedCategory = category)
        }
        // Ideally trigger a filter function here
    }

    // --- SPOTLIGHT LOGIC ---
    fun updateSpotlight(channel: Channel) {
        // Debounce could be added here if performance suffers,
        // but simple state update is usually cheap enough.
        if (_uiState.value.spotlightChannel?.id != channel.id) {
            _uiState.update { it.copy(spotlightChannel = channel) }
        }
    }

    private fun loadContent(playlist: Playlist) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val typeName = _uiState.value.selectedTab.name

            setLoading("Syncing ${typeName} Library...")

            // 1. Get Categories (Fast)
            val categories = repository.getCategories(playlist.id, typeName)

            setLoading("Curating Content...")

            // 2. Get Channels (Heavier)
            // For "Lite" version, we limit to first 500 to prevent OOM on grid
            val channels = repository.getChannelsWithPrograms(playlist.id, typeName)
                .take(500)

            // 3. Get Favorites / History
            val favorites = repository.getFavorites(playlist.id)
            val history = repository.getContinueWatching(playlist.id)

            _uiState.update { state ->
                // Set initial spotlight to the first item found (if any)
                val firstItem = channels.firstOrNull()?.channel
                    ?: history.firstOrNull()
                    ?: favorites.firstOrNull()

                state.copy(
                    categories = categories.map { it.name },
                    displayedChannels = channels,
                    favorites = favorites,
                    continueWatching = history,
                    loadingMessage = null,
                    spotlightChannel = firstItem
                )
            }
        }
    }
}