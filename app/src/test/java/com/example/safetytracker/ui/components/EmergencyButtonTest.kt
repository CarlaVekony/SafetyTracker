package com.example.safetytracker.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmergencyButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emergencyButton_displaysPlayWhenNotMonitoring() {
        var monitoringState = false
        composeTestRule.setContent {
            EmergencyButton(
                onMonitoringStateChanged = { monitoringState = it }
            )
        }

        composeTestRule.onNodeWithText("Press play to start monitoring")
            .assertExists()
    }

    @Test
    fun emergencyButton_togglesMonitoringState() {
        var monitoringState = false
        composeTestRule.setContent {
            EmergencyButton(
                onMonitoringStateChanged = { monitoringState = it }
            )
        }

        // Click to start monitoring
        composeTestRule.onNodeWithText("Press play to start monitoring")
            .performClick()

        composeTestRule.waitForIdle()

        // Should show monitoring in progress
        composeTestRule.onNodeWithText("Monitoring in progress")
            .assertExists()

        // Click again to stop
        composeTestRule.onNodeWithText("Monitoring in progress")
            .performClick()

        composeTestRule.waitForIdle()

        // Should show paused
        composeTestRule.onNodeWithText("Monitoring paused")
            .assertExists()
    }

    @Test
    fun emergencyButton_displaysCorrectStatusText() {
        composeTestRule.setContent {
            EmergencyButton()
        }

        composeTestRule.onNodeWithText("Monitoring paused")
            .assertExists()

        composeTestRule.onNodeWithText("Press play to start monitoring")
            .assertExists()
    }
}


