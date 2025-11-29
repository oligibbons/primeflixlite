package com.example.primeflixlite.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor() {

    private val _state = MutableStateFlow<FeedbackState>(FeedbackState.Idle)
    val state = _state.asStateFlow()

    fun showLoading(task: String, type: String) {
        _state.value = FeedbackState.Loading(task, type, 0f)
    }

    fun updateProgress(progress: Float) {
        val current = _state.value
        if (current is FeedbackState.Loading) {
            _state.value = current.copy(progress = progress)
        }
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
    data class Loading(val task: String, val type: String, val progress: Float) : FeedbackState() // e.g. task="Syncing", type="Movies"
    data class Success(val message: String) : FeedbackState()
    data class Error(val message: String) : FeedbackState()
}