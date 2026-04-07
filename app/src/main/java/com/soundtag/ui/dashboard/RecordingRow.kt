package com.soundtag.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.data.RecordingEntry
import com.soundtag.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val noiseEmoji = mapOf(
    "traffic" to "\uD83D\uDE97",
    "horn" to "\uD83D\uDCEF",
    "construction" to "\uD83C\uDFD7\uFE0F",
    "industrial" to "\uD83C\uDFED",
    "crowd" to "\uD83D\uDC65",
    "nature" to "\uD83C\uDF27\uFE0F",
    "silence" to "\uD83D\uDD07",
    "mixed" to "\uD83D\uDD00"
)

@Composable
fun RecordingRow(entry: RecordingEntry, modifier: Modifier = Modifier) {
    val time = Instant.ofEpochMilli(entry.timestamp)
        .atZone(ZoneId.systemDefault())
    val timeStr = time.format(DateTimeFormatter.ofPattern("h:mm a"))
    val mm = entry.durationSeconds / 60
    val ss = entry.durationSeconds % 60
    val durationStr = "${mm}m ${ss.toString().padStart(2, '0')}s"
    val emoji = noiseEmoji[entry.noiseType] ?: "\uD83C\uDFA4"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SoundTagSurface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = emoji, fontSize = 18.sp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = entry.filename,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = SoundTagTextPrimary
            )
            Text(
                text = "$timeStr \u00B7 $durationStr",
                fontSize = 12.sp,
                color = SoundTagTextTertiary
            )
        }

        StatusBadge(status = entry.uploadStatus)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, textColor, bgColor) = when (status) {
        "uploaded" -> Triple("Uploaded", SoundTagSuccess, SoundTagSuccess.copy(alpha = 0.1f))
        "pending" -> Triple("Pending", SoundTagWarning, SoundTagWarning.copy(alpha = 0.1f))
        "failed" -> Triple("Failed", SoundTagError, SoundTagError.copy(alpha = 0.1f))
        else -> Triple("Local", SoundTagTextSecondary, SoundTagSurface)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
