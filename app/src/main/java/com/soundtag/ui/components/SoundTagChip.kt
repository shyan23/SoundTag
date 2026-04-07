package com.soundtag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.ui.theme.SoundTagGreen
import com.soundtag.ui.theme.SoundTagGreenSubtle
import com.soundtag.ui.theme.SoundTagSurface
import com.soundtag.ui.theme.SoundTagTextSecondary

@Composable
fun SoundTagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) SoundTagGreenSubtle else SoundTagSurface
    val textColor = if (selected) SoundTagGreen else SoundTagTextSecondary
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    val shape = RoundedCornerShape(9999.dp)

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (selected) Modifier.border(1.dp, SoundTagGreen, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}
