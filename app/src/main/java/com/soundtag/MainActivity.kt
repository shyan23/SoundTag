package com.soundtag

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundtag.service.RecordingState
import com.soundtag.ui.annotate.AnnotateSheetContent
import com.soundtag.ui.dashboard.DashboardScreen
import com.soundtag.ui.record.RecordScreen
import com.soundtag.ui.setup.FolderPickerDialog
import com.soundtag.ui.setup.SetupScreen
import com.soundtag.ui.theme.SoundTagBackground
import com.soundtag.ui.theme.SoundTagBorder
import com.soundtag.ui.theme.SoundTagGreen
import com.soundtag.ui.theme.SoundTagSurface
import com.soundtag.ui.theme.SoundTagSurfaceVariant
import com.soundtag.ui.theme.SoundTagTextPrimary
import com.soundtag.ui.theme.SoundTagTextSecondary
import com.soundtag.ui.theme.SoundTagTextTertiary
import com.soundtag.ui.theme.SoundTagTheme
import com.soundtag.viewmodel.RecordingViewModel
import com.soundtag.viewmodel.SaveResult
import com.soundtag.viewmodel.UiState
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SoundTagTheme {
                val vm: RecordingViewModel = viewModel()
                val context = LocalContext.current
                val uiState by vm.uiState.collectAsState()
                val serviceState by vm.serviceState.collectAsState()
                val elapsed by vm.elapsedSeconds.collectAsState()
                val annotation by vm.annotation.collectAsState()
                val hasPerms by vm.hasPermissions.collectAsState()
                val saveResult by vm.saveResult.collectAsState()
                val showSetup by vm.showSetup.collectAsState()
                val showDashboard by vm.showDashboard.collectAsState()
                val annotatorName by vm.annotatorName.collectAsState()
                val annotatorId by vm.annotatorId.collectAsState()
                val isDriveConnected by vm.isDriveConnected.collectAsState()
                val customFolderName by vm.customFolderName.collectAsState()
                val showFolderPicker by vm.showFolderPicker.collectAsState()
                val driveFolders by vm.driveFolders.collectAsState()
                val todayCount by vm.todayCount.collectAsState()
                val recordings by vm.recordings.collectAsState()
                val totalCount by vm.totalCount.collectAsState()
                val labelCounts by vm.labelCounts.collectAsState()
                val totalDuration by vm.totalDuration.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                val permLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results -> vm.setPermissionsGranted(results.values.all { it }) }

                val driveSignInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result -> vm.handleDriveSignIn(result.data) }

                LaunchedEffect(Unit) {
                    val perms = arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    val allGranted = perms.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) vm.setPermissionsGranted(true)
                    else permLauncher.launch(perms)
                }

                LaunchedEffect(saveResult) {
                    when (val result = saveResult) {
                        is SaveResult.Success -> {
                            val msg = if (result.uploaded) "Saved & uploaded ${result.filename}"
                            else "Saved ${result.filename}"
                            snackbarHostState.showSnackbar(msg)
                            vm.clearSaveResult()
                        }
                        is SaveResult.Error -> {
                            snackbarHostState.showSnackbar(result.message)
                            vm.clearSaveResult()
                        }
                        null -> {}
                    }
                }

                Scaffold(
                    containerColor = SoundTagBackground,
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = SoundTagSurface,
                                contentColor = SoundTagTextPrimary,
                                actionColor = SoundTagGreen
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when {
                            showSetup -> {
                                SetupScreen(
                                    name = annotatorName,
                                    annotatorId = annotatorId,
                                    isDriveConnected = isDriveConnected,
                                    customFolderName = customFolderName,
                                    onNameChange = { vm.updateName(it) },
                                    onIdChange = { vm.updateId(it) },
                                    onConnectDrive = { driveSignInLauncher.launch(vm.getSignInIntent()) },
                                    onChooseFolder = { vm.openFolderPicker() },
                                    onClearFolder = { vm.clearCustomFolder() },
                                    onStartCollecting = { vm.completeSetup() }
                                )

                                if (showFolderPicker) {
                                    FolderPickerDialog(
                                        folders = driveFolders,
                                        onSelect = { folder -> vm.selectFolder(folder.id, folder.name) },
                                        onUseDefault = {
                                            vm.clearCustomFolder()
                                            vm.closeFolderPicker()
                                        },
                                        onDismiss = { vm.closeFolderPicker() }
                                    )
                                }
                            }
                            showDashboard -> {
                                DashboardScreen(
                                    recordings = recordings,
                                    todayCount = todayCount,
                                    totalCount = totalCount,
                                    labelCounts = labelCounts,
                                    totalDuration = totalDuration,
                                    isDriveConnected = isDriveConnected,
                                    onBack = { vm.closeDashboard() },
                                    onSyncPending = { vm.syncPending() },
                                    onOpenMap = { lat, lng, label ->
                                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        if (intent.resolveActivity(packageManager) != null) {
                                            startActivity(intent)
                                        }
                                    }
                                )
                            }
                            !hasPerms -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SoundTagSurface, RoundedCornerShape(16.dp))
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Permissions Required", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoundTagTextPrimary)
                                        Text("SoundTag needs microphone and location access to record audio with GPS metadata.", fontSize = 14.sp, color = SoundTagTextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            else -> {
                                val location = (serviceState as? RecordingState.Recording)?.location

                                RecordScreen(
                                    isRecording = uiState is UiState.Recording || serviceState is RecordingState.Recording,
                                    elapsedSeconds = elapsed,
                                    location = location,
                                    annotatorId = annotatorId,
                                    todayCount = todayCount,
                                    onToggleRecording = {
                                        when (uiState) {
                                            is UiState.Recording -> vm.stopRecording(context)
                                            is UiState.Idle -> vm.startRecording(context)
                                            else -> {}
                                        }
                                    },
                                    onDashboardTap = { vm.openDashboard() }
                                )

                                if (uiState is UiState.Annotating || uiState is UiState.Saving) {
                                    val annotatingState = uiState as? UiState.Annotating
                                    if (annotatingState != null) {
                                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                                        ModalBottomSheet(
                                            onDismissRequest = { vm.dismissAnnotation() },
                                            sheetState = sheetState,
                                            containerColor = SoundTagSurfaceVariant,
                                            dragHandle = null,
                                            modifier = Modifier.fillMaxHeight(0.6f)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .height(4.dp)
                                                        .fillMaxWidth(0.1f)
                                                        .background(SoundTagBorder, RoundedCornerShape(2.dp))
                                                )
                                            }
                                            AnnotateSheetContent(
                                                annotation = annotation,
                                                durationSeconds = annotatingState.durationSeconds,
                                                recordingTime = annotatingState.startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                                                location = annotatingState.location,
                                                onAnnotationChange = { vm.updateAnnotation(it) },
                                                onSave = { vm.saveRecording(context) },
                                                isSaving = uiState is UiState.Saving,
                                                isDriveConnected = isDriveConnected,
                                                annotatorId = annotatorId
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
