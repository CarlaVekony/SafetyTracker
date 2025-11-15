package com.example.safetytracker.network

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.safetytracker.data.model.EmergencyAlert
import com.example.safetytracker.data.model.EmergencyContact
import com.example.safetytracker.data.repository.EmergencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * SMS Manager for sending emergency alerts
 * Handles SMS sending with reliability and error handling
 */
class EmergencySMSManager(
    private val context: Context,
    private val repository: EmergencyRepository
) {
    private val smsManager = SmsManager.getDefault()
    
    companion object {
        private const val TAG = "EmergencySMSManager"
        private const val MAX_SMS_LENGTH = 160 // Standard SMS length
    }
    
    /**
     * Send emergency SMS to all contacts
     * Updates alert status based on results
     */
    suspend fun sendEmergencyAlert(alert: EmergencyAlert): EmergencyAlert {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Sending emergency alert: ${alert.id}")

                // Collect the Flow into a concrete List
                val allContacts = repository.getAllContacts().first()
                val activeContacts = allContacts.filter { it.isActive }

                if (activeContacts.isEmpty()) {
                    Log.w(TAG, "No active emergency contacts found!")
                    return@withContext alert.copy(status = EmergencyAlert.AlertStatus.FAILED)
                }

                Log.i(TAG, "Sending emergency SMS to ${activeContacts.size} active contacts out of ${allContacts.size} total")

                val message = formatEmergencyMessage(alert)
                Log.d(TAG, "Emergency message: $message")

                var allSent = true

                for (contact in activeContacts) {
                    try {
                        sendSMSToContact(contact, message)
                        Log.i(TAG, "SMS sent successfully to ${contact.name}: ${contact.phoneNumber}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to ${contact.name}: ${e.message}")
                        allSent = false
                    }
                }

                val newStatus = if (allSent) {
                    EmergencyAlert.AlertStatus.SENT
                } else {
                    EmergencyAlert.AlertStatus.FAILED
                }

                Log.i(TAG, "Emergency alert ${alert.id} status: $newStatus")
                alert.copy(status = newStatus)

            } catch (e: Exception) {
                Log.e(TAG, "Error sending emergency alert: ${e.message}")
                alert.copy(status = EmergencyAlert.AlertStatus.FAILED)
            }
        }
    }
    
    /**
     * Send SMS to a single contact
     */
    private fun sendSMSToContact(contact: EmergencyContact, message: String) {
        try {
            val messages: ArrayList<String> = if (message.length > MAX_SMS_LENGTH) {
                ArrayList(smsManager.divideMessage(message))
            } else {
                arrayListOf(message)
            }

            if (messages.size == 1) {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(contact.phoneNumber, null, messages, null, null)
            }

            Log.d(TAG, "SMS sent to ${contact.phoneNumber} (${messages.size} parts)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
            throw e
        }
    }
    
    /**
     * Format emergency message with location and details
     */
    private fun formatEmergencyMessage(alert: EmergencyAlert): String {
        val timestamp = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            .format(Date(alert.timestamp))
        
        val location = if (alert.locationLatitude != null && alert.locationLongitude != null) {
            "Location: ${alert.locationLatitude}, ${alert.locationLongitude}"
        } else {
            "Location: Unknown"
        }
        
        val confidence = "${(alert.detectionConfidence * 100).toInt()}%"
        
        return buildString {
            appendLine("🚨 EMERGENCY ALERT 🚨")
            appendLine("FALL DETECTED!")
            appendLine()
            appendLine("Time: $timestamp")
            appendLine("Confidence: $confidence")
            appendLine("Reason: ${alert.detectionReason}")
            appendLine()
            appendLine(location)
            appendLine()
            appendLine("This is an automated emergency alert from SafetyTracker.")
        }
    }
    
    /**
     * Send a test SMS to verify functionality
     */
    suspend fun sendTestMessage(contact: EmergencyContact): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val testMessage = "SafetyTracker test message - your emergency contact is working correctly."
                sendSMSToContact(contact, testMessage)
                Log.i(TAG, "Test SMS sent successfully to ${contact.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Test SMS failed to ${contact.name}: ${e.message}")
                false
            }
        }
    }
}

/**
 * Extension function to check SMS permissions
 */
fun Context.hasSMSPermission(): Boolean {
    return checkSelfPermission(android.Manifest.permission.SEND_SMS) == 
           android.content.pm.PackageManager.PERMISSION_GRANTED
}
