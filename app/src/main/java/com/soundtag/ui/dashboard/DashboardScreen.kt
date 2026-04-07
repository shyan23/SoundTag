package com.soundtag.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.R
import com.soundtag.data.RecordingEntry
import com.soundtag.ui.theme.*

@Composable
fun DashboardScreen(
    recordings: List<RecordingEntry>,
    todayCount: Int,
    totalCount: Int,
    labelCounts: Map<String, Int>,
    totalDuration: Long,
    isDriveConnected: Boolean,
    onBack: () -> Unit,
    onSyncPending: () -> Unit,
    onOpenMap: (Double, Double, String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = SoundTagTextPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onBack)
            )
            Text(
                text = "Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = SoundTagTextPrimary
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SoundTagSurface)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2699",
                    fontSize = 16.sp,
                    color = SoundTagTextSecondary
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 20.dp)
        ) {
            TabItem("Recordings", selected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
            TabItem("Dataset Stats", selected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
        }

        // Content
        when (selectedTab) {
            0 -> RecordingsTab(
                recordings = recordings,
                todayCount = todayCount,
                totalCount = totalCount,
                isDriveConnected = isDriveConnected,
                onSyncPending = onSyncPending
            )
            1 -> DatasetStatsTab(
                labelCounts = labelCounts,
                totalCount = totalCount,
                totalDuration = totalDuration,
                recordingsWithLocation = recordings.filter { it.latitude != null && it.longitude != null },
                onOpenMap = onOpenMap
            )
        }
    }
}

@Composable
private fun TabItem(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(
                    width = 2.dp,
                    color = SoundTagGreen,
                    shape = RoundedCornerShape(0.dp)
                ) else Modifier.border(
                    width = 1.dp,
                    color = SoundTagBorder,
                    shape = RoundedCornerShape(0.dp)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) SoundTagGreen else SoundTagTextTertiary
        )
    }
}

@Composable
private fun RecordingsTab(
    recordings: List<RecordingEntry>,
    todayCount: Int,
    totalCount: Int,
    isDriveConnected: Boolean,
    onSyncPending: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoundTagSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today: $todayCount recordings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundTagTextPrimary
                    )
                    Text(
                        text = "Total: $totalCount",
                        fontSize = 13.sp,
                        color = SoundTagTextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification),
                        contentDescription = null,
                        tint = if (isDriveConnected) SoundTagSuccess else SoundTagTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDriveConnected) "Connected to Drive" else "Offline mode",
                        fontSize = 13.sp,
                        color = SoundTagTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, SoundTagBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onSyncPending),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sync All Pending",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoundTagTextPrimary
                    )
                }
            }
        }

        // Recording rows
        items(recordings) { entry ->
            RecordingRow(entry = entry)
        }

        if (recordings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recordings yet",
                        fontSize = 14.sp,
                        color = SoundTagTextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun DatasetStatsTab(
    labelCounts: Map<String, Int>,
    totalCount: Int,
    totalDuration: Long,
    recordingsWithLocation: List<RecordingEntry>,
    onOpenMap: (Double, Double, String) -> Unit
) {
    val allLabels = listOf("traffic", "horn", "construction", "industrial", "crowd", "nature", "silence", "mixed")
    val maxCount = labelCounts.values.maxOrNull() ?: 1
    val lowLabels = allLabels.filter { (labelCounts[it] ?: 0) < 5 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoundTagSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Dataset Overview", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SoundTagTextPrimary)
                val hours = totalDuration / 3600
                val mins = (totalDuration % 3600) / 60
                Text(
                    text = "$totalCount recordings \u00B7 ${hours}h ${mins}m total",
                    fontSize = 13.sp,
                    color = SoundTagTextSecondary
                )
            }
        }

        // Balance warning
        if (lowLabels.isNotEmpty() && totalCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoundTagWarning.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\u26A0\uFE0F Collect more: ${lowLabels.joinToString(", ")}. Each label needs at least 5 clips.",
                        fontSize = 13.sp,
                        color = SoundTagWarning
                    )
                }
            }
        }

        // Bar chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoundTagSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Count per Label", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SoundTagTextPrimary)

                allLabels.forEach { label ->
                    val count = labelCounts[label] ?: 0
                    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = SoundTagTextSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SoundTagBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SoundTagGreen)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoundTagTextPrimary,
                            modifier = Modifier.width(30.dp)
                        )
                    }
                }
            }
        }

        // Recording Locations
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoundTagSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recording Locations", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SoundTagTextPrimary)
                    Text(
                        text = "${recordingsWithLocation.size} with GPS",
                        fontSize = 12.sp,
                        color = SoundTagTextTertiary
                    )
                }

                if (recordingsWithLocation.isEmpty()) {
                    Text("No recordings with GPS data yet", fontSize = 13.sp, color = SoundTagTextTertiary)
                } else {
                    recordingsWithLocation.take(20).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onOpenMap(entry.latitude!!, entry.longitude!!, entry.filename)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SoundTagGreen)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.filename,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = SoundTagTextPrimary
                                )
                                Text(
                                    text = "%.4f\u00B0, %.4f\u00B0".format(entry.latitude, entry.longitude),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = SoundTagTextTertiary
                                )
                            }
                            Text(
                                text = "\u2192",
                                fontSize = 14.sp,
                                color = SoundTagGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
