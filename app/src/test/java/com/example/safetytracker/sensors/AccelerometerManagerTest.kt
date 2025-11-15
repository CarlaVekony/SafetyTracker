package com.example.safetytracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.first
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

class AccelerometerManagerTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sensorManager: SensorManager

    @Mock
    private lateinit var accelerometer: Sensor

    private lateinit var accelerometerManager: AccelerometerManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(accelerometer)
        `when`(sensorManager.registerListener(any(), any(), anyInt())).thenReturn(true)

        accelerometerManager = AccelerometerManager(context)
    }

    @Test
    fun `getAccelerometerData returns flow with correct readings`() = runTest {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        val flow = accelerometerManager.getAccelerometerData()
        
        // Verify listener was registered
        verify(sensorManager).registerListener(
            listenerCaptor.capture(),
            eq(accelerometer),
            anyInt()
        )
        
        // Simulate sensor event
        val event = mock(SensorEvent::class.java)
        `when`(event.sensor).thenReturn(accelerometer)
        `when`(event.sensor.type).thenReturn(Sensor.TYPE_ACCELEROMETER)
        `when`(event.values).thenReturn(floatArrayOf(1.0f, 2.0f, 3.0f))
        
        // Trigger sensor event
        listenerCaptor.firstValue.onSensorChanged(event)
        
        // Get first reading
        val reading = flow.first()
        
        assertNotNull(reading)
        assertEquals(1.0f, reading.x)
        assertEquals(2.0f, reading.y)
        assertEquals(3.0f, reading.z)
        assertEquals(sqrt(14.0f), reading.magnitude, 0.01f)
        assertNotNull(reading.timestamp)
    }

    @Test
    fun `getAccelerometerData closes flow when sensor is null`() = runTest {
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(null)

        val flow = accelerometerManager.getAccelerometerData()
        
        // Flow should be closed immediately
        val result = runCatching { flow.first() }
        assert(result.isFailure)
    }

    @Test
    fun `stop unregisters listener`() {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        `when`(sensorManager.registerListener(any(), any(), anyInt())).thenReturn(true)

        accelerometerManager.getAccelerometerData()
        accelerometerManager.stop()

        verify(sensorManager, atLeastOnce()).unregisterListener(listenerCaptor.capture())
    }

    @Test
    fun `magnitude calculation is correct`() = runTest {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        val flow = accelerometerManager.getAccelerometerData()
        
        val event = mock(SensorEvent::class.java)
        `when`(event.sensor).thenReturn(accelerometer)
        `when`(event.sensor.type).thenReturn(Sensor.TYPE_ACCELEROMETER)
        `when`(event.values).thenReturn(floatArrayOf(3.0f, 4.0f, 0.0f))
        
        listenerCaptor.firstValue.onSensorChanged(event)
        val reading = flow.first()
        
        // Magnitude should be sqrt(3^2 + 4^2 + 0^2) = 5.0
        assertEquals(5.0f, reading.magnitude, 0.01f)
    }
}
