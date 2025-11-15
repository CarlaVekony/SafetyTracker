package com.example.safetytracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorDataStateTest {
    @Test
    fun sensorDataState_initializesWithEmptyLists() {
        val state = SensorDataState()

        assertTrue(state.accelerometerReadings.isEmpty())
        assertTrue(state.gyroscopeReadings.isEmpty())
        assertTrue(state.microphoneReadings.isEmpty())
    }

    @Test
    fun sensorDataState_storesDataCorrectly() {
        val accData = listOf(1.0f, 2.0f, 3.0f)
        val gyroData = listOf(0.1f, 0.2f, 0.3f)
        val micData = listOf(10f, 20f, 30f)

        val state = SensorDataState(
            accelerometerReadings = accData,
            gyroscopeReadings = gyroData,
            microphoneReadings = micData
        )

        assertEquals(accData, state.accelerometerReadings)
        assertEquals(gyroData, state.gyroscopeReadings)
        assertEquals(micData, state.microphoneReadings)
    }

    @Test
    fun sensorDataState_dataClassesEquality() {
        val state1 = SensorDataState(
            accelerometerReadings = listOf(1.0f),
            gyroscopeReadings = listOf(0.1f),
            microphoneReadings = listOf(10f)
        )

        val state2 = SensorDataState(
            accelerometerReadings = listOf(1.0f),
            gyroscopeReadings = listOf(0.1f),
            microphoneReadings = listOf(10f)
        )

        assertEquals(state1, state2)
    }
}


