package com.soundtag.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.soundtag.data.AnnotationData
import com.soundtag.data.LocationFix
import com.soundtag.service.RecordingService
import com.soundtag.service.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

sealed class UiState {
    data object Idle : UiState()
    data object Recording : UiState()
    data class Annotating(
        val startTime: ZonedDateTime,
        val location: LocationFix?,
        val tempFile: File,
        val durationSeconds: Long
    ) : UiState()
}

class RecordingViewModel : ViewModel() {

    val serviceState: StateFlow<RecordingState> = RecordingService.state
    val elapsedSeconds: StateFlow<Long> = RecordingService.elapsedSeconds

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _annotation = MutableStateFlow(AnnotationData())
    val annotation: StateFlow<AnnotationData> = _annotation.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
    }

    fun startRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        context.startForegroundService(intent)
        _uiState.value = UiState.Recording
    }

    fun stopRecording(context: Context) {
        // Capture current recording state before stopping
        val currentState = RecordingService.state.value
        val duration = RecordingService.elapsedSeconds.value

        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)

        if (currentState is RecordingState.Recording) {
            val timestamp = currentState.startTime.format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            )
            _annotation.value = AnnotationData(
                fileName = "misc_$timestamp"
            )
            _uiState.value = UiState.Annotating(
                startTime = currentState.startTime,
                location = currentState.location,
                tempFile = currentState.tempFile,
                durationSeconds = duration
            )
        } else {
            _uiState.value = UiState.Idle
        }
    }

    fun updateAnnotation(data: AnnotationData) {
        _annotation.value = data
    }

    fun saveRecording() {
        // Placeholder — actual file saving wired in next phase
        _annotation.value = AnnotationData()
        _uiState.value = UiState.Idle
    }

    fun dismissAnnotation() {
        _annotation.value = AnnotationData()
        _uiState.value = UiState.Idle
    }
}
