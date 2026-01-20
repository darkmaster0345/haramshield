package com.haramshield.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object Whitelist : Screen("app_whitelist")
    data object History : Screen("violation_history")
    data object PinSetup : Screen("pin_setup")
    data object About : Screen("about")
    
    // Routes with arguments
    data object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String) = "app_detail/$packageName"
    }
}

/**
 * Navigation argument keys
 */
object NavArgs {
    const val PACKAGE_NAME = "packageName"
}
