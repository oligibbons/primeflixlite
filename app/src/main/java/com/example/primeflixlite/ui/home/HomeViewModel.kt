package com.example.primeflixlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState = _uiState.asStateFlow()

    private var contentJob: Job? = null
    private var groupsJob: Job? = null

    init {
        // [Requirement] Default tab is SERIES
        _uiState.value = _uiState.value.copy(selectedTab = StreamType.SERIES)

        loadPlaylists()
        observeFavorites()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.playlists.collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
                if (_uiState.value.selectedPlaylist == null && playlists.isNotEmpty()) {
                    selectPlaylist(playlists.first())
                }
            }
        }
    }

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
            loadingMessage = "Loading Playlist..."
        )
        refreshContent()
    }

    fun selectTab(tab: StreamType) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            selectedCategory = "All"
        )
        refreshContent()
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadChannels()
    }

    private fun refreshContent() {
        loadGroups()
        loadChannels()
        refreshContinueWatching()
    }

    private fun loadGroups() {
        val playlist = _uiState.value.selectedPlaylist ?: return
        val type = _uiState.value.selectedTab.name

        groupsJob?.cancel()
        groupsJob = repository.getGroups(playlist.url, type)
            .onEach { groups ->
                val smartCategories = mutableListOf("All")
                smartCategories.add("Favorites")

                if (_uiState.value.selectedTab != StreamType.LIVE) {
                    smartCategories.add("Recently Added")
                    smartCategories.add("Continue Watching")
                }
                smartCategories.addAll(groups)

                _uiState.value = _uiState.value.copy(categories = smartCategories)
            }
            .launchIn(viewModelScope)
    }

    private fun loadChannels() {
        val playlist = _uiState.value.selectedPlaylist ?: return
        val type = _uiState.value.selectedTab
        val category = _uiState.value.selectedCategory

        contentJob?.cancel()
        _uiState.value = _uiState.value.copy(loadingMessage = "Loading Content...")

        // Robust flow selection without ambiguity
        val flow = when (category) {
            "All" -> {
                repository.getBrowsingContent(playlist.url, type, "All")
            }
            "Favorites" -> {
                // Fix for ambiguity: Use kotlinx.coroutines.flow.map explicitly for the outer map
                // Return a flow that emits the list of favorites filtered by type and playlist
                repository.favorites.map { favs ->
                    favs.filter { it.playlistUrl == playlist.url && it.type == type.name }
                        .map { ChannelWithProgram(it, null) }
                }
            }
            "Recently Added" -> {
                repository.getRecentAdded(playlist.url, type)
                    .map { channels -> channels.map { ChannelWithProgram(it, null) } }
            }
            "Continue Watching" -> {
                repository.getContinueWatching(type)
                    .map { progressItems ->
                        progressItems.map { ChannelWithProgram(it.channel, null) }
                    }
            }
            else -> {
                repository.getBrowsingContent(playlist.url, type, category)
            }
        }

        contentJob = flow.onEach { items ->
            _uiState.value = _uiState.value.copy(
                displayedChannels = items,
                loadingMessage = null
            )
        }.launchIn(viewModelScope)
    }

    fun refreshContinueWatching() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            try {
                repository.getContinueWatching(tab).collect { progressList ->
                    val channels = progressList.map { it.channel }
                    _uiState.value = _uiState.value.copy(continueWatching = channels)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun backToPlaylists() {
        _uiState.value = _uiState.value.copy(selectedPlaylist = null)
    }
}