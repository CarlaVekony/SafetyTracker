package com.example.safetytracker.network

import android.content.Context
import android.os.Bundle
import android.app.PendingIntent
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.provider.Telephony
import com.example.safetytracker.data.model.EmergencyAlert
import com.example.safetytracker.data.model.EmergencyContact
import com.example.safetytracker.data.repository.EmergencyRepository
import com.example.safetytracker.media.AudioUtils
import com.example.safetytracker.network.AudioUploadClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import java.util.ArrayList

/**
 * SMS Manager for sending emergency alerts
 * Handles SMS sending with reliability and error handling
 */
class EmergencySMSManager(
    private val context: Context,
    private val repository: EmergencyRepository
) {
    private val smsManager = SmsManager.getDefault()
    private val audioUploader = AudioUploadClient()
    
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

	            // Only attempt MMS if we are the default SMS app (required by many devices)
	            val isDefaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
	            val hasAudio = alert.audioData != null && alert.audioData.isNotEmpty()
	            val audioUri = if (isDefaultSmsApp && hasAudio) {
	                try { AudioUtils.writePcmToWavAndGetUri(context, alert.audioData!!) }
	                catch (e: Exception) { Log.e(TAG, "Failed to prepare audio for MMS: ${e.message}"); null }
	            } else null

	            // If MMS is not possible but we have audio, upload and include link in SMS
	            val uploadedUrl: String? = if (!isDefaultSmsApp && hasAudio) {
	                try {
	                    // Convert PCM to WAV bytes entirely in memory and upload
	                    val wavBytes = AudioUtils.pcmToWavBytes(alert.audioData!!)
	                    if (wavBytes != null) audioUploader.uploadWav(wavBytes) else null
	                } catch (e: Exception) {
	                    Log.e(TAG, "Audio upload failed: ${e.message}")
	                    null
	                }
	            } else null

	            for (contact in activeContacts) {
	                try {
	                    if (audioUri != null) {
	                        sendMMSToContact(contact, message, audioUri)
	                        Log.i(TAG, "MMS sent attempt to ${contact.name}: ${contact.phoneNumber}")
	                    } else {
	                        val smsText = if (uploadedUrl != null) {
	                            "$message\nAudio: $uploadedUrl"
	                        } else {
	                            message
	                        }
	                        sendSMSToContact(contact, smsText)
	                        Log.i(TAG, "SMS sent successfully to ${contact.name}: ${contact.phoneNumber}")
	                    }
	                } catch (e: Exception) {
	                    Log.e(TAG, "Failed to send message to ${contact.name}: ${e.message}")
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
	     * Attempt to send MMS with audio attachment. Falls back to SMS on unsupported devices.
	     */
	    private fun sendMMSToContact(contact: EmergencyContact, text: String, contentUri: android.net.Uri) {
	        try {
	            // Some devices require being default SMS app for MMS; this may fail silently.
	            val config = Bundle()
	            config.putString("address", contact.phoneNumber)

	            // Add subject and text if supported by OEM
	            config.putString("subject", "Emergency audio")
	            config.putString("text", text)

	            val sentIntent = PendingIntent.getBroadcast(
	                context,
	                0,
	                Intent("com.example.safetytracker.MMS_SENT"),
	                PendingIntent.FLAG_IMMUTABLE
	            )

	            smsManager.sendMultimediaMessage(
	                context,
	                contentUri,
	                null,
	                config,
	                sentIntent
	            )
	        } catch (e: Exception) {
	            // If MMS fails for any reason, fallback to SMS
	            Log.e(TAG, "MMS send failed, falling back to SMS: ${e.message}")
	            sendSMSToContact(contact, text)
	        }
	    }
    
    /**
     * Format emergency message with location and details
     */
	    private fun formatEmergencyMessage(alert: EmergencyAlert): String {
        val location = if (alert.locationLatitude != null && alert.locationLongitude != null) {
            "Location: ${alert.locationLatitude}, ${alert.locationLongitude}"
        } else {
            "Location: Unknown"
        }
        
        val confidence = "${(alert.detectionConfidence * 100).toInt()}%"
        
        return buildString {
	            appendLine("EMERGENCY ALERT")
	            appendLine("Confidence: $confidence")
	            appendLine(location)
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
