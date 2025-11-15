package com.example.safetytracker.data.model

/**
 * Represents an emergency alert that has been detected and prepared
 */
data class EmergencyAlert(
    val id: Long,
    val timestamp: Long,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationAccuracy: Float?,
    val audioData: ByteArray?, // 5 seconds of audio recording
    val detectionConfidence: Float,
    val detectionReason: String,
    val status: AlertStatus
) {
    enum class AlertStatus {
        PREPARED,    // Alert has been prepared but not sent
        SENT,        // Alert has been sent successfully
        FAILED,      // Alert sending failed
        ACKNOWLEDGED // Alert has been acknowledged by recipient
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmergencyAlert

        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (locationLatitude != other.locationLatitude) return false
        if (locationLongitude != other.locationLongitude) return false
        if (locationAccuracy != other.locationAccuracy) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false
        if (detectionConfidence != other.detectionConfidence) return false
        if (detectionReason != other.detectionReason) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (locationLatitude?.hashCode() ?: 0)
        result = 31 * result + (locationLongitude?.hashCode() ?: 0)
        result = 31 * result + (locationAccuracy?.hashCode() ?: 0)
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        result = 31 * result + detectionConfidence.hashCode()
        result = 31 * result + detectionReason.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}
