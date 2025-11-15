package com.example.safetytracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.any
import kotlin.math.sqrt

class GyroscopeManagerTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sensorManager: SensorManager

    @Mock
    private lateinit var gyroscope: Sensor

    private lateinit var gyroscopeManager: GyroscopeManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)).thenReturn(gyroscope)
        `when`(sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), anyInt())).thenReturn(true)

        gyroscopeManager = GyroscopeManager(context)
    }

    @Test
    fun `getGyroscopeData closes flow when sensor is null`() = runTest {
        // Create a new manager with null sensor
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)).thenReturn(null)
        val managerWithNullSensor = GyroscopeManager(context)

        val flow = managerWithNullSensor.getGyroscopeData()
        
        val result = runCatching { flow.first() }
        assert(result.isFailure)
    }

    @Test
    fun `stop unregisters listener`() = runTest {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        val flow = gyroscopeManager.getGyroscopeData()
        
        // Start collecting to register listener
        val collectJob = launch {
            flow.collect { }
        }
        
        // Wait for listener to be registered
        delay(10)
        
        // Verify listener was registered
        verify(sensorManager, atLeastOnce()).registerListener(
            listenerCaptor.capture(),
            eq(gyroscope),
            anyInt()
        )
        
        // Stop the manager
        gyroscopeManager.stop()
        
        // Verify listener was unregistered
        verify(sensorManager, atLeastOnce()).unregisterListener(listenerCaptor.firstValue)
        
        // Cancel collection to close the flow
        collectJob.cancel()
        collectJob.join()
    }

}
