package com.haramshield.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.ui.navigation.NavGraph
import com.haramshield.ui.navigation.Screen
import com.haramshield.ui.theme.HaramShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var securityUtils: com.haramshield.util.SecurityUtils
    @Inject lateinit var settingsManager: SettingsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 1. Signature Verify (Audit)
        // In a real release, you would finish() if this returns false.
        // For now, we just log it so you can see the hash.
        securityUtils.verifyAppSignature(this)
        
        // 2. Battery Optimization Check
        checkBatteryOptimization()
        
        // Check onboarding state synchronously for start destination
        val onboardingCompleted = runBlocking {
            settingsManager.onboardingCompleted.first()
        }
        
        setContent {
            HaramShieldTheme {
                val navController = rememberNavController()
                
                val startDestination = if (onboardingCompleted) {
                    Screen.Dashboard.route
                } else {
                    Screen.Onboarding.route
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    private fun checkBatteryOptimization() {
        // 1. Battery Optimization
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 2. Overlay Permission (Draw Over Other Apps)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
