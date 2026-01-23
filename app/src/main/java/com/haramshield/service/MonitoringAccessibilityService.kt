package com.haramshield.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.haramshield.Constants
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.data.repository.AppRepository
import com.haramshield.domain.usecase.CheckAppLockStatusUseCase
import com.haramshield.domain.usecase.LockStatus
import com.haramshield.ui.lockout.LockoutActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringAccessibilityService : AccessibilityService() {
    
    @Inject lateinit var repository: AppRepository
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var checkAppLockStatusUseCase: CheckAppLockStatusUseCase
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentPackage: String? = null
    private var lastEventTime: Long = 0
    private val isMonitoringEnabled = AtomicBoolean(false)
    @Volatile private var whitelistedPackages = setOf<String>()
    
    // Anti-Cheat: Detect if user is trying to kill service settings
    private val SETTINGS_PACKAGE = "com.android.settings"
    private val APP_NAME_KEYWORDS = listOf("haramshield", "haram shield")
    
    private val controlReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PERFORM_GLOBAL_HOME -> {
                    Timber.w("Received request to perform GLOBAL_ACTION_HOME")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
                Constants.ACTION_SNOOZE -> {
                    Timber.w("SNOOZE: Pausing protection for 15 seconds")
                    snoozeShield(15000L)
                }
                Constants.ACTION_DISABLE_SERVICE -> {
                    Timber.w("DISABLE: Deactivating Accessibility Service")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        disableSelf()
                    } else {
                        Timber.e("disableSelf() not supported on this Android version")
                    }
                }
            }
        }
    }
    
    // Snooze State
    private var isSnoozed = false
    private val snoozeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    
    private fun snoozeShield(durationMs: Long) {
        if (isSnoozed) return
        
        isSnoozed = true
        snoozeHandler.postDelayed({
            isSnoozed = false
            Timber.d("SNOOZE EXPIRED: Protection active")
        }, durationMs)
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("MonitoringAccessibilityService created")
        
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PERFORM_GLOBAL_HOME)
            addAction(Constants.ACTION_SNOOZE)
            addAction(Constants.ACTION_DISABLE_SERVICE)
        }
        
        ContextCompat.registerReceiver(
            this,
            controlReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        serviceScope.launch {
            settingsManager.monitoringEnabled.collect { enabled ->
                isMonitoringEnabled.set(enabled)
                Timber.d("Monitoring enabled: $enabled")
            }
        }
        
        // Observe whitelist
        serviceScope.launch {
            repository.getWhitelistedPackageNamesFlow().collect { packages ->
                whitelistedPackages = packages.toSet()
                Timber.d("Whitelist updated: ${packages.size} apps")
            }
        }
    }
    
    // Foreground Hardening: Allow Accessibility Service to be started properly
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("MonitoringAccessibilityService started via command")
        // We can promote the paired service here, but AccessibilityService itself is system managed.
        // However, we can ensure the persistent notification logic is triggered via the companion service.
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("MonitoringAccessibilityService connected")
        
        // Critical: Ensure we can retrieve window content
        val info = serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        
        checkBatteryOptimizations()
    }

    private fun checkBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Timber.w("Battery optimizations NOT ignored. Requesting exemption.")
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to launch battery optimization intent")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(controlReceiver)
        
        // Anti-Regret: Ensure we don't just die silently
        Timber.d("MonitoringAccessibilityService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isMonitoringEnabled.get()) return
        
        // SNOOZE CHECK: If snoozed, bypass all logic
        if (isSnoozed) return
        
        // Anti-Cheat: Monitor for "App Info" page of THIS app
        if (event.packageName?.toString() == SETTINGS_PACKAGE) {
             val text = event.text.joinToString(" ").lowercase()
             val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""
             
             if (APP_NAME_KEYWORDS.any { text.contains(it) || contentDesc.contains(it) }) {
                 Timber.w("ANTI-CHEAT: User attempting to open App Info for HaramShield. LOCKING.")
                 
                 // Auto-Hardening: Increment tamper count
                 serviceScope.launch {
                     settingsManager.incrementTamperAttemptCount()
                 }
                 
                 performGlobalAction(GLOBAL_ACTION_HOME)
                 showLockoutScreen(SETTINGS_PACKAGE, 5 * 60 * 1000L, "Tampering Attempt") // 5 min penalty
                 return
             }
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                
                // Ignore our own package
                if (packageName == this.packageName) return
                
                // Ignore system UI
                if (isSystemPackage(packageName)) return
                
                handleAppChanged(packageName)
            }
        }
    }

    private fun handleAppChanged(packageName: String) {
        if (packageName == currentPackage) return
        
        currentPackage = packageName
        Timber.d("App changed to: $packageName")
        
        // Check if whitelisted
        if (whitelistedPackages.contains(packageName)) {
            Timber.d("App is whitelisted, skipping")
            return
        }
        
        // Check if app is locked
        serviceScope.launch {
            when (val status = checkAppLockStatusUseCase(packageName)) {
                is LockStatus.Locked -> {
                    Timber.d("App is locked, showing lockout screen")
                    showLockoutScreen(packageName, status.remainingTimeMs, status.lockedApp.category)
                }
                is LockStatus.Unlocked -> {
                    // Start screen capture for this app
                    notifyScreenCapture(packageName)
                }
            }
        }
    }
    
    private fun showLockoutScreen(packageName: String, remainingTimeMs: Long, category: String) {
        val intent = Intent(this, LockoutActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_REMAINING_TIME, remainingTimeMs)
            putExtra(Constants.EXTRA_CATEGORY, category)
        }
        startActivity(intent)
    }
    
    private fun notifyScreenCapture(packageName: String) {
        // Send broadcast to screen capture service
        val intent = Intent(ACTION_START_CAPTURE).apply {
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            setPackage(this@MonitoringAccessibilityService.packageName)
        }
        sendBroadcast(intent)
    }
    
    private fun isSystemPackage(packageName: String): Boolean {
        return packageName.startsWith("com.android") ||
               packageName.startsWith("android") ||
               packageName == "com.google.android.launcher" ||
               packageName == "com.sec.android.app.launcher" ||
               packageName.contains("launcher") ||
               packageName.contains("systemui")
    }
    
    override fun onInterrupt() {
        Timber.d("MonitoringAccessibilityService interrupted")
    }

    companion object {
        const val ACTION_START_CAPTURE = "com.haramshield.START_CAPTURE"
        const val ACTION_RESTART_SERVICE = "com.haramshield.RESTART_SERVICE"
        const val ACTION_PERFORM_GLOBAL_HOME = "com.haramshield.PERFORM_GLOBAL_HOME"
    }
}
