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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.soundtag.ui.theme.SoundTagBorder
import com.soundtag.ui.theme.SoundTagGreen
import com.soundtag.ui.theme.SoundTagSurface
import com.soundtag.ui.theme.SoundTagTextTertiary

@Composable
fun DbLineChart(
    dbHistory: List<Float>,
    modifier: Modifier = Modifier,
    maxDb: Float = 40f
) {
    val labelColor = SoundTagTextTertiary
    val gridColor = SoundTagBorder

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SoundTagSurface)
    ) {
        val w = size.width
        val h = size.height
        val leftPadding = 36.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 10.dp.toPx()
        val bottomPadding = 10.dp.toPx()
        val chartW = w - leftPadding - rightPadding
        val chartH = h - topPadding - bottomPadding

        val effectiveMax = if (dbHistory.isNotEmpty()) dbHistory.max().coerceAtLeast(maxDb) else maxDb

        // Y-axis labels + grid lines
        val gridCount = 4
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.hashCode()
            textSize = 9.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        for (i in 0..gridCount) {
            val y = topPadding + chartH * i / gridCount
            val dbValue = effectiveMax * (1f - i.toFloat() / gridCount)

            // Grid line
            drawLine(
                color = gridColor.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(w - rightPadding, y),
                strokeWidth = 0.5f
            )

            // Y-axis label
            drawContext.canvas.nativeCanvas.drawText(
                "${dbValue.toInt()}",
                leftPadding - 6.dp.toPx(),
                y + 3.dp.toPx(),
                textPaint
            )
        }

        // "dB" label at top-left
        val unitPaint = android.graphics.Paint().apply {
            color = labelColor.hashCode()
            textSize = 8.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }
        drawContext.canvas.nativeCanvas.drawText(
            "dB",
            2.dp.toPx(),
            topPadding - 1.dp.toPx(),
            unitPaint
        )

        if (dbHistory.size < 2) return@Canvas

        val stepX = chartW / (dbHistory.size - 1).coerceAtLeast(1)

        // Draw line
        val path = Path()
        dbHistory.forEachIndexed { index, db ->
            val x = leftPadding + index * stepX
            val normalized = (db / effectiveMax).coerceIn(0f, 1f)
            val y = topPadding + chartH * (1f - normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SoundTagGreen,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw dots at each point
        dbHistory.forEachIndexed { index, db ->
            val x = leftPadding + index * stepX
            val normalized = (db / effectiveMax).coerceIn(0f, 1f)
            val y = topPadding + chartH * (1f - normalized)
            drawCircle(
                color = SoundTagGreen,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
