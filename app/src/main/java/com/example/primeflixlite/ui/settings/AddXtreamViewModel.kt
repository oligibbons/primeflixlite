package com.example.primeflixlite.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeflixlite.data.local.entity.DataSource
import com.example.primeflixlite.data.repository.PrimeFlixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddXtreamViewModel @Inject constructor(
    private val repository: PrimeFlixRepository
) : ViewModel() {

    var serverUrl by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun validateAndSave() {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "All fields are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                // Formatting the URL so XtreamInput.decodeFromPlaylistUrl can read it.
                // Assuming standard format: http://url.com
                // We will pack the credentials into a format your XtreamInput likely expects
                // OR we pass them via a specialized format if your Input class supports it.
                // For now, let's assume we format it as a combined string:
                // "serverUrl|username|password"
                // NOTE: You must update XtreamInput.decodeFromPlaylistUrl to split this string!
                val combinedUrl = "${cleanUrl(serverUrl)}|$username|$password"

                // We use the server domain as the Playlist Title initially
                val title = try {
                    java.net.URI(serverUrl).host ?: "Xtream Playlist"
                } catch (e: Exception) { "Xtream Playlist" }

                // The repository's syncPlaylist() will attempt to fetch data.
                // If it fails, it throws an exception, which we catch here.
                repository.addPlaylist(
                    title = title,
                    url = combinedUrl,
                    source = DataSource.Xtream
                )

                _uiState.value = LoginUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = "Connection Failed: ${e.message}")
            }
        }
    }

    private fun cleanUrl(url: String): String {
        return url.trim().removeSuffix("/")
    }

    fun resetState() {
        _uiState.value = LoginUiState()
        serverUrl = ""
        username = ""
        password = ""
    }
}