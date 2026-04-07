package com.soundtag.ui.record

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.R
import com.soundtag.data.LocationFix
import com.soundtag.ui.theme.*

@Composable
fun RecordScreen(
    isRecording: Boolean,
    elapsedSeconds: Long,
    location: LocationFix?,
    annotatorId: String,
    todayCount: Int,
    onToggleRecording: () -> Unit,
    onDashboardTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isRecording) SoundTagError else SoundTagGreen
    val accentSubtle = if (isRecording) Color(0x60EF4444) else Color(0x3000E5A0)
    val accentFaint = if (isRecording) Color(0x40EF4444) else Color(0x1800E5A0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoundTagBackground)
    ) {
        // Top bar
        TopBar(isRecording = isRecording, annotatorId = annotatorId, onDashboardTap = onDashboardTap)

        // Colored strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isRecording) SoundTagError else SoundTagSuccess)
        )

        // Center content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mic button with rings
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .border(1.5.dp, SoundTagBorder, CircleShape)
                )
                // Inner glow ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(1.dp, accentSubtle, CircleShape)
                )
                // Main button
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentFaint, Color.Transparent)
                            )
                        )
                        .border(1.5.dp, accentColor, CircleShape)
                        .clickable(onClick = onToggleRecording),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification),
                            contentDescription = if (isRecording) "Stop recording" else "Start recording",
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                        if (isRecording) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val mm = elapsedSeconds / 60
                            val ss = elapsedSeconds % 60
                            Text(
                                text = "%02d:%02d".format(mm, ss),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = SoundTagTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isRecording) {
                // Recording indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SoundTagError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording in background\u2026",
                        fontSize = 14.sp,
                        color = SoundTagTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Waveform visualization (decorative)
                WaveformBars()
            } else {
                Text(
                    text = "Tap to start recording",
                    fontSize = 14.sp,
                    color = SoundTagTextTertiary
                )
            }
        }

        // Bottom bar
        BottomBar(isRecording = isRecording, location = location, todayCount = todayCount)
    }
}

@Composable
private fun TopBar(isRecording: Boolean, annotatorId: String, onDashboardTap: () -> Unit) {
    val dotColor = if (isRecording) SoundTagError else SoundTagGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Status pill
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(SoundTagSurface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = annotatorId.ifEmpty { "---" },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = SoundTagTextSecondary
            )
        }

        Text(
            text = "SoundTag",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = SoundTagTextPrimary
        )

        // Settings button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SoundTagSurface)
                .clickable(onClick = onDashboardTap),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u2261",
                fontSize = 18.sp,
                color = SoundTagTextSecondary
            )
        }
    }
}

@Composable
private fun WaveformBars() {
    val barHeights = listOf(8, 16, 24, 18, 28, 12, 20, 32, 14, 22, 10, 26, 16, 8)
    val barAlphas = listOf(0.3f, 0.45f, 1f, 0.5f, 1f, 0.4f, 0.55f, 1f, 0.45f, 0.5f, 0.3f, 1f, 0.45f, 0.3f)

    Row(
        modifier = Modifier.width(200.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SoundTagError.copy(alpha = barAlphas[index]))
            )
        }
    }
}

@Composable
private fun BottomBar(isRecording: Boolean, location: LocationFix?, todayCount: Int) {
    val pinColor = if (isRecording) SoundTagError else SoundTagGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // GPS row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_notification),
                contentDescription = null,
                tint = pinColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (location != null) {
                Text(
                    text = "%.4f° N, %.4f° E".format(location.latitude, location.longitude),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SoundTagTextTertiary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SoundTagSurface)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isRecording) "Locked" else "%.0fm".format(location.accuracyMeters),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRecording) SoundTagError else SoundTagGreen
                    )
                }
            } else {
                Text(
                    text = "Acquiring GPS\u2026",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SoundTagTextTertiary
                )
            }
        }

        if (isRecording) {
            // Background safe badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SoundTagSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDEE1\uFE0F",
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Background safe \u2014 screen can turn off",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SoundTagTextTertiary
                )
            }
        } else {
            Text(
                text = "Today: $todayCount recordings",
                fontSize = 13.sp,
                color = SoundTagTextTertiary
            )
        }
    }
}
