package com.example.safetytracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.safetytracker.ui.screens.ContactsScreenWithData
import com.example.safetytracker.ui.screens.HomeScreen
import com.example.safetytracker.ui.screens.SettingsScreen
import com.example.safetytracker.ui.screens.AboutScreen
import com.example.safetytracker.ui.theme.SafetyTrackerTheme

// Navigation destinations
sealed class SafetyTrackerDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : SafetyTrackerDestination(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    object Contacts : SafetyTrackerDestination(
        route = "contacts",
        title = "Contacts",
        icon = Icons.Default.Person
    )
    
    object Settings : SafetyTrackerDestination(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
}

@Composable
fun SafetyTrackerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = SafetyTrackerDestination.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(SafetyTrackerDestination.Home.route) {
            HomeScreen()
        }
        
        composable(SafetyTrackerDestination.Contacts.route) {
            ContactsScreenWithData()
        }
        
        composable(SafetyTrackerDestination.Settings.route) {
            SettingsScreen(
                onAboutClick = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun SafetyTrackerBottomNavigation(
    navController: NavHostController,
    destinations: List<SafetyTrackerDestination> = listOf(
        SafetyTrackerDestination.Home,
        SafetyTrackerDestination.Contacts,
        SafetyTrackerDestination.Settings
    )
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = { Text(destination.title) },
                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                onClick = {
                    navController.navigate(destination.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SafetyTrackerBottomNavigationPreview() {
    SafetyTrackerTheme {
        SafetyTrackerBottomNavigation(
            navController = rememberNavController()
        )
    }
}