package com.soundtag.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundtag.data.AnnotationData
import com.soundtag.data.AudioPlayer
import com.soundtag.data.DbStats
import com.soundtag.data.DriveFolder
import com.soundtag.data.DriveUploader
import com.soundtag.data.FileSaver
import com.soundtag.data.LocationFix
import com.soundtag.data.MetadataWriter
import com.soundtag.data.RecordingEntry
import com.soundtag.data.RecordingRepository
import com.soundtag.data.PlaybackState
import com.soundtag.data.UploadQueueManager
import com.soundtag.data.UploadResult
import com.soundtag.data.UserPreferences
import com.soundtag.service.UploadWorker
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
        val durationSeconds: Long,
        val dbStats: DbStats? = null
    ) : UiState()
    data class Saving(
        val startTime: ZonedDateTime,
        val location: LocationFix?,
        val durationSeconds: Long,
        val dbStats: DbStats? = null
    ) : UiState()
}

sealed class SaveResult {
    data class Success(val filename: String, val uploaded: Boolean) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val repo = RecordingRepository(application)
    private val uploadQueue = UploadQueueManager(application)
    val audioPlayer = AudioPlayer()

    val serviceState: StateFlow<RecordingState> = RecordingService.state
    val elapsedSeconds: StateFlow<Long> = RecordingService.elapsedSeconds
    val currentDb: StateFlow<Float> = RecordingService.currentDb
    val isCalibrating: StateFlow<Boolean> = RecordingService.isCalibrating

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

    private val _customFolderName = MutableStateFlow(prefs.customDriveFolderName)
    val customFolderName: StateFlow<String> = _customFolderName.asStateFlow()

    private val _driveFolders = MutableStateFlow<List<DriveFolder>?>(null)
    val driveFolders: StateFlow<List<DriveFolder>?> = _driveFolders.asStateFlow()

    private val _showFolderPicker = MutableStateFlow(false)
    val showFolderPicker: StateFlow<Boolean> = _showFolderPicker.asStateFlow()

    // Breadcrumb: list of (id, name) pairs for navigation
    private val _folderPath = MutableStateFlow<List<Pair<String?, String>>>(listOf(null to "My Drive"))
    val folderPath: StateFlow<List<Pair<String?, String>>> = _folderPath.asStateFlow()

    // Recording detail
    private val _selectedRecording = MutableStateFlow<RecordingEntry?>(null)
    val selectedRecording: StateFlow<RecordingEntry?> = _selectedRecording.asStateFlow()

    private val _selectedRecordingJson = MutableStateFlow<Map<String, Any?>?>(null)
    val selectedRecordingJson: StateFlow<Map<String, Any?>?> = _selectedRecordingJson.asStateFlow()

    private val _showRecordingDetail = MutableStateFlow(false)
    val showRecordingDetail: StateFlow<Boolean> = _showRecordingDetail.asStateFlow()

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

    // Folder picker
    fun openFolderPicker() {
        _folderPath.value = listOf(null to "My Drive")
        _driveFolders.value = null
        _showFolderPicker.value = true
        viewModelScope.launch {
            _driveFolders.value = DriveUploader.listFolders(getApplication(), parentId = null)
        }
    }

    fun closeFolderPicker() {
        _showFolderPicker.value = false
        _folderPath.value = listOf(null to "My Drive")
    }

    fun browseIntoFolder(folderId: String, folderName: String) {
        _folderPath.value = _folderPath.value + (folderId to folderName)
        _driveFolders.value = null
        viewModelScope.launch {
            _driveFolders.value = DriveUploader.listFolders(getApplication(), parentId = folderId)
        }
    }

    fun browseBack() {
        val path = _folderPath.value
        if (path.size <= 1) {
            closeFolderPicker()
            return
        }
        _folderPath.value = path.dropLast(1)
        _driveFolders.value = null
        val parentId = _folderPath.value.last().first
        viewModelScope.launch {
            _driveFolders.value = DriveUploader.listFolders(getApplication(), parentId = parentId)
        }
    }

    fun selectFolder(id: String, name: String) {
        prefs.customDriveFolderId = id
        prefs.customDriveFolderName = name
        _customFolderName.value = name
        _showFolderPicker.value = false
    }

    fun clearCustomFolder() {
        prefs.customDriveFolderId = ""
        prefs.customDriveFolderName = ""
        _customFolderName.value = ""
    }

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

    fun openRecordingDetail(entry: RecordingEntry, context: Context) {
        _selectedRecording.value = entry
        _showRecordingDetail.value = true
        viewModelScope.launch {
            val jsonStr = repo.getJsonForRecording(context, entry.filename)
            if (jsonStr != null) {
                try {
                    val obj = org.json.JSONObject(jsonStr)
                    _selectedRecordingJson.value = jsonToMap(obj)
                } catch (_: Exception) {
                    _selectedRecordingJson.value = null
                }
            }
        }
    }

    fun closeRecordingDetail() {
        audioPlayer.stop()
        _showRecordingDetail.value = false
        _selectedRecording.value = null
        _selectedRecordingJson.value = null
    }

    private fun jsonToMap(obj: org.json.JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        obj.keys().forEach { key ->
            val value = obj.get(key)
            map[key] = when (value) {
                is org.json.JSONObject -> jsonToMap(value)
                org.json.JSONObject.NULL -> null
                else -> value
            }
        }
        return map
    }

    fun syncPending() {
        UploadWorker.enqueue(getApplication())
    }

    fun retryUpload(filename: String) {
        repo.updateUploadStatus(filename, "pending")
        refreshDashboardData()
        UploadWorker.enqueue(getApplication())
    }

    fun playAnnotationAudio() {
        val state = _uiState.value
        if (state is UiState.Annotating && state.tempFile.exists()) {
            audioPlayer.playFile(state.tempFile, viewModelScope)
        }
    }

    fun playDashboardRecording(context: Context, filename: String) {
        // Try to find the file in Music/SoundTag/
        val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.Media._ID), selection, arrayOf("$filename.m4a"), null)
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(0)
                val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                audioPlayer.playUri(context, contentUri, filename, viewModelScope)
            }
        }
    }

    fun deleteRecording(filename: String) {
        repo.deleteRecording(filename)
        uploadQueue.removePending(filename)
        refreshDashboardData()
    }

    // Drive
    fun getSignInIntent(): Intent = DriveUploader.getSignInIntent(getApplication())

    fun handleDriveSignIn(data: Intent?) {
        val success = DriveUploader.handleSignInResult(data)
        _isDriveConnected.value = success
    }

    fun signOutDrive() {
        DriveUploader.signOut(getApplication()) {
            _isDriveConnected.value = false
            clearCustomFolder()
        }
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
        val capturedDbStats = RecordingService.dbStats.value

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
                durationSeconds = duration,
                dbStats = capturedDbStats
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

        val capturedStats = state.dbStats
        audioPlayer.stop()
        _uiState.value = UiState.Saving(
            startTime = state.startTime,
            location = state.location,
            durationSeconds = state.durationSeconds,
            dbStats = capturedStats
        )

        viewModelScope.launch {
            val json = MetadataWriter.buildJson(
                filename = filename,
                annotation = currentAnnotation,
                startTime = state.startTime,
                location = state.location,
                durationSeconds = state.durationSeconds,
                annotatorId = aid,
                dbStats = capturedStats
            )

            // Upload to Drive first if connected (before temp file is deleted)
            val customFolderId = prefs.customDriveFolderId.ifEmpty { null }
            var uploaded = false
            if (_isDriveConnected.value) {
                val result = DriveUploader.uploadRecording(
                    context = context,
                    audioFile = state.tempFile,
                    jsonContent = json,
                    filename = filename,
                    annotatorId = aid,
                    customFolderId = customFolderId
                )
                uploaded = result is UploadResult.Success
            }

            // Queue for retry if not uploaded and Drive is configured
            if (!uploaded && _isDriveConnected.value && state.tempFile.exists()) {
                uploadQueue.queueForUpload(state.tempFile, json, filename, aid, customFolderId ?: "")
                UploadWorker.enqueue(context)
            }

            // Save locally (this deletes the temp file)
            val uri = FileSaver.saveRecording(
                context = context,
                audioFile = state.tempFile,
                desiredName = filename,
                jsonContent = json
            )

            // Track in repository with location
            val uploadStatus = when {
                uploaded -> "uploaded"
                _isDriveConnected.value -> "pending"
                else -> "local"
            }
            repo.addRecording(
                RecordingEntry(
                    filename = filename,
                    noiseType = currentAnnotation.noiseType.ifEmpty { "misc" },
                    timestamp = state.startTime.toInstant().toEpochMilli(),
                    durationSeconds = state.durationSeconds,
                    uploadStatus = uploadStatus,
                    latitude = state.location?.latitude,
                    longitude = state.location?.longitude,
                    avgDb = capturedStats?.avgDb
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
        audioPlayer.stop()
        _annotation.value = AnnotationData()
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
