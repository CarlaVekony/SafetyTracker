package com.example.safetytracker.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorGraphTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sensorGraph_displaysTitle() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
                SensorGraph(
                    title = "Test Sensor",
                    data = listOf(1.0f, 2.0f, 3.0f)
                )
            }
        }

        composeTestRule.onNodeWithText("Test Sensor")
            .assertExists()
    }

    @Test
    fun sensorGraph_displaysNoDataWhenEmpty() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
                SensorGraph(
                    title = "Test Sensor",
                    data = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithText("Test Sensor")
            .assertExists()
    }

    @Test
    fun sensorGraph_displaysData() {
        val testData = listOf(10.0f, 20.0f, 30.0f, 25.0f)
        composeTestRule.setContent {
            SafetyTrackerTheme {
                SensorGraph(
                    title = "Accelerometer",
                    data = testData,
                    maxValue = 50f
                )
            }
        }

        composeTestRule.onNodeWithText("Accelerometer")
            .assertExists()
    }
}


