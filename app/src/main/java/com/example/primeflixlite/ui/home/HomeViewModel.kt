package com.example.primeflixlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // In-Memory Cache (Separated for Performance)
    private var liveList: List<ChannelWithProgram> = emptyList()
    private var movieList: List<ChannelWithProgram> = emptyList()
    private var seriesList: List<ChannelWithProgram> = emptyList()

    init {
        // 1. Load Playlists
        viewModelScope.launch {
            repository.playlists.collectLatest { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
                if (_uiState.value.selectedPlaylist == null && playlists.isNotEmpty()) {
                    selectPlaylist(playlists.first())
                }
            }
        }

        // 2. Load History (Recent Live Channels)
        viewModelScope.launch {
            repository.getRecentChannels().collectLatest { recent ->
                _uiState.value = _uiState.value.copy(recentLive = recent)
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _uiState.value = _uiState.value.copy(
            selectedPlaylist = playlist,
            isLoading = true
        )

        // Reset Caches
        liveList = emptyList()
        movieList = emptyList()
        seriesList = emptyList()

        viewModelScope.launch {
            // Fetch everything (Live + VOD)
            repository.getChannelsWithEpg(playlist.url).collect { allChannels ->

                // Sort into buckets on background thread
                withContext(Dispatchers.Default) {
                    liveList = allChannels.filter { it.channel.type == StreamType.LIVE }
                    movieList = allChannels.filter { it.channel.type == StreamType.MOVIE }
                    seriesList = allChannels.filter { it.channel.type == StreamType.SERIES }
                }

                // Initial Load: Show LIVE tab by default
                refreshTabContent(_uiState.value.selectedTab)

                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectTab(tab: StreamType) {
        if (_uiState.value.selectedTab == tab) return

        // Update Tab State
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        refreshTabContent(tab)

        // Observe "Continue Watching" for this specific tab (Movies/Series)
        viewModelScope.launch {
            repository.getContinueWatching(tab).collectLatest { history ->
                _uiState.value = _uiState.value.copy(continueWatching = history)
            }
        }
    }

    private fun refreshTabContent(tab: StreamType) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Pick the correct list
            val sourceList = when(tab) {
                StreamType.LIVE -> liveList
                StreamType.MOVIE -> movieList
                StreamType.SERIES -> seriesList
            }

            // 2. Extract Categories
            val cats = sourceList.map { it.channel.group }.distinct().sorted().toMutableList()
            cats.add(0, "All")

            // 3. Update UI
            _uiState.value = _uiState.value.copy(
                categories = cats,
                selectedCategory = "All",
                displayedChannels = sourceList
            )
        }
    }

    fun selectCategory(category: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val tab = _uiState.value.selectedTab
            val sourceList = when(tab) {
                StreamType.LIVE -> liveList
                StreamType.MOVIE -> movieList
                StreamType.SERIES -> seriesList
            }

            val filtered = if (category == "All") sourceList else sourceList.filter { it.channel.group == category }

            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                displayedChannels = filtered
            )
        }
    }

    fun backToPlaylists() {
        _uiState.value = _uiState.value.copy(selectedPlaylist = null)
    }

    fun addSamplePlaylist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.addPlaylist(
                title = "Samsung TV Plus (US)",
                url = "https://i.mjh.nz/SamsungTVPlus/us.m3u8",
                source = DataSource.M3U
            )
        }
    }

    fun syncCurrentPlaylist() {
        val current = _uiState.value.selectedPlaylist ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.syncPlaylist(current)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}