package com.soundtag.data

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime

object MetadataWriter {

    private val ALL_CATEGORIES = listOf(
        "traffic", "horn", "construction", "industrial",
        "crowd", "nature", "silence", "mixed"
    )

    fun buildJson(
        filename: String,
        annotation: AnnotationData,
        startTime: ZonedDateTime,
        location: LocationFix?,
        durationSeconds: Long,
        annotatorId: String = "unknown"
    ): String {
        val root = JSONObject()

        // input_features
        val inputFeatures = JSONObject()

        val geospatial = JSONObject().apply {
            put("latitude", location?.latitude ?: JSONObject.NULL)
            put("longitude", location?.longitude ?: JSONObject.NULL)
        }
        inputFeatures.put("geospatial", geospatial)

        val temporal = JSONObject().apply {
            put("year", startTime.year)
            put("month", startTime.monthValue)
            put("date", startTime.dayOfMonth)
            put("day", startTime.dayOfWeek.value)
            put("hour", startTime.hour)
            put("min", startTime.minute)
            put("sec", startTime.second)
        }
        inputFeatures.put("temporal", temporal)
        root.put("input_features", inputFeatures)

        // classification
        val classification = JSONObject().apply {
            put("target", "noise_label")
            put("categories", JSONArray(ALL_CATEGORIES))
        }
        root.put("classification", classification)

        // annotation
        val annotationObj = JSONObject().apply {
            put("noise_label", annotation.noiseType.ifEmpty { "misc" })
            put("is_noise", annotation.isNoise)
            put("severity", annotation.severity)
            put("severity_score", annotation.severityScore)
            put("environment", annotation.environment)
            put("location_context", annotation.locationContext.ifEmpty { "other" })
        }
        root.put("annotation", annotationObj)

        // recording
        val recording = JSONObject().apply {
            put("filename", "$filename.m4a")
            put("duration_seconds", durationSeconds)
            put("sample_rate_hz", 44100)
            put("channels", 1)
            put("encoding", "AAC")
            put("location_accuracy_m", location?.accuracyMeters?.toDouble() ?: JSONObject.NULL)
            put("notes", annotation.notes)
        }
        root.put("recording", recording)

        // device
        val device = JSONObject().apply {
            put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("android_version", Build.VERSION.RELEASE)
            put("app_version", "1.0.0")
            put("annotator_id", annotatorId)
        }
        root.put("device", device)

        return root.toString(2)
    }

    private fun severityToScore(severity: String): Int = when (severity) {
        "low" -> 1
        "medium" -> 3
        "high" -> 5
        else -> 3
    }
}
