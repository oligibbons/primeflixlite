package com.example.primeflixlite.ui.home

import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram

data class HomeUiState(
    // Playlist Selection
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,

    // Navigation / Filtering
    val selectedTab: StreamType = StreamType.LIVE,
    val categories: List<String> = emptyList(), // ["All", "Sports", "News"...]
    val selectedCategory: String = "All",

    // Content
    val displayedChannels: List<ChannelWithProgram> = emptyList(), // Main Grid
    val continueWatching: List<Channel> = emptyList(),             // Top Row
    val favorites: List<Channel> = emptyList(),                    // Favorites Row

    // Status
    val isLoading: Boolean = false,
    val error: String? = null
)