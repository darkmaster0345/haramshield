package com.haramshield.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.service.ScreenCaptureService
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

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                startScreenCaptureService(result.resultCode, it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timber.log.Timber.d("App Started")
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

        if (onboardingCompleted) {
            requestScreenCapturePermission()
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

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_INIT_PROJECTION
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        startService(intent)
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
                    timber.log.Timber.e(e, "Failed to request battery optimization")
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
                    timber.log.Timber.e(e, "Failed to request overlay permission")
                }
            }
        }
    }
}
