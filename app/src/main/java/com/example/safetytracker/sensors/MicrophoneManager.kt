package com.example.safetytracker.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

data class MicrophoneReading(
    val amplitude: Float,
    val timestamp: Long
)

class MicrophoneManager(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // 5-second rolling buffer: 44100 samples/sec * 2 bytes/sample * 5 seconds = 441000 bytes
    private val audioBufferSize = sampleRate * 2 * 5 // 5 seconds of audio
    // Use ByteArray for better performance than MutableList
    private val audioBuffer = ByteArray(audioBufferSize)
    private var bufferWriteIndex = 0
    private var bufferFilled = false
    private val bufferLock = Any()

    fun getMicrophoneData(): Flow<MicrophoneReading> = callbackFlow {
        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }

        // Check if buffer size is valid (ERROR_BAD_VALUE is returned as a negative value)
        if (bufferSize <= 0) {
            close()
            return@callbackFlow
        }

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .build()

            audioRecord?.let { recorder ->
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    close()
                    return@callbackFlow
                }

                try {
                    isRecording = true
                    recorder.startRecording()

                    val buffer = ShortArray(bufferSize)

                    while (isRecording) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            var maxAmplitude = 0
                            for (i in 0 until read) {
                                maxAmplitude = max(maxAmplitude, abs(buffer[i].toInt()))
                            }
                            
                            // Normalize amplitude to 0-100 range
                            val normalizedAmplitude = (maxAmplitude / 32768.0f) * 100f
                            
                            // Store audio data in rolling buffer (keep last 5 seconds)
                            // Use efficient circular buffer instead of list operations
                            synchronized(bufferLock) {
                                // Convert Short array to Byte array and write to circular buffer
                                for (i in 0 until read) {
                                    val sample = buffer[i]
                                    audioBuffer[bufferWriteIndex] = (sample.toInt() and 0xFF).toByte()
                                    audioBuffer[bufferWriteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                                    
                                    bufferWriteIndex = (bufferWriteIndex + 2) % audioBufferSize
                                    if (bufferWriteIndex == 0) {
                                        bufferFilled = true
                                    }
                                }
                            }
                            
                            val reading = MicrophoneReading(
                                amplitude = normalizedAmplitude,
                                timestamp = System.currentTimeMillis()
                            )
                            trySend(reading)
                        } else if (read == 0) {
                            // Send zero amplitude if no data read (silence)
                            val reading = MicrophoneReading(
                                amplitude = 0f,
                                timestamp = System.currentTimeMillis()
                            )
                            trySend(reading)
                        }
                        // Small delay to prevent overwhelming the system, but read frequently
                        delay(50) // Read every 50ms for responsive updates
                    }
                } catch (e: SecurityException) {
                    // Permission was revoked or denied
                    close()
                }
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            // Handle any other exceptions
            close()
        }

        awaitClose {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    /**
     * Get the last 5 seconds of audio recording as a ByteArray
     * Returns null if no audio data is available
     */
    fun getLast5SecondsAudio(): ByteArray? {
        synchronized(bufferLock) {
            return if (bufferFilled || bufferWriteIndex > 0) {
                if (bufferFilled) {
                    // Buffer has wrapped around, get most recent 5 seconds in chronological order
                    val result = ByteArray(audioBufferSize)
                    System.arraycopy(audioBuffer, bufferWriteIndex, result, 0, audioBufferSize - bufferWriteIndex)
                    System.arraycopy(audioBuffer, 0, result, audioBufferSize - bufferWriteIndex, bufferWriteIndex)
                    result
                } else {
                    // Buffer hasn't wrapped, just get what we have
                    audioBuffer.copyOf(bufferWriteIndex)
                }
            } else {
                null
            }
        }
    }

    /**
     * Clear the audio buffer
     */
    fun clearBuffer() {
        synchronized(bufferLock) {
            bufferWriteIndex = 0
            bufferFilled = false
        }
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
