package com.soundtag.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundtag.data.AnnotationData
import com.soundtag.data.DriveUploader
import com.soundtag.data.FileSaver
import com.soundtag.data.LocationFix
import com.soundtag.data.MetadataWriter
import com.soundtag.data.RecordingEntry
import com.soundtag.data.RecordingRepository
import com.soundtag.data.UploadResult
import com.soundtag.data.UserPreferences
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

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val repo = RecordingRepository(application)

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

    // Setup
    private val _isSetupComplete = MutableStateFlow(prefs.isSetupComplete)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _annotatorName = MutableStateFlow(prefs.annotatorName)
    val annotatorName: StateFlow<String> = _annotatorName.asStateFlow()

    private val _annotatorId = MutableStateFlow(prefs.annotatorId)
    val annotatorId: StateFlow<String> = _annotatorId.asStateFlow()

    private val _isDriveConnected = MutableStateFlow(DriveUploader.isSignedIn(application))
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected.asStateFlow()

    private val _showSetup = MutableStateFlow(!prefs.isSetupComplete)
    val showSetup: StateFlow<Boolean> = _showSetup.asStateFlow()

    // Dashboard
    private val _showDashboard = MutableStateFlow(false)
    val showDashboard: StateFlow<Boolean> = _showDashboard.asStateFlow()

    private val _recordings = MutableStateFlow(repo.getAll())
    val recordings: StateFlow<List<RecordingEntry>> = _recordings.asStateFlow()

    private val _todayCount = MutableStateFlow(repo.getTodayCount())
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    private val _totalCount = MutableStateFlow(repo.getTotalCount())
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _labelCounts = MutableStateFlow(repo.getCountByLabel())
    val labelCounts: StateFlow<Map<String, Int>> = _labelCounts.asStateFlow()

    private val _totalDuration = MutableStateFlow(repo.getTotalDuration())
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    // Setup
    fun updateName(name: String) { _annotatorName.value = name }
    fun updateId(id: String) { _annotatorId.value = id }

    fun completeSetup() {
        prefs.annotatorName = _annotatorName.value
        prefs.annotatorId = _annotatorId.value
        prefs.isSetupComplete = true
        _isSetupComplete.value = true
        _showSetup.value = false
    }

    fun openSetup() { _showSetup.value = true }

    // Dashboard
    fun openDashboard() {
        refreshDashboardData()
        _showDashboard.value = true
    }

    fun closeDashboard() { _showDashboard.value = false }

    private fun refreshDashboardData() {
        _recordings.value = repo.getAll()
        _todayCount.value = repo.getTodayCount()
        _totalCount.value = repo.getTotalCount()
        _labelCounts.value = repo.getCountByLabel()
        _totalDuration.value = repo.getTotalDuration()
    }

    fun syncPending() {
        if (!_isDriveConnected.value) return
        val pending = repo.getPending()
        viewModelScope.launch {
            pending.forEach { entry ->
                // We can't re-upload without the original file, so just mark as noted
                // In a real app, we'd queue the files. For now this is a placeholder.
            }
        }
    }

    // Drive
    fun getSignInIntent(): Intent = DriveUploader.getSignInIntent(getApplication())

    fun handleDriveSignIn(data: Intent?) {
        val success = DriveUploader.handleSignInResult(data)
        _isDriveConnected.value = success
    }

    // Recording
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
            _annotation.value = AnnotationData(fileName = "misc_$timestamp")
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

    fun updateAnnotation(data: AnnotationData) { _annotation.value = data }

    fun saveRecording(context: Context) {
        val state = _uiState.value
        if (state !is UiState.Annotating) return

        val currentAnnotation = _annotation.value
        val aid = _annotatorId.value.ifEmpty { "unknown" }
        val filename = currentAnnotation.fileName.ifEmpty {
            "misc_${state.startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}"
        }

        _uiState.value = UiState.Saving

        viewModelScope.launch {
            val json = MetadataWriter.buildJson(
                filename = filename,
                annotation = currentAnnotation,
                startTime = state.startTime,
                location = state.location,
                durationSeconds = state.durationSeconds,
                annotatorId = aid
            )

            // Upload to Drive first if connected
            var uploaded = false
            if (_isDriveConnected.value) {
                val result = DriveUploader.uploadRecording(
                    context = context,
                    audioFile = state.tempFile,
                    jsonContent = json,
                    filename = filename,
                    annotatorId = aid
                )
                uploaded = result is UploadResult.Success
            }

            // Save locally
            val uri = FileSaver.saveRecording(
                context = context,
                audioFile = state.tempFile,
                desiredName = filename,
                jsonContent = json
            )

            // Track in repository
            val uploadStatus = when {
                uploaded -> "uploaded"
                _isDriveConnected.value -> "failed"
                else -> "local"
            }
            repo.addRecording(
                RecordingEntry(
                    filename = filename,
                    noiseType = currentAnnotation.noiseType.ifEmpty { "misc" },
                    timestamp = state.startTime.toInstant().toEpochMilli(),
                    durationSeconds = state.durationSeconds,
                    uploadStatus = uploadStatus
                )
            )
            refreshDashboardData()

            if (uri != null) {
                _saveResult.value = SaveResult.Success("$filename.m4a", uploaded = uploaded)
            } else {
                _saveResult.value = SaveResult.Error("Failed to save recording")
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
