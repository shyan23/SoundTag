package com.soundtag.data

import android.app.Application
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RecordingEntry(
    val filename: String,
    val noiseType: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val uploadStatus: String
)

class RecordingRepository(application: Application) {

    private val prefs = application.getSharedPreferences("recording_history", Context.MODE_PRIVATE)
    private val key = "recordings"

    fun addRecording(entry: RecordingEntry) {
        val list = loadAll().toMutableList()
        list.add(0, entry)
        save(list)
    }

    fun getAll(): List<RecordingEntry> = loadAll()

    fun getTodayCount(): Int {
        val today = LocalDate.now()
        return loadAll().count { entry ->
            Instant.ofEpochMilli(entry.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == today
        }
    }

    fun getTotalCount(): Int = loadAll().size

    fun getCountByLabel(): Map<String, Int> {
        return loadAll().groupingBy { it.noiseType.ifEmpty { "misc" } }.eachCount()
    }

    fun getTotalDuration(): Long = loadAll().sumOf { it.durationSeconds }

    fun updateUploadStatus(filename: String, status: String) {
        val list = loadAll().map {
            if (it.filename == filename) it.copy(uploadStatus = status) else it
        }
        save(list)
    }

    fun getPending(): List<RecordingEntry> =
        loadAll().filter { it.uploadStatus == "pending" || it.uploadStatus == "failed" }

    private fun loadAll(): List<RecordingEntry> {
        val json = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RecordingEntry(
                filename = obj.getString("filename"),
                noiseType = obj.optString("noiseType", "misc"),
                timestamp = obj.getLong("timestamp"),
                durationSeconds = obj.getLong("durationSeconds"),
                uploadStatus = obj.optString("uploadStatus", "pending")
            )
        }
    }

    private fun save(list: List<RecordingEntry>) {
        val arr = JSONArray()
        list.forEach { entry ->
            arr.put(JSONObject().apply {
                put("filename", entry.filename)
                put("noiseType", entry.noiseType)
                put("timestamp", entry.timestamp)
                put("durationSeconds", entry.durationSeconds)
                put("uploadStatus", entry.uploadStatus)
            })
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
