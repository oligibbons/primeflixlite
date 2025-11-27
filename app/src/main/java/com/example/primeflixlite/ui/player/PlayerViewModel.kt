package com.example.primeflixlite.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _playlistChannels = MutableStateFlow<List<Channel>>(emptyList())

    private val _currentProgram = MutableStateFlow<Programme?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    private val _overlayVisible = MutableStateFlow(true)
    val overlayVisible = _overlayVisible.asStateFlow()

    private val _resizeMode = MutableStateFlow(0)
    val resizeMode = _resizeMode.asStateFlow()

    private var overlayJob: Job? = null

    fun initialize(channel: Channel) {
        if (_currentChannel.value?.id == channel.id) return
        _currentChannel.value = channel
        loadEpgForChannel(channel)
        showOverlay()

        viewModelScope.launch {
            repository.getChannels(channel.playlistUrl).collect { channels ->
                _playlistChannels.value = channels
            }
        }
    }

    // NEW: Save Progress logic
    fun onPause(position: Long, duration: Long) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.saveProgress(channel.url, position, duration)
        }
    }

    private fun loadEpgForChannel(channel: Channel) {
        viewModelScope.launch {
            val epgId = channel.relationId ?: channel.title
            val program = repository.getCurrentProgram(epgId)
            _currentProgram.value = program
        }
    }

    fun nextChannel() {
        // ... (Zapping logic same as before)
        val list = _playlistChannels.value
        val current = _currentChannel.value ?: return
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val nextIndex = if (index + 1 < list.size) index + 1 else 0
            val nextChannel = list[nextIndex]
            initialize(nextChannel) // Re-use initialize to reset EPG
        }
    }

    fun prevChannel() {
        // ... (Zapping logic same as before)
        val list = _playlistChannels.value
        val current = _currentChannel.value ?: return
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val prevIndex = if (index - 1 >= 0) index - 1 else list.lastIndex
            val prevChannel = list[prevIndex]
            initialize(prevChannel)
        }
    }

    fun toggleResizeMode() {
        _resizeMode.value = (_resizeMode.value + 1) % 3
        showOverlay()
    }

    fun showOverlay() {
        _overlayVisible.value = true
        overlayJob?.cancel()
        overlayJob = viewModelScope.launch {
            delay(4000)
            _overlayVisible.value = false
        }
    }
}