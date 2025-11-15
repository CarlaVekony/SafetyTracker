package com.example.safetytracker.data.model

import org.junit.Assert.*
import org.junit.Test

class EmergencyAlertTest {
    
    @Test
    fun `EmergencyAlert equals works correctly`() {
        val audio1 = ByteArray(100) { it.toByte() }
        val audio2 = ByteArray(100) { it.toByte() }
        
        val alert1 = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = audio1,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        val alert2 = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = audio1,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        val alert3 = EmergencyAlert(
            id = 2L, // Different ID
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = audio1,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        assertEquals("Alerts with same data should be equal", alert1, alert2)
        assertNotEquals("Alerts with different IDs should not be equal", alert1, alert3)
    }
    
    @Test
    fun `EmergencyAlert equals handles null audio data`() {
        val alert1 = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = null,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        val alert2 = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = null,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        val alert3 = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = ByteArray(10),
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        assertEquals("Alerts with both null audio should be equal", alert1, alert2)
        assertNotEquals("Alerts with null vs non-null audio should not be equal", alert1, alert3)
    }
    
    @Test
    fun `EmergencyAlert hashCode is consistent`() {
        val audio = ByteArray(100) { it.toByte() }
        val alert = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 10f,
            audioData = audio,
            detectionConfidence = 0.9f,
            detectionReason = "Test reason",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        val hashCode1 = alert.hashCode()
        val hashCode2 = alert.hashCode()
        
        assertEquals("Hash code should be consistent", hashCode1, hashCode2)
    }
    
    @Test
    fun `EmergencyAlert AlertStatus enum values are correct`() {
        assertEquals("PREPARED should exist", EmergencyAlert.AlertStatus.PREPARED, EmergencyAlert.AlertStatus.valueOf("PREPARED"))
        assertEquals("SENT should exist", EmergencyAlert.AlertStatus.SENT, EmergencyAlert.AlertStatus.valueOf("SENT"))
        assertEquals("FAILED should exist", EmergencyAlert.AlertStatus.FAILED, EmergencyAlert.AlertStatus.valueOf("FAILED"))
        assertEquals("ACKNOWLEDGED should exist", EmergencyAlert.AlertStatus.ACKNOWLEDGED, EmergencyAlert.AlertStatus.valueOf("ACKNOWLEDGED"))
    }
    
    @Test
    fun `EmergencyAlert can be created with all fields`() {
        val audio = ByteArray(100) { it.toByte() }
        val alert = EmergencyAlert(
            id = 12345L,
            timestamp = 9876543210L,
            locationLatitude = 44.4268,
            locationLongitude = 26.1025,
            locationAccuracy = 15.5f,
            audioData = audio,
            detectionConfidence = 0.85f,
            detectionReason = "High confidence fall detected",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        assertEquals("ID should match", 12345L, alert.id)
        assertEquals("Timestamp should match", 9876543210L, alert.timestamp)
        assertEquals("Latitude should match", 44.4268, alert.locationLatitude!!, 0.0001)
        assertEquals("Longitude should match", 26.1025, alert.locationLongitude!!, 0.0001)
        assertEquals("Accuracy should match", 15.5f, alert.locationAccuracy!!, 0.1f)
        assertArrayEquals("Audio data should match", audio, alert.audioData!!)
        assertEquals("Confidence should match", 0.85f, alert.detectionConfidence, 0.01f)
        assertEquals("Reason should match", "High confidence fall detected", alert.detectionReason)
        assertEquals("Status should match", EmergencyAlert.AlertStatus.PREPARED, alert.status)
    }
    
    @Test
    fun `EmergencyAlert can be created with null optional fields`() {
        val alert = EmergencyAlert(
            id = 1L,
            timestamp = 1000L,
            locationLatitude = null,
            locationLongitude = null,
            locationAccuracy = null,
            audioData = null,
            detectionConfidence = 0.9f,
            detectionReason = "Test",
            status = EmergencyAlert.AlertStatus.PREPARED
        )
        
        assertNull("Latitude should be null", alert.locationLatitude)
        assertNull("Longitude should be null", alert.locationLongitude)
        assertNull("Accuracy should be null", alert.locationAccuracy)
        assertNull("Audio data should be null", alert.audioData)
    }
}

