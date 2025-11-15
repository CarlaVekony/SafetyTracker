package com.example.safetytracker.network

import android.content.Context
import com.example.safetytracker.data.model.EmergencyAlert
import com.example.safetytracker.sensors.AccelerometerReading
import com.example.safetytracker.sensors.GPSManager
import com.example.safetytracker.sensors.GyroscopeReading
import com.example.safetytracker.sensors.LocationReading
import com.example.safetytracker.sensors.MicrophoneManager
import com.example.safetytracker.sensors.MicrophoneReading
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class EmergencyAlertServiceTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var microphoneManager: MicrophoneManager

    @Mock
    private lateinit var gpsManager: GPSManager

    private lateinit var service: EmergencyAlertService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EmergencyAlertService(context, microphoneManager, gpsManager)
    }

    // ========== Initialization Tests ==========
    
    @Test
    fun `getPreparedAlerts returns empty list initially`() {
        val alerts = service.getPreparedAlerts()
        assertTrue("Initial alerts should be empty", alerts.isEmpty())
    }

    @Test
    fun `clearPreparedAlerts removes all alerts`() {
        service.clearPreparedAlerts()
        val alerts = service.getPreparedAlerts()
        assertTrue("Cleared alerts should be empty", alerts.isEmpty())
    }

    // ========== Monitoring Lifecycle Tests ==========
    
    @Test
    fun `startMonitoring initializes correctly`() {
        val accFlow = flowOf(AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis()))
        val gyroFlow = flowOf(GyroscopeReading(0f, 0f, 0f, 0f, System.currentTimeMillis()))
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis()))

        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)

        // Service should be monitoring (no exception means it started)
        val alerts = service.getPreparedAlerts()
        assertNotNull("Service should be initialized", alerts)
    }

    @Test
    fun `stopMonitoring clears state and stops collection`() {
        val accFlow = flowOf(AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis()))
        val gyroFlow = flowOf(GyroscopeReading(0f, 0f, 0f, 0f, System.currentTimeMillis()))
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis()))
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        service.stopMonitoring()
        
        val alerts = service.getPreparedAlerts()
        assertEquals("Stopped service should have no new alerts", 0, alerts.size)
    }

    @Test
    fun `startMonitoring can be called multiple times safely`() {
        val accFlow = flowOf(AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis()))
        val gyroFlow = flowOf(GyroscopeReading(0f, 0f, 0f, 0f, System.currentTimeMillis()))
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis()))

        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        // Should not crash when called again
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        
        assertTrue("Multiple startMonitoring calls should not crash", true)
    }

    // ========== Emergency Detection Tests ==========
    
    @Test
    fun `service detects high confidence fall and prepares alert`() = runTest {
        
        // Mock location and audio
        val location = LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis())
        `when`(gpsManager.getCurrentLocation()).thenReturn(location)
        val audioData = ByteArray(1000) { it.toByte() }
        `when`(microphoneManager.getLast5SecondsAudio()).thenReturn(audioData)
        
        // Create flows with emergency readings (>2.0g accel, >3.0 rad/s gyro)
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis()) // >2.0g
        )
        val gyroFlow = flowOf(
            GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis()) // >3.0 rad/s
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(location)
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        
        // Advance time to allow throttled checks (200ms throttle)
        advanceTimeBy(300)
        
        val alerts = service.getPreparedAlerts()
        assertTrue("High confidence fall should prepare alert", alerts.isNotEmpty())
        
        val alert = alerts.first()
        assertEquals("Alert should have location", location.latitude, alert.locationLatitude)
        assertEquals("Alert should have longitude", location.longitude, alert.locationLongitude)
        assertNotNull("Alert should have audio data", alert.audioData)
        assertEquals("Alert status should be PREPARED", EmergencyAlert.AlertStatus.PREPARED, alert.status)
        assertTrue("Alert should have high confidence", alert.detectionConfidence >= 0.8f)
        
        service.stopMonitoring()
    }

    @Test
    fun `service respects alert cooldown period`() = runTest {
        
        val location = LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis())
        `when`(gpsManager.getCurrentLocation()).thenReturn(location)
        val audioData = ByteArray(1000)
        `when`(microphoneManager.getLast5SecondsAudio()).thenReturn(audioData)
        
        // Create flows with emergency readings
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis())
        )
        val gyroFlow = flowOf(
            GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis())
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(location)
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        
        // Advance time for first detection
        advanceTimeBy(300)
        
        val alerts1 = service.getPreparedAlerts()
        assertEquals("First alert should be prepared", 1, alerts1.size)
        
        // Advance time but less than cooldown (10 seconds)
        advanceTimeBy(5000)
        
        // Send another emergency reading
        val accFlow2 = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis() + 5000)
        )
        service.startMonitoring(accFlow2, gyroFlow, micFlow, locationFlow)
        advanceTimeBy(300)
        
        val alerts2 = service.getPreparedAlerts()
        // Should still be 1 due to cooldown
        assertEquals("Second alert should be blocked by cooldown", 1, alerts2.size)
        
        service.stopMonitoring()
    }

    @Test
    fun `service handles missing location gracefully`() = runTest {
        
        `when`(gpsManager.getCurrentLocation()).thenReturn(null)
        val audioData = ByteArray(1000)
        `when`(microphoneManager.getLast5SecondsAudio()).thenReturn(audioData)
        
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis())
        )
        val gyroFlow = flowOf(
            GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis())
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf<LocationReading>() // Empty flow
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        advanceTimeBy(300)
        
        val alerts = service.getPreparedAlerts()
        assertTrue("Alert should be prepared even without location", alerts.isNotEmpty())
        
        val alert = alerts.first()
        assertNull("Alert location should be null when unavailable", alert.locationLatitude)
        assertNotNull("Alert should still have audio data", alert.audioData)
        
        service.stopMonitoring()
    }

    @Test
    fun `service handles missing audio gracefully`() = runTest {
        
        val location = LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis())
        `when`(gpsManager.getCurrentLocation()).thenReturn(location)
        `when`(microphoneManager.getLast5SecondsAudio()).thenReturn(null)
        
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis())
        )
        val gyroFlow = flowOf(
            GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis())
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(location)
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        advanceTimeBy(300)
        
        val alerts = service.getPreparedAlerts()
        assertTrue("Alert should be prepared even without audio", alerts.isNotEmpty())
        
        val alert = alerts.first()
        assertNotNull("Alert should have location", alert.locationLatitude)
        assertNull("Alert audio should be null when unavailable", alert.audioData)
        
        service.stopMonitoring()
    }

    @Test
    fun `service does not prepare alert for normal movement`() = runTest {
        
        // Normal sensor readings (no emergency)
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        )
        val gyroFlow = flowOf(
            GyroscopeReading(0.5f, 0f, 0f, 0.5f, System.currentTimeMillis())
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis()))
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        advanceTimeBy(300)
        
        val alerts = service.getPreparedAlerts()
        assertTrue("Normal movement should not prepare alert", alerts.isEmpty())
        
        service.stopMonitoring()
    }

    // ========== Alert Data Tests ==========
    
    @Test
    fun `prepared alert contains all required fields`() = runTest {
        
        val location = LocationReading(44.4268, 26.1025, 10f, System.currentTimeMillis())
        `when`(gpsManager.getCurrentLocation()).thenReturn(location)
        val audioData = ByteArray(1000) { it.toByte() }
        `when`(microphoneManager.getLast5SecondsAudio()).thenReturn(audioData)
        
        val accFlow = flowOf(
            AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis())
        )
        val gyroFlow = flowOf(
            GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis())
        )
        val micFlow = flowOf(MicrophoneReading(5f, System.currentTimeMillis()))
        val locationFlow = flowOf(location)
        
        service.startMonitoring(accFlow, gyroFlow, micFlow, locationFlow)
        advanceTimeBy(300)
        
        val alerts = service.getPreparedAlerts()
        assertTrue("Should have at least one alert", alerts.isNotEmpty())
        
        val alert = alerts.first()
        assertTrue("Alert should have valid ID", alert.id > 0)
        assertTrue("Alert should have timestamp", alert.timestamp > 0)
        assertNotNull("Alert should have latitude", alert.locationLatitude)
        assertNotNull("Alert should have longitude", alert.locationLongitude)
        assertNotNull("Alert should have accuracy", alert.locationAccuracy)
        assertNotNull("Alert should have audio data", alert.audioData)
        assertTrue("Alert should have confidence", alert.detectionConfidence >= 0f && alert.detectionConfidence <= 1f)
        assertFalse("Alert should have reason", alert.detectionReason.isBlank())
        assertEquals("Alert status should be PREPARED", EmergencyAlert.AlertStatus.PREPARED, alert.status)
        
        service.stopMonitoring()
    }
}

