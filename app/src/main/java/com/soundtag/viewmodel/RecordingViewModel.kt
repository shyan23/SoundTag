package com.soundtag.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundtag.data.AnnotationData
import com.soundtag.data.FileSaver
import com.soundtag.data.FirebaseUploader
import com.soundtag.data.LocationFix
import com.soundtag.data.MetadataWriter
import com.soundtag.data.UploadStatus
import com.soundtag.service.RecordingService
import com.soundtag.service.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    data object Saving : UiState()
}

sealed class SaveResult {
    data class Success(val filename: String, val uploaded: Boolean) : SaveResult()
    data class Error(val message: String) : SaveResult()
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

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun startRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        context.startForegroundService(intent)
        _uiState.value = UiState.Recording
    }

    fun stopRecording(context: Context) {
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

    fun saveRecording(context: Context) {
        val state = _uiState.value
        if (state !is UiState.Annotating) return

        val currentAnnotation = _annotation.value
        val filename = currentAnnotation.fileName.ifEmpty {
            "misc_${state.startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}"
        }

        _uiState.value = UiState.Saving
        _uploadStatus.value = UploadStatus.Uploading(0)

        viewModelScope.launch {
            val json = MetadataWriter.buildJson(
                filename = filename,
                annotation = currentAnnotation,
                startTime = state.startTime,
                location = state.location,
                durationSeconds = state.durationSeconds
            )

            // Upload to Firebase Storage FIRST (before local save deletes temp file)
            _uploadStatus.value = UploadStatus.Uploading(50)
            val uploadResult = FirebaseUploader.uploadRecording(
                audioFile = state.tempFile,
                jsonContent = json,
                filename = filename
            )
            val uploaded = uploadResult is UploadStatus.Success
            _uploadStatus.value = uploadResult

            // Save locally (this deletes the temp file)
            val uri = FileSaver.saveRecording(
                context = context,
                audioFile = state.tempFile,
                desiredName = filename,
                jsonContent = json
            )

            if (uri != null) {
                _saveResult.value = SaveResult.Success("$filename.m4a", uploaded = uploaded)
            } else {
                _saveResult.value = SaveResult.Error("Failed to save recording locally")
            }

            _annotation.value = AnnotationData()
            _uiState.value = UiState.Idle
        }
    }

    fun dismissAnnotation() {
        _annotation.value = AnnotationData()
        _uiState.value = UiState.Idle
    }
}
