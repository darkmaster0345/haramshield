package com.haramshield.domain.usecase

import com.haramshield.data.db.entity.WhitelistedApp
import com.haramshield.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing app whitelist
 */
@Singleton
class ManageWhitelistUseCase @Inject constructor(
    private val repository: AppRepository
) {
    
    /**
     * Add an app to the whitelist
     */
    suspend fun addToWhitelist(packageName: String, appLabel: String) {
        val app = WhitelistedApp(
            packageName = packageName,
            appLabel = appLabel
        )
        repository.addToWhitelist(app)
        
        // If the app is currently locked, unlock it
        if (repository.isAppLocked(packageName)) {
            repository.unlockApp(packageName)
        }
    }
    
    /**
     * Remove an app from the whitelist
     */
    suspend fun removeFromWhitelist(packageName: String) {
        repository.removeFromWhitelist(packageName)
    }
    
    /**
     * Check if an app is whitelisted
     */
    suspend fun isWhitelisted(packageName: String): Boolean {
        return repository.isWhitelisted(packageName)
    }
    
    /**
     * Get Flow of whitelisted apps
     */
    fun getWhitelistedAppsFlow(): Flow<List<WhitelistedApp>> {
        return repository.getWhitelistedAppsFlow()
    }
    
    /**
     * Get Flow of whitelisted package names
     */
    fun getWhitelistedPackageNamesFlow(): Flow<List<String>> {
        return repository.getWhitelistedPackageNamesFlow()
    }
    
    /**
     * Get all whitelisted apps
     */
    suspend fun getAllWhitelistedApps(): List<WhitelistedApp> {
        return repository.getAllWhitelistedApps()
    }
    
    /**
     * Toggle whitelist status for an app
     * @return true if app is now whitelisted, false if removed
     */
    suspend fun toggleWhitelist(packageName: String, appLabel: String): Boolean {
        return if (repository.isWhitelisted(packageName)) {
            removeFromWhitelist(packageName)
            false
        } else {
            addToWhitelist(packageName, appLabel)
            true
        }
    }
}
