package com.example.primeflixlite.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    // --- State ---
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<Programme?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    // Playlist context for zapping
    private var playlistChannels: List<Channel> = emptyList()

    private val _resizeMode = MutableStateFlow(0) // 0: Fit, 1: Fill, 2: Zoom
    val resizeMode = _resizeMode.asStateFlow()

    // Progress tracking job
    private var progressJob: Job? = null

    // --- Initialization ---
    fun initialize(channel: Channel) {
        // If it's the same channel URL, we just update the object (in case favorite status changed)
        // but we don't reset the player or EPG reload unless necessary.
        if (_currentChannel.value?.url == channel.url) {
            _currentChannel.value = channel
            return
        }

        _currentChannel.value = channel
        loadEpgForChannel(channel)

        // Fetch sibling channels for zapping context
        viewModelScope.launch {
            // We collect once to get the list for zapping
            repository.getChannels(channel.playlistUrl).collect { channels ->
                playlistChannels = channels
            }
        }
    }

    // --- Favorites Logic (NEW) ---
    fun toggleFavorite() {
        val current = _currentChannel.value ?: return
        viewModelScope.launch {
            // 1. Update Database
            repository.toggleFavorite(current)

            // 2. Update Local State immediately for UI feedback
            // We create a copy of the channel with the toggled boolean
            _currentChannel.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    // --- Zapping Logic ---
    fun nextChannel() {
        val current = _currentChannel.value ?: return
        if (playlistChannels.isEmpty()) return

        val index = playlistChannels.indexOfFirst { it.url == current.url }
        if (index != -1) {
            // Loop back to start if at end
            val nextIndex = if (index + 1 < playlistChannels.size) index + 1 else 0
            val nextChannel = playlistChannels[nextIndex]
            switchChannel(nextChannel)
        }
    }

    fun prevChannel() {
        val current = _currentChannel.value ?: return
        if (playlistChannels.isEmpty()) return

        val index = playlistChannels.indexOfFirst { it.url == current.url }
        if (index != -1) {
            // Loop to end if at start
            val prevIndex = if (index - 1 >= 0) index - 1 else playlistChannels.lastIndex
            val prevChannel = playlistChannels[prevIndex]
            switchChannel(prevChannel)
        }
    }

    private fun switchChannel(channel: Channel) {
        _currentChannel.value = channel
        loadEpgForChannel(channel)
    }

    // --- EPG Logic ---
    private fun loadEpgForChannel(channel: Channel) {
        viewModelScope.launch {
            // Try relationId (XMLTV ID) first, then title fallback
            val epgId = channel.relationId ?: channel.title
            val program = repository.getCurrentProgram(epgId)
            _currentProgram.value = program
        }
    }

    // --- Player Logic ---
    fun toggleResizeMode() {
        // Cycle: 0 -> 1 -> 2 -> 0
        _resizeMode.value = (_resizeMode.value + 1) % 3
    }

    fun startProgressTracking(player: Player) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val channel = _currentChannel.value
                if (player.isPlaying && channel != null && player.duration > 0) {
                    repository.saveProgress(
                        url = channel.url,
                        position = player.currentPosition,
                        duration = player.duration
                    )
                }
                delay(10_000) // Save every 10s
            }
        }
    }

    fun saveFinalProgress(position: Long, duration: Long) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.saveProgress(channel.url, position, duration)
        }
    }
}