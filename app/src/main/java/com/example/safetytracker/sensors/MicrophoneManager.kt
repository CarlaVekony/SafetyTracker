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
    @Volatile private var isRecording = false

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // 5 seconds buffer for audio (PCM16)
    private val audioBufferSize = sampleRate * 2 * 5
    private val audioBuffer = ByteArray(audioBufferSize)
    private var writeIndex = 0
    private var bufferFilled = false
    private val lock = Any()

    fun getMicrophoneData(): Flow<MicrophoneReading> = callbackFlow {

        // Permission check
        if (
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }

        if (minBuffer <= 0) {
            close()
            return@callbackFlow
        }

        // Init recorder safely
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfig).build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .build()
        } catch (e: Exception) {
            close()
            return@callbackFlow
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            close()
            return@callbackFlow
        }

        audioRecord = recorder
        isRecording = true

        try {
            recorder.startRecording()
        } catch (e: IllegalStateException) {
            recorder.release()
            close()
            return@callbackFlow
        }

        val buffer = ShortArray(minBuffer)

        // Recording loop
        while (isRecording && audioRecord != null) {

            val read = try {
                recorder.read(buffer, 0, buffer.size)
            } catch (e: IllegalStateException) {
                break
            }

            if (read > 0) {
                // amplitude calc
                var maxAmp = 0
                for (i in 0 until read) {
                    maxAmp = max(maxAmp, abs(buffer[i].toInt()))
                }
                val normalized = (maxAmp / 32768f) * 100f

                // Push into circular buffer
                synchronized(lock) {
                    for (i in 0 until read) {
                        val sample = buffer[i]
                        audioBuffer[writeIndex] = (sample.toInt() and 0xFF).toByte()
                        audioBuffer[writeIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()

                        writeIndex = (writeIndex + 2) % audioBufferSize
                        if (writeIndex == 0) bufferFilled = true
                    }
                }

                trySend(
                    MicrophoneReading(
                        amplitude = normalized,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            delay(50)
        }

        // Cleanup section
        awaitClose {
            isRecording = false

            try {
                audioRecord?.let { r ->
                    if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        r.stop()
                    }
                    r.release()
                }
            } catch (_: Exception) {}

            audioRecord = null
        }
    }

    fun getLast5SecondsAudio(): ByteArray? {
        synchronized(lock) {
            if (!bufferFilled && writeIndex == 0) return null

            return if (bufferFilled) {
                val result = ByteArray(audioBufferSize)
                System.arraycopy(audioBuffer, writeIndex, result, 0, audioBufferSize - writeIndex)
                System.arraycopy(audioBuffer, 0, result, audioBufferSize - writeIndex, writeIndex)
                result
            } else {
                audioBuffer.copyOf(writeIndex)
            }
        }
    }

    fun clearBuffer() {
        synchronized(lock) {
            writeIndex = 0
            bufferFilled = false
        }
    }

    fun stop() {
        isRecording = false

        try {
            audioRecord?.let { r ->
                if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    r.stop()
                }
                r.release()
            }
        } catch (_: Exception) {}

        audioRecord = null
    }
}
