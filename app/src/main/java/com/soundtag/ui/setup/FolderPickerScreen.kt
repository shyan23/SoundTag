package com.soundtag.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.R
import com.soundtag.data.DriveFolder
import com.soundtag.ui.theme.*

@Composable
fun FolderPickerScreen(
    folders: List<DriveFolder>?,
    folderPath: List<Pair<String?, String>>,
    onBrowseInto: (String, String) -> Unit,
    onSelect: (DriveFolder) -> Unit,
    onSelectCurrent: () -> Unit,
    onUseDefault: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredFolders = folders?.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }
    val isInsideFolder = folderPath.size > 1

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
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onBack)
            )
            Text(
                text = "Select Upload Folder",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = SoundTagTextPrimary
            )
        }

        // Breadcrumb path
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            folderPath.forEachIndexed { index, (_, name) ->
                if (index > 0) {
                    Text(
                        text = " \u203A ",
                        fontSize = 13.sp,
                        color = SoundTagTextTertiary
                    )
                }
                val isLast = index == folderPath.lastIndex
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isLast) SoundTagTextPrimary else SoundTagGreen,
                    modifier = if (!isLast) Modifier.clickable {
                        // Navigate back to this level — handled by repeated browseBack
                    } else Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoundTagSurface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDD0D", fontSize = 14.sp, color = SoundTagTextTertiary)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search folders\u2026",
                            fontSize = 15.sp,
                            color = SoundTagTextTertiary
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = SoundTagTextPrimary),
                        cursorBrush = SolidColor(SoundTagGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "\u2715",
                        fontSize = 14.sp,
                        color = SoundTagTextSecondary,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Select this folder" button when inside a subfolder
        if (isInsideFolder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoundTagGreen)
                    .clickable(onClick = onSelectCurrent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2713 Select \"${folderPath.last().second}\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoundTagBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Folder list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Use Default option (only at root)
            if (!isInsideFolder) {
                item {
                    FolderRow(
                        name = "Use Default (SoundTag/)",
                        isShared = false,
                        nameColor = SoundTagGreen,
                        showArrow = false,
                        onClick = onUseDefault
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (filteredFolders == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SoundTagGreen,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else if (filteredFolders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No folders match \"$searchQuery\""
                            else "No subfolders",
                            fontSize = 14.sp,
                            color = SoundTagTextTertiary
                        )
                    }
                }
            } else {
                items(filteredFolders) { folder ->
                    FolderRow(
                        name = folder.name,
                        isShared = folder.isShared,
                        showArrow = true,
                        onClick = { onBrowseInto(folder.id, folder.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    isShared: Boolean,
    showArrow: Boolean,
    onClick: () -> Unit,
    nameColor: androidx.compose.ui.graphics.Color = SoundTagTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SoundTagSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "\uD83D\uDCC1", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = nameColor,
            modifier = Modifier.weight(1f)
        )
        if (isShared) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SoundTagGreenSubtle)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Shared",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoundTagGreen
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Text(text = "\u203A", fontSize = 16.sp, color = SoundTagTextTertiary)
        }
    }
}
