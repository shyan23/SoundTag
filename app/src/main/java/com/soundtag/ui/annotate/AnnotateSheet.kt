package com.soundtag.ui.annotate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.data.AnnotationData
import com.soundtag.data.LocationFix
import com.soundtag.ui.components.SoundTagChip
import com.soundtag.ui.components.SoundTagTextField
import com.soundtag.ui.components.ToggleGroup
import com.soundtag.ui.theme.*

private val noiseTypes = listOf(
    "\uD83D\uDE97 Traffic" to "traffic",
    "\uD83D\uDCEF Horn" to "horn",
    "\uD83C\uDFD7\uFE0F Construction" to "construction",
    "\uD83C\uDFED Industrial" to "industrial",
    "\uD83D\uDC65 Crowd" to "crowd",
    "\uD83C\uDF27\uFE0F Nature" to "nature",
    "\uD83D\uDD07 Silence" to "silence",
    "\uD83D\uDD00 Mixed" to "mixed"
)

private val locationContexts = listOf(
    "Roadside", "Market", "Residential",
    "Construction Site", "Industrial Zone", "Other"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnnotateSheetContent(
    annotation: AnnotationData,
    durationSeconds: Long,
    recordingTime: String,
    location: LocationFix?,
    onAnnotationChange: (AnnotationData) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean = false,
    isDriveConnected: Boolean = false,
    annotatorId: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Annotate Recording",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = SoundTagTextPrimary
            )
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            Text(
                text = "${minutes} min ${seconds} sec \u00B7 $recordingTime",
                fontSize = 14.sp,
                color = SoundTagTextSecondary
            )
            if (location != null) {
                Text(
                    text = "%.4f° N, %.4f° E".format(location.latitude, location.longitude),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SoundTagTextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section 1: Noise type
        SectionLabel("What did you record?")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            noiseTypes.forEach { (label, value) ->
                SoundTagChip(
                    label = label,
                    selected = annotation.noiseType == value,
                    onClick = {
                        onAnnotationChange(
                            annotation.copy(
                                noiseType = value,
                                fileName = annotation.fileName.let { current ->
                                    if (current.isEmpty() || noiseTypes.any { (_, v) -> current.startsWith(v) }) {
                                        value + current.substringAfter("_", missingDelimiterValue = "")
                                            .let { if (it.isNotEmpty()) "_$it" else "" }
                                    } else current
                                }
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section 2: Is noise + Severity
        SectionLabel("Is this noise pollution?")
        Spacer(modifier = Modifier.height(14.dp))
        ToggleGroup(
            options = listOf("Yes", "No"),
            selected = if (annotation.isNoise) "Yes" else "No",
            onSelect = { onAnnotationChange(annotation.copy(isNoise = it == "Yes")) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Severity",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SoundTagTextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("low", "medium", "high").forEach { level ->
                val isSelected = annotation.severity == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SoundTagGreenSubtle else SoundTagSurface)
                        .then(
                            if (isSelected) Modifier.border(1.dp, SoundTagGreen, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { onAnnotationChange(annotation.copy(severity = level)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) SoundTagGreen else SoundTagTextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Severity score slider (1-5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: ${annotation.severityScore}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SoundTagTextSecondary,
                modifier = Modifier.width(60.dp)
            )
            (1..5).forEach { score ->
                val isSelected = annotation.severityScore == score
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SoundTagGreen else SoundTagSurface)
                        .clickable { onAnnotationChange(annotation.copy(severityScore = score)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$score",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SoundTagBackground else SoundTagTextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section 3: Environment + Location context
        SectionLabel("Where were you?")
        Spacer(modifier = Modifier.height(14.dp))
        ToggleGroup(
            options = listOf("Outdoor", "Indoor"),
            selected = annotation.environment.replaceFirstChar { it.uppercase() },
            onSelect = { onAnnotationChange(annotation.copy(environment = it.lowercase())) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Location context",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SoundTagTextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            locationContexts.forEach { ctx ->
                val value = ctx.lowercase().replace(" ", "_")
                SoundTagChip(
                    label = ctx,
                    selected = annotation.locationContext == value,
                    onClick = { onAnnotationChange(annotation.copy(locationContext = value)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section 4: File details
        SectionLabel("File Details")
        Spacer(modifier = Modifier.height(14.dp))
        SoundTagTextField(
            label = "Recording Name",
            value = annotation.fileName,
            onValueChange = { onAnnotationChange(annotation.copy(fileName = it)) },
            monospace = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Prefix before underscore becomes the ML label",
            fontSize = 12.sp,
            color = SoundTagTextTertiary
        )
        Spacer(modifier = Modifier.height(14.dp))
        SoundTagTextField(
            label = "Notes (optional)",
            value = annotation.notes,
            onValueChange = { onAnnotationChange(annotation.copy(notes = it)) },
            placeholder = "e.g. Near Farmgate intersection",
            multiline = true,
            height = 100.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSaving) SoundTagGreen.copy(alpha = 0.5f) else SoundTagGreen)
                .clickable(enabled = !isSaving, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSaving) "Saving\u2026" else "Save & Upload",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoundTagBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isDriveConnected) "Saves to Drive under $annotatorId/" else "Saves locally to Music/SoundTag/",
            fontSize = 12.sp,
            color = SoundTagTextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = SoundTagTextPrimary
    )
}
