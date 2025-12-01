package com.example.primeflixlite.ui.home

import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram

data class HomeUiState(
    // UI Loading State - Null means "Not Loading"
    // We use a String so we can tell the user exactly what we are doing
    val loadingMessage: String? = null,

    // Data Lists
    val playlists: List<Playlist> = emptyList(),
    val categories: List<String> = emptyList(),

    // The main grid content
    val displayedChannels: List<ChannelWithProgram> = emptyList(),

    // "My List" and History
    val favorites: List<Channel> = emptyList(),
    val continueWatching: List<Channel> = emptyList(),

    // Selections
    val selectedPlaylist: Playlist? = null,
    val selectedTab: StreamType = StreamType.LIVE,
    val selectedCategory: String = "All",

    // SPOTLIGHT STATE (New)
    // The channel currently focused in the grid.
    // This drives the large hero background image at the top of the Home Screen.
    val spotlightChannel: Channel? = null
) {
    // Helper for simple boolean checks
    val isLoading: Boolean
        get() = loadingMessage != null
}