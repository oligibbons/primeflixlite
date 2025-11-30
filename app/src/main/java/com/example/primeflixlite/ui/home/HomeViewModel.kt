package com.example.primeflixlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var contentJob: Job? = null
    private var groupsJob: Job? = null

    init {
        loadPlaylists()
        observeFavorites()
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
        refreshContent()
    }

    fun selectTab(tab: StreamType) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, selectedCategory = "All")
        refreshContent()
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadChannels() // Reload channels filter, no need to reload groups
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
                val allGroups = listOf("All") + groups
                _uiState.value = _uiState.value.copy(categories = allGroups)
            }
            .launchIn(viewModelScope)
    }

    private fun loadChannels() {
        val playlist = _uiState.value.selectedPlaylist ?: return
        val type = _uiState.value.selectedTab
        val category = _uiState.value.selectedCategory

        contentJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)

        if (type == StreamType.LIVE) {
            // Live Channels have EPG Join
            contentJob = repository.getLiveChannels(playlist.url, category)
                .onEach { items ->
                    _uiState.value = _uiState.value.copy(displayedChannels = items, isLoading = false)
                }
                .launchIn(viewModelScope)
        } else {
            // VOD (Movies/Series) - Simple Fetch, no EPG
            contentJob = repository.getVodChannels(playlist.url, type.name, category)
                .map { channels ->
                    // Map simple Channel to ChannelWithProgram (program = null)
                    channels.map { ChannelWithProgram(it, null) }
                }
                .onEach { items ->
                    _uiState.value = _uiState.value.copy(displayedChannels = items, isLoading = false)
                }
                .launchIn(viewModelScope)
        }
    }

    fun refreshContinueWatching() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            try {
                repository.getContinueWatching(tab).collect { progressList ->
                    // Extract channel objects from the Join
                    val channels = progressList.map { it.channel }
                    _uiState.value = _uiState.value.copy(continueWatching = channels)
                }
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun backToPlaylists() {
        _uiState.value = _uiState.value.copy(selectedPlaylist = null)
    }

    fun addSamplePlaylist() {}
}