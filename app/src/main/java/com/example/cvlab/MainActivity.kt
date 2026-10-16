package com.example.cvlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cvlab.navigation.Screen
import com.example.cvlab.theme.*
import com.example.cvlab.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge() must be called BEFORE setContent{} so that the
        // window is configured for edge-to-edge before Compose lays out.
        // Required for activity-compose:1.10.0 + targetSdk=35 on Android 14.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CvLabTheme(darkTheme = true) {
                CvLabApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CvLabApp() {
    val navController       = rememberNavController()
    val drawerState         = rememberDrawerState(DrawerValue.Closed)
    val scope               = rememberCoroutineScope()
    val currentBackStack    by navController.currentBackStackEntryAsState()
    val currentRoute        = currentBackStack?.destination?.route ?: Screen.Dashboard.route

    fun navigate(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            DrawerContent(currentRoute = currentRoute, onNavigate = ::navigate)
        },
    ) {
        Scaffold(
            topBar = {
                CvLabTopBar(
                    title          = Screen.drawerItems.filterNotNull().find { it.route == currentRoute }?.label ?: "CV Lab",
                    onMenuClick    = { scope.launch { drawerState.open() } },
                )
            },
            bottomBar = {
                CvLabBottomBar(currentRoute = currentRoute, onNavigate = ::navigate)
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            NavHost(
                navController     = navController,
                startDestination  = Screen.Dashboard.route,
                modifier          = Modifier.padding(innerPadding),
                enterTransition   = { fadeIn(tween(200)) },
                exitTransition    = { fadeOut(tween(150)) },
            ) {
                composable(Screen.Dashboard.route)   { DashboardScreen(onNavigate = ::navigate) }
                composable(Screen.Simulator.route)   { SimulatorScreen() }
                composable(Screen.VisionLab.route)   { VisionLabScreen() }
                composable(Screen.TrackingLab.route) { TrackingScreen() }
                composable(Screen.Analytics.route)   { AnalyticsScreen() }
                composable(Screen.Settings.route)    { SettingsScreen() }
                composable(Screen.Docs.route)        { DocsScreen() }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CvLabTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title        = {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = MaterialTheme.colorScheme.onBackground,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Open drawer", tint = Mint)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.surface,
            titleContentColor      = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = Mint,
        ),
    )
}

// ---------------------------------------------------------------------------
// Bottom navigation bar (5 primary destinations)
// ---------------------------------------------------------------------------

@Composable
private fun CvLabBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Screen.bottomNavItems.filterNotNull().forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected  = selected,
                onClick   = { onNavigate(screen.route) },
                icon      = { Icon(screen.icon, contentDescription = screen.label) },
                label     = { Text(screen.label, fontSize = 10.sp) },
                colors    = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Mint,
                    selectedTextColor   = Mint,
                    indicatorColor      = Mint.copy(alpha = 0.15f),
                    unselectedIconColor = Color(0xFF6677AA),
                    unselectedTextColor = Color(0xFF6677AA),
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Navigation drawer
// ---------------------------------------------------------------------------

@Composable
private fun DrawerContent(currentRoute: String, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = InkSurface,
        modifier             = Modifier.width(260.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("CV AIM ANALYSIS LAB", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Navigation", color = Color(0xFF8899AA), fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(8.dp))

        Screen.drawerItems.filterNotNull().forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationDrawerItem(
                icon     = { Icon(screen.icon, contentDescription = screen.label) },
                label    = { Text(screen.label) },
                selected = selected,
                onClick  = { onNavigate(screen.route) },
                colors   = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Mint.copy(alpha = 0.15f),
                    selectedIconColor      = Mint,
                    selectedTextColor      = Mint,
                    unselectedIconColor    = Color(0xFF6677AA),
                    unselectedTextColor    = Color(0xFF8899AA),
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}
