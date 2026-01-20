package com.haramshield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Receiver to start services on device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.d("Boot completed, scheduling service restart")
            
            // Schedule the KeepAlive worker to ensure services start
            KeepAliveWorker.schedule(context)
        }
    }
}
