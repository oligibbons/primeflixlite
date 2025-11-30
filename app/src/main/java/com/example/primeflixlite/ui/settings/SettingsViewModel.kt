package com.example.primeflixlite.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.playlists.collect { list ->
                _uiState.value = _uiState.value.copy(playlists = list)
            }
        }
    }

    fun syncPlaylist(playlist: Playlist) {
        // Updated for Background Sync:
        // We trigger the sync via repository (which now uses Application Scope).
        // We do NOT wait for it to finish here.
        // We just show a quick toast saying "Sync Started".

        repository.syncPlaylist(playlist)

        _uiState.value = _uiState.value.copy(message = "Background Sync Started...")
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.deletePlaylist(playlist)
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Playlist Removed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Delete Failed: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}