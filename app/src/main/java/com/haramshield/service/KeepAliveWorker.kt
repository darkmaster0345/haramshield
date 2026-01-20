package com.haramshield.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.haramshield.Constants
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.data.repository.AppRepository
import com.haramshield.util.PermissionHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic worker to ensure services are running and perform maintenance
 */
@HiltWorker
class KeepAliveWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsManager: SettingsManager,
    private val repository: AppRepository,
    private val permissionHelper: PermissionHelper
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        Timber.d("KeepAliveWorker running")
        
        try {
            // Check if monitoring is enabled
            val isMonitoringEnabled = settingsManager.monitoringEnabled.first()
            
            if (!isMonitoringEnabled) {
                Timber.d("Monitoring disabled, skipping keep-alive")
                return Result.success()
            }
            
            // Check and log permission status
            checkPermissions()
            
            // Cleanup expired locks
            repository.cleanupExpiredLocks()
            Timber.d("Cleaned up expired locks")
            
            // Cleanup old violation logs (older than retention period)
            val retentionMs = Constants.VIOLATION_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L
            val cutoffTime = System.currentTimeMillis() - retentionMs
            val deletedCount = repository.deleteOldViolations(cutoffTime)
            if (deletedCount > 0) {
                Timber.d("Deleted $deletedCount old violation logs")
            }
            
            return Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "KeepAliveWorker failed")
            return Result.retry()
        }
    }
    
    private fun checkPermissions() {
        val accessibilityEnabled = permissionHelper.isAccessibilityServiceEnabled()
        val overlayEnabled = permissionHelper.canDrawOverlays()
        val notificationsEnabled = permissionHelper.areNotificationsEnabled()
        
        Timber.d("Permission status - Accessibility: $accessibilityEnabled, Overlay: $overlayEnabled, Notifications: $notificationsEnabled")
        
        // Could trigger notification to user if permissions are lost
        if (!accessibilityEnabled) {
            Timber.w("Accessibility service is disabled!")
        }
    }
    
    companion object {
        private const val WORK_NAME = "keep_alive_worker"
        
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(
                Constants.KEEP_ALIVE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .addTag(Constants.WORK_TAG_KEEP_ALIVE)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            
            Timber.d("KeepAliveWorker scheduled")
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("KeepAliveWorker cancelled")
        }
    }
}
