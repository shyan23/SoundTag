package com.soundtag.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.R
import com.soundtag.data.RecordingEntry
import com.soundtag.ui.components.PlaybackBar
import com.soundtag.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val noiseEmoji = mapOf(
    "traffic" to "\uD83D\uDE97", "horn" to "\uD83D\uDCEF",
    "construction" to "\uD83C\uDFD7\uFE0F", "industrial" to "\uD83C\uDFED",
    "crowd" to "\uD83D\uDC65", "nature" to "\uD83C\uDF27\uFE0F",
    "silence" to "\uD83D\uDD07", "mixed" to "\uD83D\uDD00"
)

data class MetadataSection(val title: String, val rows: List<Pair<String, String>>)

@Composable
fun RecordingDetailScreen(
    entry: RecordingEntry,
    jsonFields: Map<String, Any?>?,
    isPlaying: Boolean,
    playbackPositionMs: Int,
    playbackDurationMs: Int,
    onTogglePlayback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = noiseEmoji[entry.noiseType] ?: "\uD83C\uDFA4"
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
    val timeStr = time.format(DateTimeFormatter.ofPattern("MMM d, yyyy \u00B7 h:mm a"))
    val mm = entry.durationSeconds / 60
    val ss = entry.durationSeconds % 60

    val sections = buildSections(jsonFields, entry)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoundTagBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = SoundTagTextPrimary,
                modifier = Modifier.size(22.dp).clickable(onClick = onBack)
            )
            Text(
                text = "Recording Detail",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = SoundTagTextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoundTagSurface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = emoji, fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.filename, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = SoundTagTextPrimary)
                            Text(timeStr, fontSize = 12.sp, color = SoundTagTextTertiary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${mm}m ${ss}s", fontSize = 13.sp, color = SoundTagTextSecondary)
                        if (entry.avgDb != null) {
                            Text("\u00B7 ${entry.avgDb.toInt()} dB avg", fontSize = 13.sp, color = SoundTagTextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Playback
            item {
                PlaybackBar(
                    isPlaying = isPlaying,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs,
                    onToggle = onTogglePlayback
                )
            }

            // Metadata sections
            items(sections) { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoundTagSurface)
                ) {
                    // Section header
                    Text(
                        text = section.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundTagGreen,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
                    )
                    // Rows
                    section.rows.forEachIndexed { index, (label, value) ->
                        val rowBg = if (index % 2 == 0) SoundTagSurface else SoundTagBackground.copy(alpha = 0.5f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp, color = SoundTagTextSecondary, modifier = Modifier.weight(0.4f))
                            Text(
                                text = value,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = SoundTagTextPrimary,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private fun buildSections(json: Map<String, Any?>?, entry: RecordingEntry): List<MetadataSection> {
    if (json == null) {
        return listOf(
            MetadataSection("Recording", listOf(
                "Filename" to entry.filename,
                "Noise Type" to entry.noiseType,
                "Duration" to "${entry.durationSeconds}s",
                "Status" to entry.uploadStatus
            ))
        )
    }

    val annotation = json["annotation"] as? Map<*, *>
    val recording = json["recording"] as? Map<*, *>
    val geospatial = (json["input_features"] as? Map<*, *>)?.get("geospatial") as? Map<*, *>
    val device = json["device"] as? Map<*, *>

    return listOfNotNull(
        annotation?.let {
            MetadataSection("Annotation", listOfNotNull(
                it["noise_label"]?.let { v -> "Noise Label" to "$v" },
                it["is_noise"]?.let { v -> "Is Noise" to "$v" },
                it["severity"]?.let { v -> "Severity" to "$v" },
                it["severity_score"]?.let { v -> "Severity Score" to "$v" },
                it["environment"]?.let { v -> "Environment" to "$v" },
                it["location_context"]?.let { v -> "Location Context" to "$v" }
            ))
        },
        recording?.let {
            MetadataSection("Audio", listOfNotNull(
                it["filename"]?.let { v -> "Filename" to "$v" },
                it["duration_seconds"]?.let { v -> "Duration" to "${v}s" },
                it["sample_rate_hz"]?.let { v -> "Sample Rate" to "${v} Hz" },
                it["channels"]?.let { v -> "Channels" to if ("$v" == "1") "1 (mono)" else "$v" },
                it["encoding"]?.let { v -> "Encoding" to "$v" },
                it["avg_db"]?.let { v -> "Avg dB" to "$v" },
                it["max_db"]?.let { v -> "Max dB" to "$v" },
                it["min_db"]?.let { v -> "Min dB" to "$v" },
                it["db_samples"]?.let { v -> "dB Samples" to "$v" },
                it["notes"]?.toString()?.takeIf { s -> s.isNotBlank() && s != "" }?.let { v -> "Notes" to v }
            ))
        },
        geospatial?.let {
            MetadataSection("Location", listOfNotNull(
                it["latitude"]?.takeIf { v -> v.toString() != "null" }?.let { v -> "Latitude" to "$v" },
                it["longitude"]?.takeIf { v -> v.toString() != "null" }?.let { v -> "Longitude" to "$v" },
                recording?.get("location_accuracy_m")?.takeIf { v -> v.toString() != "null" }?.let { v -> "Accuracy" to "${v}m" }
            ))
        },
        device?.let {
            MetadataSection("Device", listOfNotNull(
                it["model"]?.let { v -> "Model" to "$v" },
                it["android_version"]?.let { v -> "Android" to "$v" },
                it["app_version"]?.let { v -> "App Version" to "$v" },
                it["annotator_id"]?.let { v -> "Annotator" to "$v" }
            ))
        }
    )
}
