package com.haramshield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.haramshield.ui.about.AboutScreen
import com.haramshield.ui.dashboard.DashboardScreen
import com.haramshield.ui.history.ViolationHistoryScreen
import com.haramshield.ui.onboarding.OnboardingScreen
import com.haramshield.ui.settings.PinSetupScreen
import com.haramshield.ui.settings.SettingsScreen
import com.haramshield.ui.whitelist.AppWhitelistScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToWhitelist = {
                    navController.navigate(Screen.Whitelist.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }
        
        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPinSetup = { navController.navigate(Screen.PinSetup.route) },
                onNavigateToWhitelist = { navController.navigate(Screen.Whitelist.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }
        
        // App Whitelist
        composable(Screen.Whitelist.route) {
            AppWhitelistScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // Violation History
        composable(Screen.History.route) {
            ViolationHistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // PIN Setup
        composable(Screen.PinSetup.route) {
            PinSetupScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // About
        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // App Detail with argument
        composable(
            route = Screen.AppDetail.route,
            arguments = listOf(
                navArgument(NavArgs.PACKAGE_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString(NavArgs.PACKAGE_NAME) ?: ""
            // Placeholder: AppDetailScreen(packageName)
        }
    }
}
