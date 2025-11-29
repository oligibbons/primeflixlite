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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var allChannels: List<ChannelWithProgram> = emptyList()
    private var refreshJob: Job? = null

    init {
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

    fun refreshContinueWatching() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            try {
                val progressList = repository.getContinueWatching(tab).first()
                val progressChannels = progressList.mapNotNull { progress ->
                    // FIX: Accessed 'channel.url' properly via the embedded object
                    allChannels.find { it.channel.url == progress.channel.url }?.channel
                }
                _uiState.value = _uiState.value.copy(continueWatching = progressChannels)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshContent() {
        val tab = _uiState.value.selectedTab

        val tabChannels = allChannels.filter { it.channel.type == tab.name }

        val categories = listOf("All") + tabChannels
            .map { it.channel.group }
            .distinct()
            .sorted()

        _uiState.value = _uiState.value.copy(
            categories = categories,
            isLoading = false
        )

        refreshContinueWatching()
        filterChannels()
    }

    private fun filterChannels() {
        val tab = _uiState.value.selectedTab
        val category = _uiState.value.selectedCategory

        val filtered = allChannels.filter { item ->
            item.channel.type == tab.name &&
                    (category == "All" || item.channel.group == category)
        }

        _uiState.value = _uiState.value.copy(displayedChannels = filtered)
    }

    fun backToPlaylists() {
        _uiState.value = _uiState.value.copy(selectedPlaylist = null)
    }

    fun addSamplePlaylist() {}
}