package com.example.primeflixlite.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.local.model.ChannelWithProgram // Ensure this is imported
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuideUiState(
    val channels: List<ChannelWithProgram> = emptyList(),
    val currentGroup: String = "All",
    val isLoading: Boolean = true
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState = _uiState.asStateFlow()

    private var contentJob: Job? = null
    private var clockJob: Job? = null

    init {
        startClock()
    }

    fun loadGuide(playlist: Playlist, group: String) {
        _uiState.value = _uiState.value.copy(currentGroup = group, isLoading = true)

        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            repository.getLiveChannels(playlist.url, group)
                .collect { items ->
                    _uiState.value = _uiState.value.copy(
                        channels = items,
                        isLoading = false
                    )
                }
        }
    }

    private fun startClock() {
        clockJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                val current = _uiState.value
                _uiState.value = current.copy()
            }
        }
    }
}