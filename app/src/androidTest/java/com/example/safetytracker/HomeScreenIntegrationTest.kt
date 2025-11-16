package com.example.safetytracker

import androidx.compose.ui.test.junit4.createComposeRule
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
	val composeTestRule = createComposeRule()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.safetytracker", appContext.packageName)
    }

    @Test
    fun homeScreen_displaysMonitoringControls() {
        composeTestRule.setContent {
            SafetyTrackerTheme {
				HomeScreen(enableEmergencyMonitoring = false)
            }
        }

        composeTestRule.onNodeWithText("Safety monitoring")
            .assertExists()

        composeTestRule.onNodeWithText("Use the play button to begin monitoring sensors. Press pause to stop.")
            .assertExists()
    }

    @Test
	fun homeScreen_smokeTest_rendersStaticTexts() {
		composeTestRule.setContent {
			SafetyTrackerTheme {
				HomeScreen(enableEmergencyMonitoring = false)
			}
		}

		composeTestRule.onNodeWithText("Safety monitoring")
			.assertExists()

		composeTestRule.onNodeWithText("Press play to start monitoring")
			.assertExists()
	}
}


