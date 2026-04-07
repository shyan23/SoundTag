package com.soundtag.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.soundtag.ui.theme.SoundTagBorder
import com.soundtag.ui.theme.SoundTagGreen
import com.soundtag.ui.theme.SoundTagSurface

@Composable
fun DbLineChart(
    dbHistory: List<Float>,
    modifier: Modifier = Modifier,
    maxDb: Float = 40f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SoundTagSurface)
    ) {
        val w = size.width
        val h = size.height
        val padding = 8.dp.toPx()
        val chartW = w - padding * 2
        val chartH = h - padding * 2

        // Grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = padding + chartH * i / gridCount
            drawLine(
                color = SoundTagBorder,
                start = Offset(padding, y),
                end = Offset(w - padding, y),
                strokeWidth = 0.5f
            )
        }

        if (dbHistory.size < 2) return@Canvas

        val effectiveMax = dbHistory.max().coerceAtLeast(maxDb)
        val stepX = chartW / (dbHistory.size - 1).coerceAtLeast(1)

        // Draw line
        val path = Path()
        dbHistory.forEachIndexed { index, db ->
            val x = padding + index * stepX
            val normalized = (db / effectiveMax).coerceIn(0f, 1f)
            val y = padding + chartH * (1f - normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SoundTagGreen,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw dots at each point
        dbHistory.forEachIndexed { index, db ->
            val x = padding + index * stepX
            val normalized = (db / effectiveMax).coerceIn(0f, 1f)
            val y = padding + chartH * (1f - normalized)
            drawCircle(
                color = SoundTagGreen,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
