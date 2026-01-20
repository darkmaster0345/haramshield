package com.haramshield.domain.usecase

import com.haramshield.data.repository.AppRepository
import com.haramshield.data.db.entity.LockedApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to check if an app is currently locked
 */
@Singleton
class CheckAppLockStatusUseCase @Inject constructor(
    private val repository: AppRepository
) {
    
    /**
     * Check if an app is locked
     * @return Pair of (isLocked, remainingTimeMs)
     */
    suspend operator fun invoke(packageName: String): LockStatus {
        val lock = repository.getActiveLock(packageName)
        
        return if (lock != null) {
            val remainingTime = lock.lockUntil - System.currentTimeMillis()
            if (remainingTime > 0) {
                LockStatus.Locked(
                    lockedApp = lock,
                    remainingTimeMs = remainingTime
                )
            } else {
                // Lock has expired, clean it up
                repository.unlockApp(packageName)
                LockStatus.Unlocked
            }
        } else {
            LockStatus.Unlocked
        }
    }
    
    /**
     * Get remaining lock time in milliseconds (0 if not locked)
     */
    suspend fun getRemainingTimeMs(packageName: String): Long {
        return when (val status = invoke(packageName)) {
            is LockStatus.Locked -> status.remainingTimeMs
            is LockStatus.Unlocked -> 0L
        }
    }
}

sealed class LockStatus {
    data class Locked(
        val lockedApp: LockedApp,
        val remainingTimeMs: Long
    ) : LockStatus()
    
    data object Unlocked : LockStatus()
}
