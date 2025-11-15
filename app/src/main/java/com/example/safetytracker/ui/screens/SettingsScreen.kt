package com.example.safetytracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.safetytracker.data.preferences.UserPreferences
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences.getInstance(context) }

    val isDarkModeEnabled by prefs.isDarkModeEnabled.collectAsState(initial = false)
    val isDynamicColorsEnabled by prefs.isDynamicColorsEnabled.collectAsState(initial = true)
    val isAutoMonitoringEnabled by prefs.isAutoMonitoringEnabled.collectAsState(initial = false)

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            // Appearance
            SettingsCard(
                title = "Appearance",
                icon = Icons.Outlined.Palette
            ) {
                SettingToggle(
                    title = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    isChecked = isDarkModeEnabled,
                    onToggle = { scope.launch { prefs.setDarkMode(it) }},
                    icon = if (isDarkModeEnabled) Icons.Outlined.DarkMode else Icons.Outlined.LightMode
                )

                SettingToggle(
                    title = "Dynamic Colors",
                    subtitle = "Use system colors (Android 12+)",
                    isChecked = isDynamicColorsEnabled,
                    onToggle = { scope.launch { prefs.setDynamicColors(it) }},
                    icon = Icons.Outlined.ColorLens
                )
            }

            // Monitoring
            SettingsCard(
                title = "Monitoring",
                icon = Icons.Outlined.Security
            ) {
                SettingToggle(
                    title = "Auto-start Monitoring",
                    subtitle = "Begin monitoring when the app opens",
                    isChecked = isAutoMonitoringEnabled,
                    onToggle = { scope.launch { prefs.setAutoMonitoring(it) }},
                    icon = Icons.Outlined.PlayArrow
                )
            }

            // Emergency detection placeholder
            SettingsCard(
                title = "Emergency Detection",
                icon = Icons.Outlined.Shield
            ) {
                Text(
                    text = "Configure fall sensitivity and emergency thresholds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Coming soon...",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Notifications
            SettingsCard(
                title = "Notifications",
                icon = Icons.Outlined.Notifications
            ) {
                Text(
                    text = "Manage alert preferences and notification settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // About section
            SettingsCard(
                title = "About SafetyTracker",
                icon = Icons.Outlined.Info
            ) {
                Text(
                    text = "Version 1.0\nEmergency detection & alert system\n\nDeveloped for safety and peace of mind.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(scale)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SafetyTrackerTheme {
        SettingsScreen()
    }
}
