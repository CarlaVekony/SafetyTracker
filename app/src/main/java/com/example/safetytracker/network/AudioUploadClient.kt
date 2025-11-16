package com.example.safetytracker.network

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.min

/**
 * Minimal client to upload audio to a public file host and return a URL.
 * Uses transfer.sh which supports anonymous uploads via HTTP PUT.
 * No external dependencies are required.
 */
class AudioUploadClient {
	companion object {
		private const val TAG = "AudioUploadClient"
		private const val TRANSFER_SH = "https://transfer.sh"
		private const val ZEROX0 = "https://0x0.st"
		private const val CONNECTION_TIMEOUT_MS = 15000
		private const val READ_TIMEOUT_MS = 20000
		private const val RETRIES = 2
	}

	/**
	 * Uploads WAV bytes and returns a public URL, or null on failure.
	 */
	fun uploadWav(wavBytes: ByteArray, fileNameHint: String = "emergency_last5s.wav"): String? {
		val safeName = if (fileNameHint.isNotBlank()) fileNameHint else "audio_${UUID.randomUUID()}.wav"

		// Try transfer.sh with small retries
		repeat(RETRIES) { attempt ->
			val url = uploadToTransferSh(wavBytes, safeName)
			if (url != null) return url
		}

		// Fallback to 0x0.st with small retries
		repeat(RETRIES) { attempt ->
			val url = uploadTo0x0(wavBytes, safeName)
			if (url != null) return url
		}

		return null
	}

	private fun uploadToTransferSh(bytes: ByteArray, fileName: String): String? {
		val endpoint = "$TRANSFER_SH/$fileName"
		var connection: HttpURLConnection? = null
		var input: InputStream? = null
		return try {
			val url = URL(endpoint)
			connection = (url.openConnection() as HttpURLConnection).apply {
				requestMethod = "PUT"
				connectTimeout = CONNECTION_TIMEOUT_MS
				readTimeout = READ_TIMEOUT_MS
				doOutput = true
				setRequestProperty("Content-Type", "audio/wav")
				setRequestProperty("Content-Length", bytes.size.toString())
			}
			BufferedOutputStream(connection.outputStream).use { out ->
				input = BufferedInputStream(ByteArrayInputStream(bytes))
				val buffer = ByteArray(16 * 1024)
				while (true) {
					val read = input!!.read(buffer)
					if (read == -1) break
					out.write(buffer, 0, read)
				}
				out.flush()
			}
			val responseCode = connection.responseCode
			val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
			val response = stream.bufferedReader().use { it.readText() }.trim()
			if (responseCode in 200..299 && response.startsWith("https://")) {
				Log.i(TAG, "Audio uploaded (transfer.sh): $response")
				response
			} else {
				Log.e(TAG, "transfer.sh failed: code=$responseCode, response=$response")
				null
			}
		} catch (e: Exception) {
			Log.e(TAG, "Upload error (transfer.sh): ${e.message}")
			null
		} finally {
			try { input?.close() } catch (_: Exception) {}
			connection?.disconnect()
		}
	}

	private fun uploadTo0x0(bytes: ByteArray, fileName: String): String? {
		// POST multipart/form-data to https://0x0.st with field name "file"
		val boundary = "----SafetyTrackerBoundary${UUID.randomUUID()}"
		val lineEnd = "\r\n"
		var connection: HttpURLConnection? = null

		return try {
			val url = URL(ZEROX0)
			connection = (url.openConnection() as HttpURLConnection).apply {
				requestMethod = "POST"
				connectTimeout = CONNECTION_TIMEOUT_MS
				readTimeout = READ_TIMEOUT_MS
				doOutput = true
				setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
			}

			connection.outputStream.use { os ->
				fun writeString(s: String) = os.write(s.toByteArray(Charsets.UTF_8))

				writeString("--$boundary$lineEnd")
				writeString("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$lineEnd")
				writeString("Content-Type: audio/wav$lineEnd$lineEnd")
				os.write(bytes)
				writeString(lineEnd)
				writeString("--$boundary--$lineEnd")
				os.flush()
			}

			val code = connection.responseCode
			val stream = if (code in 200..299) connection.inputStream else connection.errorStream
			val response = stream.bufferedReader().use { it.readText() }.trim()

			// 0x0.st responds with a plain URL text on success
			return if (code in 200..299 && (response.startsWith("http://") || response.startsWith("https://"))) {
				Log.i(TAG, "Audio uploaded (0x0.st): $response")
				response
			} else {
				Log.e(TAG, "0x0.st failed: code=$code, response=$response")
				null
			}
		} catch (e: Exception) {
			Log.e(TAG, "Upload error (0x0.st): ${e.message}")
			null
		} finally {
			connection?.disconnect()
		}
	}
}
