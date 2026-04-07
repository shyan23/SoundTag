package com.soundtag.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentFile: String = "",
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun playFile(file: File, scope: CoroutineScope) {
        val path = file.absolutePath
        if (_state.value.isPlaying && _state.value.currentFile == path) {
            pause()
            return
        }
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener { stop() }
            }
            _state.value = PlaybackState(
                isPlaying = true,
                currentFile = path,
                durationMs = mediaPlayer?.duration ?: 0
            )
            startTicker(scope)
        } catch (_: Exception) {
            stop()
        }
    }

    fun playUri(context: Context, uri: Uri, identifier: String, scope: CoroutineScope) {
        if (_state.value.isPlaying && _state.value.currentFile == identifier) {
            pause()
            return
        }
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { stop() }
            }
            _state.value = PlaybackState(
                isPlaying = true,
                currentFile = identifier,
                durationMs = mediaPlayer?.duration ?: 0
            )
            startTicker(scope)
        } catch (_: Exception) {
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        tickerJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = PlaybackState()
    }

    private fun startTicker(scope: CoroutineScope) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(200)
                val mp = mediaPlayer ?: break
                if (!mp.isPlaying) break
                _state.value = _state.value.copy(
                    positionMs = mp.currentPosition
                )
            }
        }
    }
}
