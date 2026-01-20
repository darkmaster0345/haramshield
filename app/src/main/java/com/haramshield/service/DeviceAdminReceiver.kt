package com.haramshield.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Device admin receiver for force-lock functionality
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.d("Device admin enabled")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.d("Device admin disabled")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin will reduce HaramShield's effectiveness. Are you sure?"
    }
}
