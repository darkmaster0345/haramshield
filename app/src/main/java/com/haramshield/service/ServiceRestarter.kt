package com.haramshield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Receiver to restart services if they are killed
 */
class ServiceRestarter : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MonitoringAccessibilityService.ACTION_RESTART_SERVICE) {
            Timber.d("Received restart service broadcast")
            
            // Schedule the KeepAlive worker
            KeepAliveWorker.schedule(context)
        }
    }
}
