package com.soundtag.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.soundtag.data.DriveFolder
import com.soundtag.ui.theme.*

@Composable
fun FolderPickerDialog(
    folders: List<DriveFolder>?,
    onSelect: (DriveFolder) -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SoundTagSurfaceVariant)
                .padding(20.dp)
        ) {
            Text(
                text = "Select Upload Folder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoundTagTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a shared folder or use default",
                fontSize = 13.sp,
                color = SoundTagTextTertiary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Use Default option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SoundTagSurface)
                    .clickable(onClick = onUseDefault)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\uD83D\uDCC1", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Use Default (SoundTag/)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SoundTagGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (folders == null) {
                // Loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SoundTagGreen,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No folders found",
                        fontSize = 13.sp,
                        color = SoundTagTextTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height((folders.size.coerceAtMost(6) * 48).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoundTagSurface)
                                .clickable { onSelect(folder) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "\uD83D\uDCC1", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = folder.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoundTagTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (folder.isShared) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(SoundTagGreenSubtle)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Shared",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SoundTagGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
