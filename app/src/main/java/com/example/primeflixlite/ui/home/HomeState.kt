package com.example.primeflixlite.ui.home

import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram

data class HomeUiState(
    val isLoading: Boolean = false,
    // [Config] Default to SERIES as requested
    val selectedTab: StreamType = StreamType.SERIES,

    val selectedPlaylist: Playlist? = null,
    val playlists: List<Playlist> = emptyList(),

    // The list of filter chips (Smart Categories + Standard Groups)
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",

    // The main grid content
    val displayedChannels: List<ChannelWithProgram> = emptyList(),

    // Separate lists kept in state for quick access/badges if needed
    val favorites: List<Channel> = emptyList(),
    val continueWatching: List<Channel> = emptyList(),

    val error: String? = null
)