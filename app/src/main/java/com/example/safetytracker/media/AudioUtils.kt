package com.example.safetytracker.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioUtils {
	/**
	 * Write 16-bit PCM mono 44.1kHz data into a WAV file in app cache and return a content Uri.
	 * Overwrites any existing temp file for simplicity.
	 */
	fun writePcmToWavAndGetUri(
		context: Context,
		pcmData: ByteArray,
		sampleRateHz: Int = 44100,
		channels: Int = 1,
		bitsPerSample: Int = 16,
		fileName: String = "emergency_last5s.wav"
	): Uri? {
		if (pcmData.isEmpty()) return null

		return try {
			// Ensure subdir aligns with provider path
			val audioDir = File(context.cacheDir, "audio")
			if (!audioDir.exists()) {
				audioDir.mkdirs()
			}
			val wavFile = File(audioDir, fileName)

			FileOutputStream(wavFile).use { fos ->
				// Write WAV header
				val byteRate = sampleRateHz * channels * (bitsPerSample / 8)
				val blockAlign = channels * (bitsPerSample / 8)
				val dataSize = pcmData.size
				val chunkSize = 36 + dataSize

				// RIFF header
				fos.write("RIFF".toByteArray(Charsets.US_ASCII))
				fos.write(intToLittleEndian(chunkSize))
				fos.write("WAVE".toByteArray(Charsets.US_ASCII))

				// fmt subchunk
				fos.write("fmt ".toByteArray(Charsets.US_ASCII))
				fos.write(intToLittleEndian(16)) // Subchunk1Size for PCM
				fos.write(shortToLittleEndian(1)) // AudioFormat PCM = 1
				fos.write(shortToLittleEndian(channels.toShort()))
				fos.write(intToLittleEndian(sampleRateHz))
				fos.write(intToLittleEndian(byteRate))
				fos.write(shortToLittleEndian(blockAlign.toShort()))
				fos.write(shortToLittleEndian(bitsPerSample.toShort()))

				// data subchunk
				fos.write("data".toByteArray(Charsets.US_ASCII))
				fos.write(intToLittleEndian(dataSize))
				// Audio data
				fos.write(pcmData)
				fos.flush()
			}

			// Return content Uri through FileProvider
			FileProvider.getUriForFile(
				context,
				"${context.packageName}.fileprovider",
				wavFile
			)
		} catch (_: Exception) {
			null
		}
	}

	/**
	 * Convert PCM 16-bit mono 44.1kHz to WAV bytes entirely in memory.
	 */
	fun pcmToWavBytes(
		pcmData: ByteArray,
		sampleRateHz: Int = 44100,
		channels: Int = 1,
		bitsPerSample: Int = 16
	): ByteArray? {
		if (pcmData.isEmpty()) return null
		return try {
			val byteRate = sampleRateHz * channels * (bitsPerSample / 8)
			val blockAlign = channels * (bitsPerSample / 8)
			val dataSize = pcmData.size
			val chunkSize = 36 + dataSize

			val header = ByteArray(44)
			var idx = 0
			fun putAscii(s: String) { s.toByteArray(Charsets.US_ASCII).copyInto(header, idx); idx += s.length }
			fun putIntLE(v: Int) { val b = intToLittleEndian(v); b.copyInto(header, idx); idx += 4 }
			fun putShortLE(v: Short) { val b = shortToLittleEndian(v); b.copyInto(header, idx); idx += 2 }

			putAscii("RIFF"); putIntLE(chunkSize); putAscii("WAVE")
			putAscii("fmt "); putIntLE(16); putShortLE(1) // PCM
			putShortLE(channels.toShort()); putIntLE(sampleRateHz); putIntLE(byteRate)
			putShortLE(blockAlign.toShort()); putShortLE(bitsPerSample.toShort())
			putAscii("data"); putIntLE(dataSize)

			ByteArray(header.size + pcmData.size).also { out ->
				header.copyInto(out, 0)
				pcmData.copyInto(out, header.size)
			}
		} catch (_: Exception) {
			null
		}
	}

	private fun intToLittleEndian(value: Int): ByteArray {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
	}

	private fun shortToLittleEndian(value: Short): ByteArray {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
	}
}


