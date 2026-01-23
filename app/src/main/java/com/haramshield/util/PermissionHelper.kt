package com.haramshield.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.haramshield.service.DeviceAdminReceiver
import com.haramshield.service.MonitoringAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing app permissions
 */
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // ==================== Accessibility Service ====================
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val componentName = ComponentName(context, MonitoringAccessibilityService::class.java)
        val expectedId = "${componentName.packageName}/${componentName.className}"
        
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.let { info ->
                "${info.packageName}/${info.name}" == expectedId
            }
        }
    }
    
    /**
     * Get intent to open accessibility settings
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== Overlay Permission ====================
    
    /**
     * Check if overlay permission is granted
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Get intent to request overlay permission
     */
    fun getOverlaySettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== Notification Permission ====================
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Get intent to app notification settings
     */
    fun getNotificationSettingsIntent(): Intent {
        return Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${context.packageName}")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== Device Admin ====================
    
    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Get intent to request device admin
     */
    fun getDeviceAdminIntent(): Intent {
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "HaramShield needs device admin permission to enforce app locks."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== Usage Stats ====================
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * Get intent to usage stats settings
     */
    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== Media Projection ====================
    
    /**
     * Get intent to request media projection (screen capture) permission.
     * This must be launched with startActivityForResult to get the permission token.
     */
    fun getMediaProjectionIntent(): Intent {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        return projectionManager.createScreenCaptureIntent()
    }

    // ==================== Battery Optimization ====================

    /**
     * Check if battery optimization is ignored (exemption granted)
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Get intent to request exemption from battery optimization
     */
    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ==================== All Permissions Check ====================
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean {
        return isAccessibilityServiceEnabled() &&
               canDrawOverlays() &&
               areNotificationsEnabled()
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!isAccessibilityServiceEnabled()) {
            missing.add("Accessibility Service")
        }
        if (!canDrawOverlays()) {
            missing.add("Overlay Permission")
        }
        if (!areNotificationsEnabled()) {
            missing.add("Notifications")
        }
        
        return missing
    }
}
