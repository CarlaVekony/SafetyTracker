package com.example.safetytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.safetytracker.navigation.SafetyTrackerBottomNavigation
import com.example.safetytracker.navigation.SafetyTrackerNavHost
import com.example.safetytracker.ui.screens.HomeScreen
import com.example.safetytracker.ui.theme.SafetyTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafetyTrackerApp()
        }
    }
}

@Composable
fun SafetyTrackerApp() {
    SafetyTrackerTheme {
        val navController = rememberNavController()
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                SafetyTrackerBottomNavigation(navController = navController)
            }
        ) { innerPadding ->
            SafetyTrackerNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyTrackerPreview() {
    SafetyTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen()
        }
    }
}
