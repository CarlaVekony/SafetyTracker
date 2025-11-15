package com.example.safetytracker.network

import android.content.Context
import android.util.Log
import com.example.safetytracker.data.model.EmergencyAlert
import com.example.safetytracker.sensors.AccelerometerReading
import com.example.safetytracker.sensors.FallDetectionAlgorithm
import com.example.safetytracker.sensors.FallDetectionResult
import com.example.safetytracker.sensors.GPSManager
import com.example.safetytracker.sensors.GyroscopeReading
import com.example.safetytracker.sensors.LocationReading
import com.example.safetytracker.sensors.MicrophoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service that coordinates emergency detection and alert preparation
 * Does NOT send alerts, only prepares them
 */
class EmergencyAlertService(
    private val context: Context,
    private val microphoneManager: MicrophoneManager,
    private val gpsManager: GPSManager
) {
    private val fallDetectionAlgorithm = FallDetectionAlgorithm()
    
    private val _preparedAlerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val preparedAlerts: StateFlow<List<EmergencyAlert>> = _preparedAlerts.asStateFlow()
    
    private var latestAccReading: AccelerometerReading? = null
    private var latestGyroReading: GyroscopeReading? = null
    private var latestMicAmplitude: Float? = null
    private var latestLocation: LocationReading? = null
    
    private var isMonitoring = false
    private var monitoringScope: CoroutineScope? = null
    private var lastAlertTime: Long = 0
    private var lastCheckTime: Long = 0
    private val alertCooldownMs = 10000L // 10 seconds cooldown between alerts
    private val checkThrottleMs = 200L // Only check for emergency every 200ms (throttle)
    
    companion object {
        private const val TAG = "EmergencyAlertService"
    }
    
    /**
     * Start monitoring sensors and detecting emergencies
     */
    fun startMonitoring(
        accFlow: Flow<AccelerometerReading>?,
        gyroFlow: Flow<GyroscopeReading>?,
        micFlow: Flow<com.example.safetytracker.sensors.MicrophoneReading>?,
        locationFlow: Flow<LocationReading>?
    ) {
        if (isMonitoring) {
            Log.w(TAG, "Monitoring already started")
            return
        }
        
        isMonitoring = true
        fallDetectionAlgorithm.reset()
        lastAlertTime = 0 // Reset cooldown when starting monitoring
        
        val scope = CoroutineScope(Dispatchers.Default)
        monitoringScope = scope
        
        // Collect accelerometer data (throttled - don't check on every reading)
        accFlow?.let { flow ->
            scope.launch(Dispatchers.Default) {
                flow.collect { reading ->
                    latestAccReading = reading
                    // Throttle emergency checks to avoid blocking
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCheckTime >= checkThrottleMs) {
                        lastCheckTime = currentTime
                        checkForEmergency()
                    }
                }
            }
        }
        
        // Collect gyroscope data (throttled)
        gyroFlow?.let { flow ->
            scope.launch(Dispatchers.Default) {
                flow.collect { reading ->
                    latestGyroReading = reading
                    // Emergency check is throttled in accelerometer collector
                }
            }
        }
        
        // Collect microphone data (throttled)
        micFlow?.let { flow ->
            scope.launch(Dispatchers.Default) {
                flow.collect { reading ->
                    latestMicAmplitude = reading.amplitude
                    // Emergency check is throttled in accelerometer collector
                }
            }
        }
        
        // Collect location data
        locationFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    latestLocation = reading
                }
            }
        }
        
        Log.i(TAG, "Emergency monitoring started")
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringScope?.cancel()
        monitoringScope = null
        latestAccReading = null
        latestGyroReading = null
        latestMicAmplitude = null
        latestLocation = null
        Log.i(TAG, "Emergency monitoring stopped")
    }
    
    /**
     * Check if current sensor readings indicate an emergency
     * Monitoring continues after alerts are prepared
     */
    private fun checkForEmergency() {
        if (!isMonitoring) return
        
        val result = fallDetectionAlgorithm.detectFall(
            accReading = latestAccReading,
            gyroReading = latestGyroReading,
            micAmplitude = latestMicAmplitude
        )
        
        if (result.isEmergency) {
            val currentTime = System.currentTimeMillis()
            // Only prepare alert if cooldown period has passed (prevents spam)
            if (currentTime - lastAlertTime >= alertCooldownMs) {
                Log.w(TAG, "Emergency detected! Confidence: ${result.confidence}, Reason: ${result.reason}")
                prepareEmergencyAlert(result)
                lastAlertTime = currentTime
            } else {
                Log.d(TAG, "Emergency detected but in cooldown period. Monitoring continues...")
            }
        }
        // Monitoring continues regardless of whether alert was prepared
    }
    
    /**
     * Prepare an emergency alert with location and audio
     * Does NOT send the alert, only prepares it
     * Runs on background thread to avoid blocking
     */
    private fun prepareEmergencyAlert(detectionResult: FallDetectionResult) {
        // Run heavy operations on background thread
        monitoringScope?.launch(Dispatchers.IO) {
            val location = latestLocation ?: gpsManager.getCurrentLocation()
            val audioData = microphoneManager.getLast5SecondsAudio()
            
            val alert = EmergencyAlert(
                id = System.currentTimeMillis(), // Use timestamp as ID
                timestamp = System.currentTimeMillis(),
                locationLatitude = location?.latitude,
                locationLongitude = location?.longitude,
                locationAccuracy = location?.accuracy,
                audioData = audioData,
                detectionConfidence = detectionResult.confidence,
                detectionReason = detectionResult.reason,
                status = EmergencyAlert.AlertStatus.PREPARED
            )
            
            // Add to prepared alerts list (StateFlow update is thread-safe)
            _preparedAlerts.value = _preparedAlerts.value + alert
            
            Log.i(TAG, "Emergency alert prepared: ID=${alert.id}, Location=${location?.latitude},${location?.longitude}, AudioSize=${audioData?.size ?: 0} bytes")
        }
    }
    
    /**
     * Get all prepared alerts
     */
    fun getPreparedAlerts(): List<EmergencyAlert> {
        return _preparedAlerts.value
    }
    
    /**
     * Clear prepared alerts
     */
    fun clearPreparedAlerts() {
        _preparedAlerts.value = emptyList()
    }
}
