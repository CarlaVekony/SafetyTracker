package com.example.safetytracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SensorGraph(
    title: String,
    data: List<Float>,
    modifier: Modifier = Modifier,
    maxValue: Float = 100f,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val density = LocalDensity.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = onSurfaceColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (data.isEmpty()) {
                val text = "No data"
                val textPaint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.GRAY
                    textSize = with(density) { 14.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    size.width / 2,
                    size.height / 2,
                    textPaint
                )
                return@Canvas
            }

            val padding = with(density) { 16.dp.toPx() }
            val graphWidth = size.width - (padding * 2)
            val graphHeight = size.height - (padding * 2)
            val maxDataPoints = 30
            
            val displayedData = if (data.size > maxDataPoints) {
                data.takeLast(maxDataPoints)
            } else {
                data
            }

            // Draw grid lines
            for (i in 0..4) {
                val y = padding + (graphHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(size.width - padding, y),
                    strokeWidth = 1f
                )
            }

            // Draw value labels on left
            val textPaint = android.graphics.Paint().apply {
                // Use white or light gray for better visibility
                this.color = android.graphics.Color.WHITE
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            
            for (i in 0..4) {
                val value = maxValue - (maxValue / 4) * i
                val y = padding + (graphHeight / 4) * i + (with(density) { 4.sp.toPx() })
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", value),
                    padding - with(density) { 8.dp.toPx() },
                    y,
                    textPaint
                )
            }

            // Draw graph line and points
            if (displayedData.size > 1) {
                val path = Path()
                val xStep = if (displayedData.size > 1) {
                    graphWidth / (displayedData.size - 1)
                } else {
                    0f
                }

                val points = displayedData.mapIndexed { index, value ->
                    val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                    val x = padding + (xStep * index)
                    val y = padding + graphHeight - (normalizedValue * graphHeight)
                    Offset(x, y) to value
                }

                // Draw path
                points.forEachIndexed { index, (point, _) ->
                    if (index == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )

                // Draw points
                points.forEach { (point, _) ->
                    drawCircle(
                        color = color,
                        radius = with(density) { 4.dp.toPx() },
                        center = point
                    )
                }
            }

            // Draw current value
            if (displayedData.isNotEmpty()) {
                val currentValue = displayedData.last()
                val currentValuePaint = android.graphics.Paint().apply {
                    // Use white for better visibility on dark background
                    this.color = android.graphics.Color.WHITE
                    textSize = with(density) { 14.sp.toPx() }
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.2f", currentValue),
                    size.width - padding - with(density) { 8.dp.toPx() },
                    padding + with(density) { 20.dp.toPx() },
                    currentValuePaint
                )
            }
        }
    }
}
