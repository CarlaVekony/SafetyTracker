package com.example.safetytracker.ui.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val MONITORING_INTERVAL_MS = 5_000L
private const val DEFAULT_TAG = "EmergencyButton"

@Composable
fun EmergencyButton(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    logTag: String = DEFAULT_TAG,
    onMonitoringStateChanged: (Boolean) -> Unit = {},
) {
    var isMonitoring by rememberSaveable { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    val buttonColor by animateColorAsState(
        targetValue = if (isMonitoring) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        },
        label = "EmergencyButtonColor",
    )

    LaunchedEffect(isMonitoring) {
        onMonitoringStateChanged(isMonitoring)
        if (isMonitoring) {
            hasStarted = true
            Log.i(logTag, "Monitoring started")
            while (isActive) {
                delay(MONITORING_INTERVAL_MS)
                Log.i(logTag, "Monitoring sensors...")
            }
        } else if (hasStarted) {
            Log.i(logTag, "Monitoring stopped")
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { isMonitoring = !isMonitoring },
            modifier = Modifier.size(size),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        ) {
            if (isMonitoring) {
                PauseIcon(
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size / 3),
                )
            } else {
                PlayIcon(
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(size / 3),
                )
            }
        }

        MonitoringStatus(
            isMonitoring = isMonitoring,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MonitoringStatus(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusColor by animateColorAsState(
        targetValue = if (isMonitoring) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        label = "MonitoringStatusColor",
    )
    val statusText = if (isMonitoring) {
        "Monitoring in progress"
    } else {
        "Monitoring paused"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            StatusDot(color = statusColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val helperText = if (isMonitoring) {
            "Logging sensor activity"
        } else {
            "Press play to start monitoring"
        }
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
) {
    Canvas(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape),
    ) {
        drawCircle(
            color = strokeColor,
            style = Stroke(width = size.minDimension * 0.15f),
        )
        drawCircle(
            color = color,
            radius = size.minDimension / 2.5f,
            center = center,
        )
    }
}

@Composable
private fun PlayIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
private fun PauseIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width / 3f
        val gap = barWidth / 2f
        val leftBarStart = (size.width - (2 * barWidth + gap)) / 2f
        drawRect(
            color = tint,
            topLeft = Offset(leftBarStart, 0f),
            size = Size(barWidth, size.height),
        )
        drawRect(
            color = tint,
            topLeft = Offset(leftBarStart + barWidth + gap, 0f),
            size = Size(barWidth, size.height),
        )
    }
}
