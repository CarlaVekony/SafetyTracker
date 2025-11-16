package com.example.safetytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_alerts")
data class EmergencyAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val timestamp: Long,

    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationAccuracy: Float?,

    val audioData: ByteArray?, // 5 seconds of audio

    val detectionConfidence: Float,
    val detectionReason: String,

    val status: AlertStatus
) {
    enum class AlertStatus {
        PREPARED,
        SENT,
        FAILED,
        ACKNOWLEDGED
    }
}
