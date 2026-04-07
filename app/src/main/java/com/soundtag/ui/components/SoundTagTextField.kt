package com.soundtag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtag.ui.theme.SoundTagBorder
import com.soundtag.ui.theme.SoundTagSurface
import com.soundtag.ui.theme.SoundTagTextPrimary
import com.soundtag.ui.theme.SoundTagTextSecondary

@Composable
fun SoundTagTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    monospace: Boolean = false,
    multiline: Boolean = false,
    height: Dp = 48.dp
) {
    val shape = RoundedCornerShape(12.dp)
    val fontFamily = if (monospace) FontFamily.Monospace else FontFamily.SansSerif

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SoundTagTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            textStyle = TextStyle(
                fontSize = if (multiline) 15.sp else 13.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                color = SoundTagTextPrimary
            ),
            cursorBrush = SolidColor(SoundTagTextPrimary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(SoundTagSurface, shape)
                        .border(1.dp, SoundTagBorder, shape)
                        .padding(horizontal = 16.dp, vertical = if (multiline) 12.dp else 0.dp),
                    contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            fontFamily = fontFamily,
                            color = SoundTagTextSecondary
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
