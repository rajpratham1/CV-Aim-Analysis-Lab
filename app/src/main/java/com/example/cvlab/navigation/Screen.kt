package com.example.cvlab.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation destinations. */
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Dashboard   : Screen("dashboard",  "Dashboard",   Icons.Default.Dashboard)
    data object Simulator   : Screen("simulator",  "Simulator",   Icons.Default.GpsFixed)
    data object VisionLab   : Screen("visionlab",  "CV Lab",      Icons.Default.Science)
    data object TrackingLab : Screen("tracking",   "Tracking",    Icons.Default.TrackChanges)
    data object Analytics   : Screen("analytics",  "Analytics",   Icons.Default.Analytics)
    data object Settings    : Screen("settings",   "Settings",    Icons.Default.Settings)
    data object Docs        : Screen("docs",       "Docs",        Icons.Default.Description)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOfNotNull(Dashboard, Simulator, VisionLab, Analytics, Settings)

        val drawerItems: List<Screen>
            get() = listOfNotNull(Dashboard, Simulator, VisionLab, TrackingLab, Analytics, Settings, Docs)
    }
}

