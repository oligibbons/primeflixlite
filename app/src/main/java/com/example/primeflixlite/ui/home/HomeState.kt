package com.example.primeflixlite.ui.home

import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram

data class HomeUiState(
    // Global Data
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,

    // Dashboard Data
    val selectedTab: StreamType = StreamType.LIVE,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",

    // Content Lists
    val displayedChannels: List<ChannelWithProgram> = emptyList(),
    val continueWatching: List<Channel> = emptyList(),

    // NEW: "My List" Row Data
    val favorites: List<Channel> = emptyList(),

    // Status
    val isLoading: Boolean = false,
    val error: String? = null
)