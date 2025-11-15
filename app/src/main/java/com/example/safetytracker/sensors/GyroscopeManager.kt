package com.example.safetytracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

data class GyroscopeReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val timestamp: Long
)

class GyroscopeManager(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var sensorEventListener: SensorEventListener? = null

    fun getGyroscopeData(): Flow<GyroscopeReading> = callbackFlow {
        if (gyroscope == null) {
            close()
            return@callbackFlow
        }

        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    
                    val reading = GyroscopeReading(
                        x = x,
                        y = y,
                        z = z,
                        magnitude = magnitude,
                        timestamp = System.currentTimeMillis()
                    )
                    trySend(reading)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Not needed for this implementation
            }
        }

        // Try fastest first, fall back to normal if permission not granted
        val samplingRate = try {
            sensorManager.registerListener(
                sensorEventListener,
                gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            SensorManager.SENSOR_DELAY_FASTEST
        } catch (e: SecurityException) {
            // Fall back to normal rate if HIGH_SAMPLING_RATE_SENSORS permission not granted
            sensorManager.registerListener(
                sensorEventListener,
                gyroscope,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            SensorManager.SENSOR_DELAY_NORMAL
        }

        awaitClose {
            sensorEventListener?.let {
                sensorManager.unregisterListener(it)
                sensorEventListener = null
            }
        }
    }

    fun stop() {
        sensorEventListener?.let {
            sensorManager.unregisterListener(it)
            sensorEventListener = null
        }
    }
}
