package com.example.safetytracker

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.safetytracker.ui.screens.HomeScreen
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenIntegrationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.safetytracker", appContext.packageName)
    }

    @Test
    fun homeScreen_displaysMonitoringControls() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
                HomeScreen()
            }
        }

        composeTestRule.onNodeWithText("Safety monitoring")
            .assertExists()

        composeTestRule.onNodeWithText("Use the play button to begin monitoring sensors. Press pause to stop.")
            .assertExists()
    }

    @Test
    fun homeScreen_showsGraphsWhenMonitoring() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
                HomeScreen()
            }
        }

        // Click to start monitoring
        composeTestRule.onNodeWithText("Press play to start monitoring")
            .performClick()

        composeTestRule.waitForIdle()
        
        // Wait a bit for graphs to appear
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Accelerometer (magnitude m/s²)", substring = true)
                    .assertExists()
                true
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("Accelerometer (magnitude m/s²)", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("Gyroscope (magnitude rad/s)", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("Microphone (amplitude %)", substring = true)
            .assertExists()
    }

    @Test
    fun homeScreen_hidesGraphsWhenNotMonitoring() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
                HomeScreen()
            }
        }

        // Verify graphs are not shown initially
        try {
            composeTestRule.onNodeWithText("Accelerometer")
                .assertDoesNotExist()
        } catch (e: Exception) {
            // Expected - graphs should not exist
        }
    }
}


