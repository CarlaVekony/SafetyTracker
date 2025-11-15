package com.example.safetytracker.sensors

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FallDetectionAlgorithmTest {
    private lateinit var algorithm: FallDetectionAlgorithm
    private lateinit var config: FallDetectionConfig

    @Before
    fun setUp() {
        config = FallDetectionConfig()
        algorithm = FallDetectionAlgorithm(config)
    }

    // ========== Normal Movement Tests ==========
    
    @Test
    fun `detectFall returns no emergency for normal movement`() {
        val accReading = AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        val gyroReading = GyroscopeReading(0f, 0f, 0f, 0f, System.currentTimeMillis())

        val result = algorithm.detectFall(accReading, gyroReading, 5f)

        assertFalse("Normal movement should not trigger emergency", result.isEmergency)
        assertTrue("Confidence should be low", result.confidence < config.lowConfidenceThreshold)
        assertFalse("Should not require microphone check", result.requiresMicrophoneCheck)
        assertEquals("Reason should indicate normal movement", "Normal movement detected", result.reason)
    }

    @Test
    fun `detectFall ignores unrealistic sensor spikes`() {
        // Values >50 m/s² should be ignored as sensor errors
        val unrealisticAcc = AccelerometerReading(0f, 0f, 60f, 60f, System.currentTimeMillis())
        val result = algorithm.detectFall(unrealisticAcc, null, null)
        
        assertFalse("Unrealistic spike should not trigger emergency", result.isEmergency)
        assertTrue("Confidence should be zero for ignored spikes", result.confidence == 0f)
    }

    // ========== Accelerometer Tests (2.0g threshold) ==========
    
    @Test
    fun `detectFall detects impact at 2_0g threshold`() {
        // Exactly 2.0g (19.6 m/s²) should trigger
        val impactAcc = AccelerometerReading(0f, 0f, 19.6f, 19.6f, System.currentTimeMillis())
        val result = algorithm.detectFall(impactAcc, null, null)
        
        assertTrue("2.0g impact should be detected", result.confidence >= 0.85f)
        assertTrue("Should indicate accelerometer impact", result.reason.contains("Accelerometer impact"))
    }

    @Test
    fun `detectFall detects strong impact above 2_0g`() {
        // >2.4g (23.5 m/s²) should have very high confidence
        val strongImpactAcc = AccelerometerReading(0f, 0f, 25f, 25f, System.currentTimeMillis())
        val result = algorithm.detectFall(strongImpactAcc, null, null)
        
        assertTrue("Strong impact should have very high confidence", result.confidence >= 0.95f)
        assertTrue("Should indicate accelerometer impact", result.reason.contains("Accelerometer impact"))
    }

    @Test
    fun `detectFall detects free fall pattern`() {
        val normalAcc = AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        algorithm.detectFall(normalAcc, null, null) // Set previous reading
        
        // Sudden drop to <50% of normal gravity
        val freeFallAcc = AccelerometerReading(0f, 0f, 4f, 4f, System.currentTimeMillis() + 50)
        val result = algorithm.detectFall(freeFallAcc, null, null)

        assertTrue("Free fall should be detected", result.confidence >= 0.7f)
        assertTrue("Should indicate free fall", result.reason.contains("free fall"))
    }

    @Test
    fun `detectFall detects rapid acceleration change`() {
        val normalAcc = AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        algorithm.detectFall(normalAcc, null, null)
        
        // Rapid change >70% of threshold
        val rapidChangeAcc = AccelerometerReading(0f, 0f, 15f, 15f, System.currentTimeMillis() + 50)
        val result = algorithm.detectFall(rapidChangeAcc, null, null)

        assertTrue("Rapid change should be detected", result.confidence >= 0.6f)
        assertTrue("Should indicate rapid change", result.reason.contains("Rapid acceleration"))
    }

    // ========== Gyroscope Tests (3.0 rad/s threshold) ==========
    
    @Test
    fun `detectFall detects high angular velocity at 3_0 rad_s threshold`() {
        // Exactly 3.0 rad/s should trigger
        val highGyro = GyroscopeReading(3.0f, 0f, 0f, 3.0f, System.currentTimeMillis())
        val result = algorithm.detectFall(null, highGyro, null)

        assertTrue("3.0 rad/s should be detected", result.confidence >= 0.8f)
        assertTrue("Should indicate angular velocity", result.reason.contains("angular velocity"))
    }

    @Test
    fun `detectFall detects high angular velocity above threshold`() {
        val highGyro = GyroscopeReading(5f, 0f, 0f, 5f, System.currentTimeMillis())
        val result = algorithm.detectFall(null, highGyro, null)

        assertTrue("High rotation should be detected", result.confidence >= 0.8f)
    }

    @Test
    fun `detectFall detects angular acceleration at 0_05 rad_s2 threshold`() {
        val timestamp1 = System.currentTimeMillis()
        val gyro1 = GyroscopeReading(1.0f, 0f, 0f, 1.0f, timestamp1)
        algorithm.detectFall(null, gyro1, null)
        
        // Calculate: change of 0.1 rad/s in 1 second = 0.1 rad/s² (above 0.05 threshold)
        val timestamp2 = timestamp1 + 1000 // 1 second later
        val gyro2 = GyroscopeReading(1.1f, 0f, 0f, 1.1f, timestamp2)
        val result = algorithm.detectFall(null, gyro2, null)
        
        // Should detect angular acceleration even if velocity is below 3.0 rad/s
        assertTrue("Angular acceleration should be detected", result.confidence >= 0.7f)
        assertTrue("Should indicate angular acceleration", result.reason.contains("angular acceleration"))
    }

    @Test
    fun `detectFall combines angular velocity and acceleration for high confidence`() {
        val timestamp1 = System.currentTimeMillis()
        val gyro1 = GyroscopeReading(2.5f, 0f, 0f, 2.5f, timestamp1)
        algorithm.detectFall(null, gyro1, null)
        
        // High velocity (3.5 rad/s) with high acceleration (>0.05 rad/s²)
        val timestamp2 = timestamp1 + 100 // 0.1 seconds later
        val gyro2 = GyroscopeReading(3.5f, 0f, 0f, 3.5f, timestamp2)
        val result = algorithm.detectFall(null, gyro2, null)
        
        assertTrue("Combined velocity and acceleration should have high confidence", result.confidence >= 0.9f)
        assertTrue("Should mention both velocity and acceleration", 
            result.reason.contains("angular velocity") && result.reason.contains("acceleration"))
    }

    @Test
    fun `detectFall detects moderate rotation change`() {
        val gyro1 = GyroscopeReading(1.0f, 0f, 0f, 1.0f, System.currentTimeMillis())
        algorithm.detectFall(null, gyro1, null)
        
        // Change of 1.5 rad/s (50% of 3.0 threshold)
        val gyro2 = GyroscopeReading(2.5f, 0f, 0f, 2.5f, System.currentTimeMillis() + 100)
        val result = algorithm.detectFall(null, gyro2, null)
        
        assertTrue("Moderate rotation should be detected", result.confidence >= 0.5f)
    }

    // ========== Microphone Tests (9.5 threshold) ==========
    
    @Test
    fun `detectFall uses microphone at 9_5 threshold for high confidence`() {
        // Medium confidence sensors
        val ambiguousAcc = AccelerometerReading(0f, 0f, 15f, 15f, System.currentTimeMillis())
        val mediumGyro = GyroscopeReading(2.0f, 0f, 0f, 2.0f, System.currentTimeMillis())
        
        // High amplitude (≥9.5) should give high confidence
        val resultHigh = algorithm.detectFall(ambiguousAcc, mediumGyro, 10f)
        assertTrue("High mic amplitude should increase confidence", resultHigh.confidence >= 0.5f)
        
        // Medium amplitude (8-9.5) should give moderate confidence
        val resultMed = algorithm.detectFall(ambiguousAcc, mediumGyro, 8.5f)
        assertTrue("Medium mic amplitude should give moderate confidence", resultMed.confidence >= 0.3f)
        
        // Low amplitude (6-8) should give low confidence
        val resultLow = algorithm.detectFall(ambiguousAcc, mediumGyro, 7f)
        assertTrue("Low mic amplitude should give low confidence", resultLow.confidence < 0.5f)
        
        // Very low amplitude (<6) should give very low confidence
        val resultVeryLow = algorithm.detectFall(ambiguousAcc, mediumGyro, 4f)
        assertTrue("Very low mic amplitude should give very low confidence", resultVeryLow.confidence < 0.3f)
    }

    @Test
    fun `detectFall uses microphone for ambiguous cases`() {
        // Medium confidence: acc shows some change but not definitive
        val normalAcc = AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        algorithm.detectFall(normalAcc, null, null)
        
        // Medium accelerometer reading (below 2.0g threshold)
        val ambiguousAcc = AccelerometerReading(0f, 0f, 15f, 15f, System.currentTimeMillis() + 100)
        // Medium gyroscope reading (below 3.0 rad/s threshold)
        val mediumGyro = GyroscopeReading(2.0f, 0f, 0f, 2.0f, System.currentTimeMillis() + 100)

        // Without microphone: should not trigger (medium confidence)
        val resultNoMic = algorithm.detectFall(ambiguousAcc, mediumGyro, null)
        assertFalse("Ambiguous case without mic should not trigger", resultNoMic.isEmergency)
        assertTrue("Should require microphone check", resultNoMic.requiresMicrophoneCheck)

        // With high microphone amplitude (≥9.5): should trigger
        val resultWithHighMic = algorithm.detectFall(ambiguousAcc, mediumGyro, 10f)
        assertTrue("Ambiguous case with high mic amplitude should trigger", resultWithHighMic.isEmergency)
        assertTrue("Should require microphone check", resultWithHighMic.requiresMicrophoneCheck)
        assertTrue("Confidence should be high", resultWithHighMic.confidence >= config.highConfidenceThreshold)

        // With medium microphone amplitude (8-9.5): might not trigger
        val resultWithMedMic = algorithm.detectFall(ambiguousAcc, mediumGyro, 8.5f)
        // May or may not trigger depending on combined confidence
        assertTrue("Should require microphone check", resultWithMedMic.requiresMicrophoneCheck)

        // With low microphone amplitude (<6): should not trigger
        val resultWithLowMic = algorithm.detectFall(ambiguousAcc, mediumGyro, 5f)
        assertFalse("Ambiguous case with low mic amplitude should not trigger", resultWithLowMic.isEmergency)
    }

    // ========== Combined Sensor Tests ==========
    
    @Test
    fun `detectFall returns emergency for high confidence fall with both sensors`() {
        // High accelerometer (>2.0g) + High gyroscope (>3.0 rad/s)
        val impactAcc = AccelerometerReading(0f, 0f, 22f, 22f, System.currentTimeMillis())
        val highGyro = GyroscopeReading(4f, 0f, 0f, 4f, System.currentTimeMillis())

        val result = algorithm.detectFall(impactAcc, highGyro, null)

        assertTrue("High confidence fall should trigger emergency", result.isEmergency)
        assertTrue("Confidence should be high", result.confidence >= config.highConfidenceThreshold)
        assertFalse("Should not require microphone check for high confidence", result.requiresMicrophoneCheck)
        assertTrue("Should mention both sensors", 
            result.reason.contains("Accelerometer") || result.reason.contains("angular"))
    }

    @Test
    fun `detectFall calculates weighted confidence correctly`() {
        // Accelerometer confidence: 0.85 (impact detected)
        val impactAcc = AccelerometerReading(0f, 0f, 20f, 20f, System.currentTimeMillis())
        // Gyroscope confidence: 0.8 (high velocity)
        val highGyro = GyroscopeReading(3.5f, 0f, 0f, 3.5f, System.currentTimeMillis())
        
        val result = algorithm.detectFall(impactAcc, highGyro, null)
        
        // Weighted: 0.85 * 0.6 + 0.8 * 0.4 = 0.51 + 0.32 = 0.83
        assertTrue("Weighted confidence should be around 0.83", 
            result.confidence >= 0.8f && result.confidence <= 0.9f)
    }

    // ========== Reset Tests ==========
    
    @Test
    fun `reset clears previous readings`() {
        val acc1 = AccelerometerReading(0f, 0f, 9.8f, 9.8f, System.currentTimeMillis())
        algorithm.detectFall(acc1, null, null)
        
        algorithm.reset()
        
        val acc2 = AccelerometerReading(0f, 0f, 20f, 20f, System.currentTimeMillis() + 100)
        val result = algorithm.detectFall(acc2, null, null)
        
        // After reset, there's no previous reading to compare for free fall detection
        // But impact detection should still work
        assertTrue("Impact detection should work after reset", result.confidence >= 0.85f)
    }

    @Test
    fun `reset clears gyroscope timestamp`() {
        val timestamp1 = System.currentTimeMillis()
        val gyro1 = GyroscopeReading(2.0f, 0f, 0f, 2.0f, timestamp1)
        algorithm.detectFall(null, gyro1, null)
        
        algorithm.reset()
        
        // After reset, angular acceleration can't be calculated (no previous timestamp)
        val timestamp2 = timestamp1 + 1000
        val gyro2 = GyroscopeReading(3.5f, 0f, 0f, 3.5f, timestamp2)
        val result = algorithm.detectFall(null, gyro2, null)
        
        // Should still detect high velocity, but not acceleration
        assertTrue("High velocity should still be detected after reset", result.confidence >= 0.8f)
    }
}

