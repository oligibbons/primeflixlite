package com.example.primeflixlite.ui.home

import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.local.model.ChannelWithProgram
import com.example.primeflixlite.data.local.model.ChannelWithProgress

data class HomeUiState(
    // Global Data
    val playlists: List<Playlist> = emptyList(),

    // Smart Rows Data (New!)
    val continueWatching: List<ChannelWithProgress> = emptyList(),
    val recentLive: List<ChannelWithProgress> = emptyList(),

    // Navigation State
    val selectedPlaylist: Playlist? = null,
    val selectedTab: StreamType = StreamType.LIVE, // LIVE, MOVIE, or SERIES
    val selectedCategory: String = "All",

    // Main Grid Content
    val categories: List<String> = emptyList(), // Categories specific to the current Tab
    val displayedChannels: List<ChannelWithProgram> = emptyList(),

    val isLoading: Boolean = false
)