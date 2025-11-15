package com.example.safetytracker.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.safetytracker.sensors.AccelerometerManager
import com.example.safetytracker.sensors.GyroscopeManager
import com.example.safetytracker.sensors.MicrophoneManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SensorDataState(
    val accelerometerReadings: List<Float> = emptyList(),
    val gyroscopeReadings: List<Float> = emptyList(),
    val microphoneReadings: List<Float> = emptyList(),
)

@Composable
fun rememberSensorData(
    isMonitoring: Boolean,
): SensorDataState {
    val context = LocalContext.current
    var accelerometerReadings by remember { mutableStateOf<List<Float>>(emptyList()) }
    var gyroscopeReadings by remember { mutableStateOf<List<Float>>(emptyList()) }
    var microphoneReadings by remember { mutableStateOf<List<Float>>(emptyList()) }

    var latestAccReading by remember { mutableStateOf<Float?>(null) }
    var latestGyroReading by remember { mutableStateOf<Float?>(null) }
    var latestMicReading by remember { mutableStateOf<Float?>(null) }

    var accelerometerManager: AccelerometerManager? = remember { null }
    var gyroscopeManager: GyroscopeManager? = remember { null }
    var microphoneManager: MicrophoneManager? = remember { null }
    
    // Use a mutable list to track readings and update state atomically
    val accList = remember { mutableListOf<Float>() }
    val gyroList = remember { mutableListOf<Float>() }
    val micList = remember { mutableListOf<Float>() }

    LaunchedEffect(isMonitoring) {
        if (!isMonitoring) {
            // Stop all sensors and clear data
            accList.clear()
            gyroList.clear()
            micList.clear()
            accelerometerReadings = emptyList()
            gyroscopeReadings = emptyList()
            microphoneReadings = emptyList()
            latestAccReading = null
            latestGyroReading = null
            latestMicReading = null
            accelerometerManager?.stop()
            gyroscopeManager?.stop()
            microphoneManager?.stop()
            accelerometerManager = null
            gyroscopeManager = null
            microphoneManager = null
            return@LaunchedEffect
        }

        // Start sensors only when monitoring
        accelerometerManager = AccelerometerManager(context)
        gyroscopeManager = GyroscopeManager(context)
        microphoneManager = MicrophoneManager(context)

        val accFlow = accelerometerManager?.getAccelerometerData()?.catch { e ->
            // Log error but don't crash
            android.util.Log.e("SensorDataManager", "Accelerometer error", e)
        }
        val gyroFlow = gyroscopeManager?.getGyroscopeData()?.catch { e ->
            // Log error but don't crash
            android.util.Log.e("SensorDataManager", "Gyroscope error", e)
        }
        val micFlow = microphoneManager?.getMicrophoneData()?.catch { e ->
            // Log error but don't crash
            android.util.Log.e("SensorDataManager", "Microphone error", e)
        }

        // Throttle state updates to avoid too many recompositions (update every 100ms)
        val updateThrottleMs = 100L
        var lastAccUpdateTime = 0L
        var lastGyroUpdateTime = 0L
        var lastMicUpdateTime = 0L
        
        // Collect accelerometer data continuously and update graph (throttled)
        launch {
            accFlow?.collect { reading ->
                if (isActive) {
                    latestAccReading = reading.magnitude
                    // Update mutable list immediately
                    accList.add(reading.magnitude)
                    if (accList.size > 30) {
                        accList.removeAt(0)
                    }
                    // Throttle state updates to avoid blocking UI
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastAccUpdateTime >= updateThrottleMs) {
                        lastAccUpdateTime = currentTime
                        accelerometerReadings = accList.toList()
                    }
                }
            }
        }

        // Collect gyroscope data continuously and update graph (throttled)
        launch {
            gyroFlow?.collect { reading ->
                if (isActive) {
                    latestGyroReading = reading.magnitude
                    // Update mutable list immediately
                    gyroList.add(reading.magnitude)
                    if (gyroList.size > 30) {
                        gyroList.removeAt(0)
                    }
                    // Throttle state updates
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastGyroUpdateTime >= updateThrottleMs) {
                        lastGyroUpdateTime = currentTime
                        gyroscopeReadings = gyroList.toList()
                    }
                }
            }
        }

        // Collect microphone data continuously and update graph (throttled)
        launch {
            micFlow?.collect { reading ->
                if (isActive) {
                    latestMicReading = reading.amplitude
                    // Update mutable list immediately
                    micList.add(reading.amplitude)
                    if (micList.size > 30) {
                        micList.removeAt(0)
                    }
                    // Throttle state updates
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMicUpdateTime >= updateThrottleMs) {
                        lastMicUpdateTime = currentTime
                        microphoneReadings = micList.toList()
                    }
                }
            }
        }
    }

    return SensorDataState(
        accelerometerReadings = accelerometerReadings,
        gyroscopeReadings = gyroscopeReadings,
        microphoneReadings = microphoneReadings,
    )
}
