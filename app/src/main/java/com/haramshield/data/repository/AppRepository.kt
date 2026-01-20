package com.haramshield.data.repository

import com.haramshield.data.db.dao.LockedAppDao
import com.haramshield.data.db.dao.ViolationLogDao
import com.haramshield.data.db.dao.WhitelistDao
import com.haramshield.data.db.entity.LockedApp
import com.haramshield.data.db.entity.ViolationLog
import com.haramshield.data.db.entity.WhitelistedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val lockedAppDao: LockedAppDao,
    private val violationLogDao: ViolationLogDao,
    private val whitelistDao: WhitelistDao
) {
    // ==================== Locked Apps ====================
    
    fun getActiveLocksFlow(): Flow<List<LockedApp>> = 
        lockedAppDao.getActiveLocksFlow()
    
    suspend fun getActiveLocks(): List<LockedApp> = 
        lockedAppDao.getActiveLocks()
    
    suspend fun isAppLocked(packageName: String): Boolean = 
        lockedAppDao.isLocked(packageName)
    
    suspend fun getActiveLock(packageName: String): LockedApp? = 
        lockedAppDao.getActiveLock(packageName)
    
    suspend fun lockApp(lockedApp: LockedApp) {
        lockedAppDao.insert(lockedApp)
    }
    
    suspend fun unlockApp(packageName: String) {
        lockedAppDao.deleteByPackage(packageName)
    }
    
    suspend fun cleanupExpiredLocks() {
        lockedAppDao.deleteExpiredLocks()
    }
    
    // ==================== Violation Logs ====================
    
    fun getAllViolationsFlow(): Flow<List<ViolationLog>> = 
        violationLogDao.getAllViolationsFlow()
    
    fun getRecentViolationsFlow(limit: Int = 50): Flow<List<ViolationLog>> = 
        violationLogDao.getRecentViolationsFlow(limit)
    
    fun getTotalViolationCountFlow(): Flow<Int> = 
        violationLogDao.getTotalViolationCountFlow()
    
    suspend fun getTotalViolationCount(): Int = 
        violationLogDao.getTotalViolationCount()
    
    suspend fun getViolationsForApp(packageName: String): List<ViolationLog> = 
        violationLogDao.getViolationsForApp(packageName)
    
    suspend fun logViolation(violationLog: ViolationLog): Long = 
        violationLogDao.insert(violationLog)
    
    suspend fun deleteOldViolations(beforeTime: Long): Int = 
        violationLogDao.deleteOlderThan(beforeTime)
    
    // ==================== Whitelist ====================
    
    fun getWhitelistedAppsFlow(): Flow<List<WhitelistedApp>> = 
        whitelistDao.getAllWhitelistedAppsFlow()
    
    fun getWhitelistedPackageNamesFlow(): Flow<List<String>> = 
        whitelistDao.getWhitelistedPackageNamesFlow()
    
    suspend fun getAllWhitelistedApps(): List<WhitelistedApp> = 
        whitelistDao.getAllWhitelistedApps()
    
    suspend fun getWhitelistedPackageNames(): List<String> = 
        whitelistDao.getWhitelistedPackageNames()
    
    suspend fun isWhitelisted(packageName: String): Boolean = 
        whitelistDao.isWhitelisted(packageName)
    
    fun isWhitelistedFlow(packageName: String): Flow<Boolean> = 
        whitelistDao.isWhitelistedFlow(packageName)
    
    suspend fun addToWhitelist(app: WhitelistedApp) {
        whitelistDao.insert(app)
    }
    
    suspend fun removeFromWhitelist(packageName: String) {
        whitelistDao.deleteByPackage(packageName)
    }
    
    fun getWhitelistCountFlow(): Flow<Int> = 
        whitelistDao.getWhitelistCountFlow()
}
