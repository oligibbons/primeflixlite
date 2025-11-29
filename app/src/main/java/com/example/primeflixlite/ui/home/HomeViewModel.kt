package com.example.primeflixlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var allChannels: List<ChannelWithProgram> = emptyList()
    private var refreshJob: Job? = null

    init {
        loadPlaylists()
        observeFavorites() // Start listening for "My List" changes
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.playlists.collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
                // Auto-select first playlist if none selected
                if (_uiState.value.selectedPlaylist == null && playlists.isNotEmpty()) {
                    selectPlaylist(playlists.first())
                }
            }
        }
    }

    // NEW: Real-time Favorites Sync
    private fun observeFavorites() {
        viewModelScope.launch {
            repository.favorites.collect { favs ->
                _uiState.value = _uiState.value.copy(favorites = favs)
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _uiState.value = _uiState.value.copy(
            selectedPlaylist = playlist,
            isLoading = true
        )
        fetchChannels(playlist)
    }

    private fun fetchChannels(playlist: Playlist) {
        refreshJob?.cancel()
        refreshJob = repository.getChannelsWithEpg(playlist.url)
            .onEach { channels ->
                allChannels = channels
                refreshContent()
            }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: StreamType) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, selectedCategory = "All")
        refreshContent()
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        filterChannels()
    }

    // Called when returning from Player to update progress bars
    fun refreshContinueWatching() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            try {
                // Fetch latest progress from DB
                val progressList = repository.getContinueWatching(tab)
                // Map the URLs back to Channel objects
                val progressChannels = progressList.mapNotNull { progress ->
                    allChannels.find { it.channel.url == progress.channelUrl }?.channel
                }
                _uiState.value = _uiState.value.copy(continueWatching = progressChannels)
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    private fun refreshContent() {
        val tab = _uiState.value.selectedTab

        // 1. Filter by Tab (Live, Movie, Series)
        val tabChannels = allChannels.filter { it.channel.type == tab }

        // 2. Extract Categories
        val categories = listOf("All") + tabChannels
            .map { it.channel.group }
            .distinct()
            .sorted()

        // 3. Update State
        _uiState.value = _uiState.value.copy(
            categories = categories,
            isLoading = false
        )

        // 4. Update Continue Watching for this tab
        refreshContinueWatching()

        // 5. Apply Category Filter
        filterChannels()
    }

    private fun filterChannels() {
        val tab = _uiState.value.selectedTab
        val category = _uiState.value.selectedCategory

        val filtered = allChannels.filter { item ->
            item.channel.type == tab &&
                    (category == "All" || item.channel.group == category)
        }

        _uiState.value = _uiState.value.copy(displayedChannels = filtered)
    }

    fun backToPlaylists() {
        _uiState.value = _uiState.value.copy(selectedPlaylist = null)
    }

    // Deprecated: UI now uses AddXtreamScreen, keeping stub to prevent compilation error if referenced
    fun addSamplePlaylist() {}
}