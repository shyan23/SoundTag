package com.soundtag.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.soundtag.MainActivity
import com.soundtag.R
import com.soundtag.SoundTagApp
import com.soundtag.data.DbStats
import com.soundtag.data.LocationFix
import com.soundtag.data.LocationHelper
import kotlin.math.log10
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZonedDateTime

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Recording(
        val startTime: ZonedDateTime,
        val location: LocationFix?,
        val tempFile: File
    ) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class RecordingService : LifecycleService() {

    companion object {
        private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val state: StateFlow<RecordingState> = _state.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _currentDb = MutableStateFlow(0f)
        val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

        private val _dbStats = MutableStateFlow<DbStats?>(null)
        val dbStats: StateFlow<DbStats?> = _dbStats.asStateFlow()

        private val _isCalibrating = MutableStateFlow(false)
        val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

        private const val CALIBRATION_SECONDS = 3L

        const val ACTION_START = "com.soundtag.ACTION_START"
        const val ACTION_STOP = "com.soundtag.ACTION_STOP"

        private const val NOTIFICATION_ID = 1
        private const val MAX_DURATION_SECONDS = 300L
        private const val MIN_DURATION_SECONDS = 5L
    }

    private val dbSamples = mutableListOf<Float>()
    private val calibrationSamples = mutableListOf<Float>()
    private var noiseFloorDb = 0f
    private var mediaRecorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tickerJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var audioManager: AudioManager
    private lateinit var locationHelper: LocationHelper

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        locationHelper = LocationHelper(application)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (_state.value is RecordingState.Recording) return

        // 1. Start foreground IMMEDIATELY (within 5s mandate)
        val notification = buildNotification("Starting...")
        startForeground(NOTIFICATION_ID, notification)

        // 2. Acquire wake lock
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundTag::Recording").apply {
            acquire(6 * 60 * 1000L) // 6 min safety timeout
        }

        // 3. Fetch location + start recorder in coroutine
        lifecycleScope.launch {
            val locationFix = locationHelper.getLocation()

            // 4. Request audio focus
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopRecording()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)

            // 5. Configure MediaRecorder
            val tempFile = File(cacheDir, "soundtag_temp.m4a")
            try {
                val recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this@RecordingService) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioChannels(1)
                    setOutputFile(tempFile.absolutePath)
                    setOnErrorListener { _, what, extra ->
                        _state.value = RecordingState.Error("MediaRecorder error: $what ($extra)")
                        stopRecording()
                    }
                    prepare()
                    start()
                }
                mediaRecorder = recorder

                val startTime = ZonedDateTime.now()
                _state.value = RecordingState.Recording(startTime, locationFix, tempFile)
                _elapsedSeconds.value = 0L
                _currentDb.value = 0f
                _dbStats.value = null
                _isCalibrating.value = true
                dbSamples.clear()
                calibrationSamples.clear()
                noiseFloorDb = 0f

                // 6. Start ticker
                startTicker()
            } catch (e: Exception) {
                _state.value = RecordingState.Error("Failed to start recording: ${e.message}")
                cleanupAndStop()
            }
        }
    }

    fun stopRecording() {
        val currentState = _state.value
        if (currentState !is RecordingState.Recording) return

        // Enforce minimum duration
        if (_elapsedSeconds.value < MIN_DURATION_SECONDS) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // MediaRecorder may throw if stopped too early
        }
        mediaRecorder = null

        tickerJob?.cancel()
        tickerJob = null

        // Compute dB stats before reset
        if (dbSamples.isNotEmpty()) {
            _dbStats.value = DbStats(
                avgDb = dbSamples.average().toFloat(),
                maxDb = dbSamples.max(),
                minDb = dbSamples.min(),
                samples = dbSamples.size
            )
        }

        releaseWakeLock()
        abandonAudioFocus()

        _state.value = RecordingState.Idle
        _elapsedSeconds.value = 0L
        _currentDb.value = 0f

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickerJob = lifecycleScope.launch {
            while (true) {
                delay(1000L)
                val seconds = _elapsedSeconds.value + 1
                _elapsedSeconds.value = seconds

                // Sample amplitude → dB with calibration
                val amp = mediaRecorder?.maxAmplitude ?: 0
                val rawDb = if (amp > 0) (20 * log10(amp.toDouble())).toFloat() else 0f

                if (seconds <= CALIBRATION_SECONDS) {
                    // Calibration phase: collect noise floor samples
                    if (rawDb > 0f) calibrationSamples.add(rawDb)
                    _currentDb.value = 0f
                    if (seconds == CALIBRATION_SECONDS && calibrationSamples.isNotEmpty()) {
                        noiseFloorDb = calibrationSamples.average().toFloat()
                        _isCalibrating.value = false
                    }
                } else {
                    // Calibrated phase: subtract noise floor
                    val calibratedDb = (rawDb - noiseFloorDb).coerceAtLeast(0f)
                    _currentDb.value = calibratedDb
                    if (calibratedDb > 0f) dbSamples.add(calibratedDb)
                }
                val displayDb = _currentDb.value

                // Update notification
                val mm = seconds / 60
                val ss = seconds % 60
                val dbText = if (displayDb > 0 && seconds > CALIBRATION_SECONDS) " \u00B7 ${displayDb.toInt()} dB" else ""
                val timeText = "%02d:%02d".format(mm, ss)
                val notification = buildNotification("Recording... $timeText$dbText")
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(NOTIFICATION_ID, notification)

                // Auto-stop at max duration
                if (seconds >= MAX_DURATION_SECONDS) {
                    stopRecording()
                    break
                }
            }
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SoundTagApp.CHANNEL_RECORDING)
            .setContentTitle("SoundTag Recording")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    private fun cleanupAndStop() {
        mediaRecorder?.release()
        mediaRecorder = null
        releaseWakeLock()
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
        tickerJob?.cancel()
        releaseWakeLock()
        abandonAudioFocus()
    }
}
