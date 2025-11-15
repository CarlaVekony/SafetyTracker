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
        `when`(sensorManager.registerListener(any(), any(), anyInt())).thenReturn(true)

        gyroscopeManager = GyroscopeManager(context)
    }

    @Test
    fun `getGyroscopeData returns flow with correct readings`() = runTest {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        val flow = gyroscopeManager.getGyroscopeData()
        
        verify(sensorManager).registerListener(
            listenerCaptor.capture(),
            eq(gyroscope),
            anyInt()
        )
        
        val event = mock(SensorEvent::class.java)
        `when`(event.sensor).thenReturn(gyroscope)
        `when`(event.sensor.type).thenReturn(Sensor.TYPE_GYROSCOPE)
        `when`(event.values).thenReturn(floatArrayOf(0.1f, 0.2f, 0.3f))
        
        listenerCaptor.firstValue.onSensorChanged(event)
        
        val reading = flow.first()
        
        assertNotNull(reading)
        assertEquals(0.1f, reading.x)
        assertEquals(0.2f, reading.y)
        assertEquals(0.3f, reading.z)
        assertNotNull(reading.magnitude)
        assertNotNull(reading.timestamp)
    }

    @Test
    fun `getGyroscopeData closes flow when sensor is null`() = runTest {
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)).thenReturn(null)

        val flow = gyroscopeManager.getGyroscopeData()
        
        val result = runCatching { flow.first() }
        assert(result.isFailure)
    }

    @Test
    fun `stop unregisters listener`() {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        `when`(sensorManager.registerListener(any(), any(), anyInt())).thenReturn(true)

        gyroscopeManager.getGyroscopeData()
        gyroscopeManager.stop()

        verify(sensorManager, atLeastOnce()).unregisterListener(listenerCaptor.capture())
    }

    @Test
    fun `magnitude calculation is correct`() = runTest {
        val listenerCaptor = argumentCaptor<SensorEventListener>()
        val flow = gyroscopeManager.getGyroscopeData()
        
        val event = mock(SensorEvent::class.java)
        `when`(event.sensor).thenReturn(gyroscope)
        `when`(event.sensor.type).thenReturn(Sensor.TYPE_GYROSCOPE)
        `when`(event.values).thenReturn(floatArrayOf(1.0f, 1.0f, 1.0f))
        
        listenerCaptor.firstValue.onSensorChanged(event)
        val reading = flow.first()
        
        // Magnitude should be sqrt(1^2 + 1^2 + 1^2) = sqrt(3)
        assertEquals(sqrt(3.0f), reading.magnitude, 0.01f)
    }
}
