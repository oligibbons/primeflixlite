package com.example.primeflixlite.util

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // SupervisorJob ensures a crash in one UI update doesn't kill the whole scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<FeedbackState>(FeedbackState.Idle)
    val state = _state.asStateFlow()

    fun showLoading(task: String, type: String) {
        _state.value = FeedbackState.Loading(task, type, 0f)
    }

    // Removed 'suspend' to allow calling from OkHttp Callbacks
    fun updateProgress(progress: Float) {
        val current = _state.value
        if (current is FeedbackState.Loading) {
            _state.value = current.copy(progress = progress)
        }
    }

    fun updateCount(task: String, type: String, current: Int, total: Int) {
        _state.value = FeedbackState.ImportingCount(task, type, current, total)
    }

    // Removed 'suspend', now launches on Main Scope
    fun showSuccess(message: String) {
        scope.launch {
            _state.value = FeedbackState.Success(message)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Removed 'suspend', now launches on Main Scope
    fun showError(message: String) {
        scope.launch {
            _state.value = FeedbackState.Error(message)
            Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
        }
    }

    fun dismiss() {
        _state.value = FeedbackState.Idle
    }
}

sealed class FeedbackState {
    data object Idle : FeedbackState()
    data class Loading(val task: String, val type: String, val progress: Float) : FeedbackState()
    data class ImportingCount(val task: String, val type: String, val current: Int, val total: Int) : FeedbackState()
    data class Success(val message: String) : FeedbackState()
    data class Error(val message: String) : FeedbackState()
}