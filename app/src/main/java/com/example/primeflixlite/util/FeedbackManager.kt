package com.example.primeflixlite.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor() {

    private val _state = MutableStateFlow<FeedbackState>(FeedbackState.Idle)
    val state = _state.asStateFlow()

    // Show a percentage (0.0 - 1.0)
    fun showLoading(task: String, type: String) {
        _state.value = FeedbackState.Loading(task, type, 0f)
    }

    fun updateProgress(progress: Float) {
        val current = _state.value
        if (current is FeedbackState.Loading) {
            _state.value = current.copy(progress = progress)
        }
    }

    // NEW: Show a specific count (e.g. "Saved 500 / 1000")
    // This fixes the "Too many arguments" build error
    fun updateCount(task: String, type: String, current: Int, total: Int) {
        _state.value = FeedbackState.ImportingCount(task, type, current, total)
    }

    fun showSuccess(message: String) {
        _state.value = FeedbackState.Success(message)
    }

    fun showError(message: String) {
        _state.value = FeedbackState.Error(message)
    }

    fun dismiss() {
        _state.value = FeedbackState.Idle
    }
}

sealed class FeedbackState {
    data object Idle : FeedbackState()

    // Network Phase: Just shows "Downloading..." animation or %
    data class Loading(val task: String, val type: String, val progress: Float) : FeedbackState()

    // Database Phase: Shows "500 / 2000"
    data class ImportingCount(val task: String, val type: String, val current: Int, val total: Int) : FeedbackState()

    data class Success(val message: String) : FeedbackState()
    data class Error(val message: String) : FeedbackState()
}