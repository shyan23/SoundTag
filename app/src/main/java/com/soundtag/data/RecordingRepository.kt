package com.soundtag.data

import android.app.Application
import android.content.Context
import android.provider.MediaStore
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
    val uploadStatus: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val avgDb: Float? = null
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

    fun getWithLocation(): List<RecordingEntry> =
        loadAll().filter { it.latitude != null && it.longitude != null }

    fun updateUploadStatus(filename: String, status: String) {
        val list = loadAll().map {
            if (it.filename == filename) it.copy(uploadStatus = status) else it
        }
        save(list)
    }

    fun getPending(): List<RecordingEntry> =
        loadAll().filter { it.uploadStatus == "pending" || it.uploadStatus == "failed" }

    fun getJsonForRecording(context: Context, filename: String): String? {
        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Files.FileColumns._ID),
            selection,
            arrayOf("$filename.json"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(0)
                val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                context.contentResolver.openInputStream(contentUri)?.use { stream ->
                    return stream.bufferedReader().readText()
                }
            }
        }
        return null
    }

    fun deleteRecording(filename: String) {
        val list = loadAll().filter { it.filename != filename }
        save(list)
    }

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
                uploadStatus = obj.optString("uploadStatus", "pending"),
                latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null,
                longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null,
                avgDb = if (obj.has("avgDb") && !obj.isNull("avgDb")) obj.getDouble("avgDb").toFloat() else null
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
                put("latitude", entry.latitude ?: JSONObject.NULL)
                put("longitude", entry.longitude ?: JSONObject.NULL)
                put("avgDb", entry.avgDb?.toDouble() ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
