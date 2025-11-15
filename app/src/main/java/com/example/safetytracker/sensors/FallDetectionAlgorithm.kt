package com.example.safetytracker.sensors

import kotlin.math.abs

/**
 * Configuration for fall detection thresholds
 * Based on research: Bourke & Lyons, BioMed Central, PubMed studies
 */
data class FallDetectionConfig(
    // Accelerometer thresholds (based on research)
    // Lower threshold (impact detection): ~2.0 g = ~19.6 m/s²
    val accFallThreshold: Float = 19.6f, // m/s² (2.0 g) - impact detection threshold
    val accNormalRange: Float = 9.8f, // m/s² - normal gravity (1.0 g)
    val accMaxThreshold: Float = 15f, // m/s² - upper bound to ignore unrealistic spikes
    
    // Gyroscope thresholds (based on Bourke & Lyons research)
    val gyroAngularVelocityThreshold: Float = 3.0f, // rad/s - angular velocity threshold
    val gyroAngularAccelerationThreshold: Float = 0.05f, // rad/s² - angular acceleration threshold
    
    // Microphone thresholds (based on multi-sensor study)
    // Research found: values 6-8 = false positives, >10 = false negatives
    // Use 9-10 range for trigger
    val micAmplitudeThreshold: Float = 9.5f, // amplitude (0-100 scale) - high amplitude indicates distress
    
    // Confidence levels
    val highConfidenceThreshold: Float = 0.8f, // 80% confidence needed for automatic alert
    val lowConfidenceThreshold: Float = 0.5f // 50% confidence needed to check microphone
)

/**
 * Result of fall detection analysis
 */
data class FallDetectionResult(
    val isEmergency: Boolean,
    val confidence: Float, // 0.0 to 1.0
    val reason: String,
    val requiresMicrophoneCheck: Boolean // If true, check microphone amplitude
)

/**
 * Fall detection algorithm that combines accelerometer, gyroscope, and microphone data
 */
class FallDetectionAlgorithm(
    private val config: FallDetectionConfig = FallDetectionConfig()
) {
    private var previousAccReading: AccelerometerReading? = null
    private var previousGyroReading: GyroscopeReading? = null
    private var previousGyroTimestamp: Long? = null
    
    /**
     * Analyze sensor data to detect falls
     * @param accReading Current accelerometer reading
     * @param gyroReading Current gyroscope reading
     * @param micAmplitude Current microphone amplitude (0-100)
     * @return FallDetectionResult indicating if emergency is detected
     */
    fun detectFall(
        accReading: AccelerometerReading?,
        gyroReading: GyroscopeReading?,
        micAmplitude: Float? = null
    ): FallDetectionResult {
        var accConfidence = 0f
        var gyroConfidence = 0f
        var reasons = mutableListOf<String>()
        
        // Analyze accelerometer
        if (accReading != null) {
            val accConf = analyzeAccelerometer(accReading)
            accConfidence = accConf.first
            if (accConf.first > 0) {
                reasons.add(accConf.second)
            }
            previousAccReading = accReading
        }
        
        // Analyze gyroscope
        if (gyroReading != null) {
            val gyroConf = analyzeGyroscope(gyroReading)
            gyroConfidence = gyroConf.first
            if (gyroConf.first > 0) {
                reasons.add(gyroConf.second)
            }
            previousGyroReading = gyroReading
            previousGyroTimestamp = gyroReading.timestamp
        }
        
        // Calculate overall confidence
        // Weight: 60% accelerometer, 40% gyroscope
        val overallConfidence = (accConfidence * 0.6f + gyroConfidence * 0.4f)
        
        // High confidence: both sensors indicate fall
        if (overallConfidence >= config.highConfidenceThreshold) {
            return FallDetectionResult(
                isEmergency = true,
                confidence = overallConfidence,
                reason = "High confidence fall detected: ${reasons.joinToString(", ")}",
                requiresMicrophoneCheck = false
            )
        }
        
        // Medium confidence: check microphone for confirmation
        if (overallConfidence >= config.lowConfidenceThreshold) {
            val micConfidence = analyzeMicrophone(micAmplitude)
            val finalConfidence = overallConfidence * 0.7f + micConfidence * 0.3f
            
            if (finalConfidence >= config.highConfidenceThreshold) {
                return FallDetectionResult(
                    isEmergency = true,
                    confidence = finalConfidence,
                    reason = "Fall detected with microphone confirmation: ${reasons.joinToString(", ")}",
                    requiresMicrophoneCheck = true
                )
            } else {
                return FallDetectionResult(
                    isEmergency = false,
                    confidence = finalConfidence,
                    reason = "Ambiguous movement detected but microphone doesn't confirm emergency",
                    requiresMicrophoneCheck = true
                )
            }
        }
        
        // Low confidence: no emergency
        return FallDetectionResult(
            isEmergency = false,
            confidence = overallConfidence,
            reason = "Normal movement detected",
            requiresMicrophoneCheck = false
        )
    }
    
    /**
     * Analyze accelerometer reading for fall patterns
     * Based on research: falls typically show >2.0g (19.6 m/s²) impact
     * Returns (confidence 0-1, reason string)
     */
    private fun analyzeAccelerometer(reading: AccelerometerReading): Pair<Float, String> {
        // Ignore unrealistic spikes (sensor errors - values >50 m/s² are likely errors)
        val maxRealisticValue = 50f // m/s² - ignore values above this (sensor errors)
        if (reading.magnitude > maxRealisticValue) {
            return Pair(0f, "Unrealistic spike ignored (sensor error)")
        }
        
        // Check for impact detection: >2.0g (19.6 m/s²) threshold
        // This is the primary indicator based on research
        if (reading.magnitude >= config.accFallThreshold) {
            // High confidence if impact is clearly above threshold
            val confidence = if (reading.magnitude >= config.accFallThreshold * 1.2f) {
                0.95f // Very high confidence for strong impacts (>2.4g)
            } else {
                0.85f // High confidence for threshold-level impacts (≥2.0g)
            }
            return Pair(confidence, "Accelerometer impact detected (≥2.0g)")
        }
        
        // Check for sudden change (free fall followed by impact)
        if (previousAccReading != null) {
            val magnitudeChange = abs(reading.magnitude - previousAccReading!!.magnitude)
            
            // Sudden drop in magnitude (free fall) - magnitude drops significantly
            if (reading.magnitude < config.accNormalRange * 0.5f && 
                magnitudeChange > config.accFallThreshold * 0.5f) {
                return Pair(0.7f, "Sudden accelerometer drop (free fall) detected")
            }
            
            // Rapid change indicating impact
            if (magnitudeChange > config.accFallThreshold * 0.7f) {
                return Pair(0.6f, "Rapid acceleration change detected")
            }
        }
        
        return Pair(0f, "")
    }
    
    /**
     * Analyze gyroscope reading for fall patterns
     * Based on Bourke & Lyons research: >3.0 rad/s angular velocity indicates fall
     * Also checks angular acceleration >0.05 rad/s²
     * Returns (confidence 0-1, reason string)
     */
    private fun analyzeGyroscope(reading: GyroscopeReading): Pair<Float, String> {
        // Check for angular velocity threshold: >3.0 rad/s (Bourke & Lyons)
        if (reading.magnitude >= config.gyroAngularVelocityThreshold) {
            var confidence = 0.8f
            var reason = "High angular velocity detected (≥3.0 rad/s)"
            
            // Check for angular acceleration if we have previous reading
            if (previousGyroReading != null && previousGyroTimestamp != null) {
                val timeDelta = (reading.timestamp - previousGyroTimestamp!!) / 1000.0f // Convert to seconds
                if (timeDelta > 0) {
                    val angularAcceleration = abs(reading.magnitude - previousGyroReading!!.magnitude) / timeDelta
                    
                    // Angular acceleration >0.05 rad/s² confirms fall (Bourke & Lyons)
                    if (angularAcceleration >= config.gyroAngularAccelerationThreshold) {
                        confidence = 0.9f
                        reason = "High angular velocity (≥3.0 rad/s) with acceleration (≥0.05 rad/s²)"
                    }
                }
            }
            
            return Pair(confidence, reason)
        }
        
        // Check for rapid rotation change (angular acceleration)
        if (previousGyroReading != null && previousGyroTimestamp != null) {
            val timeDelta = (reading.timestamp - previousGyroTimestamp!!) / 1000.0f
            if (timeDelta > 0) {
                val angularAcceleration = abs(reading.magnitude - previousGyroReading!!.magnitude) / timeDelta
                
                // High angular acceleration even if velocity is below threshold
                if (angularAcceleration >= config.gyroAngularAccelerationThreshold) {
                    return Pair(0.7f, "High angular acceleration detected (≥0.05 rad/s²)")
                }
            }
        }
        
        // Check for moderate rotation change
        if (previousGyroReading != null) {
            val rotationChange = abs(reading.magnitude - previousGyroReading!!.magnitude)
            if (rotationChange > config.gyroAngularVelocityThreshold * 0.5f) {
                return Pair(0.5f, "Moderate rotation change detected")
            }
        }
        
        return Pair(0f, "")
    }
    
    /**
     * Analyze microphone amplitude for distress sounds
     * Based on research: values 6-8 = false positives, >10 = false negatives
     * Use 9-10 range (9.5 threshold) for trigger
     * Returns confidence 0-1
     */
    private fun analyzeMicrophone(amplitude: Float?): Float {
        if (amplitude == null) return 0f
        
        // High amplitude (≥9.5) indicates distress sound (scream, shout, impact)
        // Research found this range reduces false positives while catching real falls
        if (amplitude >= config.micAmplitudeThreshold) {
            return 0.7f // High confidence for distress sounds
        }
        
        // Medium-high amplitude (8-9.5) might indicate distress but less certain
        if (amplitude >= 8f) {
            return 0.4f // Moderate confidence
        }
        
        // Lower amplitude (6-8) - research shows these are often false positives
        // Only give low confidence
        if (amplitude >= 6f) {
            return 0.2f // Low confidence (likely false positive)
        }
        
        // Very low amplitude (silence or normal sounds) doesn't confirm emergency
        return 0.1f
    }
    
    /**
     * Reset the algorithm state (clear previous readings)
     */
    fun reset() {
        previousAccReading = null
        previousGyroReading = null
        previousGyroTimestamp = null
    }
}
