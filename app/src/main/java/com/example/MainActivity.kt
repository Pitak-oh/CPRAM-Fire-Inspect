package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.FireInspectDatabase
import com.example.data.FireInspectRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FireInspectViewModel
import com.example.ui.viewmodel.FireInspectViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Local Room Persistence & Repository Pattern
        val database = FireInspectDatabase.getDatabase(this)
        val repository = FireInspectRepository(database.dao())

        setContent {
            MyApplicationTheme {
                // 2. MVVM State Controller Scope
                val viewModel: FireInspectViewModel = viewModel(
                    factory = FireInspectViewModelFactory(repository)
                )

                // 3. Jetpack Navigation host routing hierarchy
                FireInspectNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FireInspectNavigation(
    viewModel: FireInspectViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier.fillMaxSize()
    ) {
        // A. Public Portal Gateway (Sign In + 2FA Verify Checks)
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { role ->
                    if (role == "ADMIN") {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("inspector_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // B. Admin Dashboard (Stats, Interactive Canvas Maps & Reporting)
        composable("admin_dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAddExtinguisher = {
                    navController.navigate("admin_add_extinguisher")
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // C. Admin Registrar form (Sticker Maker & Coordinates Positioner Grid)
        composable("admin_add_extinguisher") {
            AdminAddExtinguisherScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // D. Field Inspector Desk (Daily Target Percent Ring, Checklist Workloads)
        composable("inspector_home") {
            InspectorHomeScreen(
                viewModel = viewModel,
                onNavigateToScan = {
                    navController.navigate("camera_scan")
                },
                onNavigateToForm = { extId ->
                    navController.navigate("inspect_form/$extId")
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("inspector_home") { inclusive = true }
                    }
                }
            )
        }

        // E. Camera viewfinder QR code scanner (CameraX hardware preview + bypass selectors)
        composable("camera_scan") {
            CameraScannerScreen(
                viewModel = viewModel,
                onNavigateToInspect = { extId ->
                    navController.navigate("inspect_form/$extId") {
                        popUpTo("camera_scan") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // F. 5-Point ISO Safety Audit Form screen (Real camera snapshots & automatic LINE alarms)
        composable(
            route = "inspect_form/{extId}",
            arguments = listOf(navArgument("extId") { type = NavType.StringType })
        ) { backStackEntry ->
            val extId = backStackEntry.arguments?.getString("extId") ?: ""
            InspectionFormScreen(
                extId = extId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
