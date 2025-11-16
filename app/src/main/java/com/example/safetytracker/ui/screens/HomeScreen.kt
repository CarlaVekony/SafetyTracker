package com.example.safetytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.safetytracker.network.EmergencyAlertService
import com.example.safetytracker.sensors.AccelerometerManager
import com.example.safetytracker.sensors.GPSManager
import com.example.safetytracker.sensors.GyroscopeManager
import com.example.safetytracker.sensors.MicrophoneManager
import com.example.safetytracker.ui.components.EmergencyAlertPopup
import com.example.safetytracker.ui.components.EmergencyButton
import com.example.safetytracker.ui.components.SensorGraph
import com.example.safetytracker.ui.components.rememberSensorData
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(24.dp),
	enableEmergencyMonitoring: Boolean = true,
) {
    val context = LocalContext.current
    var isMonitoring by remember { mutableStateOf(false) }
    val sensorData = rememberSensorData(isMonitoring = isMonitoring)
    
    // Emergency alert service - share managers with SensorDataManager
	val emergencyService = remember { 
        val micManager = MicrophoneManager(context)
        val gpsMgr = GPSManager(context)
        EmergencyAlertService(context, micManager, gpsMgr)
    }
    
    // Monitor prepared alerts (throttled collection to avoid blocking UI)
	val preparedAlerts by if (enableEmergencyMonitoring) {
		emergencyService.preparedAlerts.collectAsState()
	} else {
		// Dummy state for tests when emergency monitoring is disabled
		mutableStateOf(emptyList())
	}
    var showAlertPopup by remember { mutableStateOf(false) }
    var lastAlertId by remember { mutableStateOf<Long?>(null) }
    
    // Show popup when new alert is detected (runs on UI thread)
	if (enableEmergencyMonitoring) {
		LaunchedEffect(preparedAlerts.size) {
			val latestAlert = preparedAlerts.lastOrNull()
			if (latestAlert != null && latestAlert.id != lastAlertId) {
				lastAlertId = latestAlert.id
				showAlertPopup = true
				// Hide after 2 seconds
				delay(2000)
				showAlertPopup = false
			}
		}
	}
    
    // Share sensor managers with EmergencyAlertService to avoid duplicates
    // Get sensor flows from SensorDataManager's internal managers
	if (enableEmergencyMonitoring) {
		LaunchedEffect(isMonitoring) {
			if (isMonitoring) {
				// Create managers for emergency detection (separate from graph display)
				val accManager = AccelerometerManager(context)
				val gyroManager = GyroscopeManager(context)
				val micManager = MicrophoneManager(context)
				val gpsMgr = GPSManager(context)
				
				val accFlow = accManager.getAccelerometerData()
				val gyroFlow = gyroManager.getGyroscopeData()
				val micFlow = micManager.getMicrophoneData()
				val locationFlow = gpsMgr.getLocationData()
				
				emergencyService.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
			} else {
				emergencyService.stopMonitoring()
			}
		}
	}

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = "Safety monitoring",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Use the play button to begin monitoring sensors. Press the button again to stop.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                
                EmergencyButton(
                    onMonitoringStateChanged = { isMonitoring = it }
                )

                if (isMonitoring) {
                    SensorGraph(
                        title = "Accelerometer (magnitude m/s²)",
                        data = sensorData.accelerometerReadings,
                        maxValue = 30f,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SensorGraph(
                        title = "Gyroscope (magnitude rad/s)",
                        data = sensorData.gyroscopeReadings,
                        maxValue = 10f,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SensorGraph(
                        title = "Microphone (amplitude %)",
                        data = sensorData.microphoneReadings,
                        maxValue = 100f,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Emergency alert popup overlay - positioned at top center, doesn't block screen
		if (enableEmergencyMonitoring) {
			EmergencyAlertPopup(
				isVisible = showAlertPopup,
				modifier = Modifier
					.align(Alignment.TopCenter)
					.padding(top = 32.dp)
					.zIndex(1000f)
			)
		}
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SafetyTrackerTheme {
        HomeScreen()
    }
}
