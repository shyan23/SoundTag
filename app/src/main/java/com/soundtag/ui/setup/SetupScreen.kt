package com.soundtag.ui.setup

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.R
import com.soundtag.ui.components.SoundTagTextField
import com.soundtag.ui.theme.*

private val AvatarPurple = Color(0xFF7C3AED)

@Composable
fun SetupScreen(
    name: String,
    annotatorId: String,
    isDriveConnected: Boolean,
    customFolderName: String,
    onNameChange: (String) -> Unit,
    onIdChange: (String) -> Unit,
    onConnectDrive: () -> Unit,
    onChooseFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onStartCollecting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoundTagBackground)
            .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section
        Column {
            // Header: icon + title + subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoundTagGreenSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification),
                        contentDescription = null,
                        tint = SoundTagGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "SoundTag",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = SoundTagTextPrimary
                )
                Text(
                    text = "Urban Noise Dataset Collector",
                    fontSize = 15.sp,
                    color = SoundTagTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Avatar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AvatarPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoundTagTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name.ifEmpty { "Your Name" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundTagTextPrimary
                    )
                    Text(
                        text = annotatorId.ifEmpty { "ID" },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SoundTagTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form fields
            SoundTagTextField(
                label = "Your Name",
                value = name,
                onValueChange = onNameChange,
                placeholder = "Enter your name"
            )
            Spacer(modifier = Modifier.height(20.dp))
            SoundTagTextField(
                label = "Annotator ID",
                value = annotatorId,
                onValueChange = onIdChange,
                placeholder = "e.g. HML-01",
                monospace = true
            )
        }

        // Bottom section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Drive status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoundTagSurface)
                    .clickable(onClick = onConnectDrive)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    tint = if (isDriveConnected) SoundTagSuccess else SoundTagTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isDriveConnected) "Connected to Team Drive" else "Tap to connect Google Drive",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SoundTagTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(
                            if (isDriveConnected) Color(0x1822C55E) else Color(0x18A1A1AA)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isDriveConnected) "Active" else "Offline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDriveConnected) SoundTagSuccess else SoundTagTextSecondary
                    )
                }
            }

            // Folder picker (only when Drive connected)
            if (isDriveConnected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoundTagSurface)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\uD83D\uDCC1", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = customFolderName.ifEmpty { "Default (SoundTag/)" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (customFolderName.isEmpty()) SoundTagTextTertiary else SoundTagTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (customFolderName.isNotEmpty()) {
                        Text(
                            text = "\u2715",
                            fontSize = 14.sp,
                            color = SoundTagTextSecondary,
                            modifier = Modifier
                                .clickable(onClick = onClearFolder)
                                .padding(8.dp)
                        )
                    }
                    Text(
                        text = if (customFolderName.isEmpty()) "Choose" else "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoundTagGreen,
                        modifier = Modifier
                            .clickable(onClick = onChooseFolder)
                            .padding(4.dp)
                    )
                }
            }

            // Start Collecting button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (name.isNotBlank() && annotatorId.isNotBlank()) SoundTagGreen
                        else SoundTagGreen.copy(alpha = 0.4f)
                    )
                    .clickable(
                        enabled = name.isNotBlank() && annotatorId.isNotBlank(),
                        onClick = onStartCollecting
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start Collecting",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoundTagBackground
                )
            }

            Text(
                text = "Settings can be changed later",
                fontSize = 12.sp,
                color = SoundTagTextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
