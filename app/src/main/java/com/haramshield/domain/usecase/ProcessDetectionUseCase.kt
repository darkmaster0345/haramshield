package com.haramshield.domain.usecase

import com.haramshield.data.db.entity.LockedApp
import com.haramshield.data.db.entity.ViolationLog
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.data.repository.AppRepository
import com.haramshield.domain.model.DetectionResult
import com.haramshield.domain.model.DetectionSummary
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to process detection results and take action
 */
@Singleton
class ProcessDetectionUseCase @Inject constructor(
    private val repository: AppRepository,
    private val settingsManager: SettingsManager
) {
    
    /**
     * Process detection results for a given app
     * @param packageName The package name of the app being monitored
     * @param appLabel Human-readable app name
     * @param summary Detection results from ML models
     * @return ProcessingResult indicating what action was taken
     */
    suspend operator fun invoke(
        packageName: String,
        appLabel: String?,
        summary: DetectionSummary
    ): ProcessingResult {
        // Skip if no violations
        if (!summary.hasViolation) {
            return ProcessingResult.NoAction
        }
        
        val violation = summary.highestConfidenceViolation ?: return ProcessingResult.NoAction
        
        // Check if already locked
        if (repository.isAppLocked(packageName)) {
            return ProcessingResult.AlreadyLocked
        }
        
        // Check if whitelisted
        if (repository.isWhitelisted(packageName)) {
            return ProcessingResult.Whitelisted
        }
        
        // Determine lockout duration based on category (User Request)
        // NSFW/Gambling = 10 Minutes (Strict)
        // Alcohol/Tobacco = 1 Minute (Warning)
        val lockoutMinutes = when (violation.category) {
            com.haramshield.domain.model.ContentCategory.NSFW,
            com.haramshield.domain.model.ContentCategory.GAMBLING,
            com.haramshield.domain.model.ContentCategory.SCREEN_BLOCKED -> 10L
            com.haramshield.domain.model.ContentCategory.ALCOHOL,
            com.haramshield.domain.model.ContentCategory.TOBACCO -> 1L
            // Default fallthrough for DIET/UNKNOWN - use safe default
            else -> 5L
        }
        
        val lockUntil = System.currentTimeMillis() + (lockoutMinutes * 60 * 1000)
        
        // Log the violation
        val violationLog = ViolationLog(
            packageName = packageName,
            category = violation.category.name,
            confidence = violation.confidence,
            appLabel = appLabel,
            lockedOut = true
        )
        repository.logViolation(violationLog)
        
        // Lock the app
        val lockedApp = LockedApp(
            packageName = packageName,
            lockUntil = lockUntil,
            category = violation.category.name,
            confidence = violation.confidence
        )
        repository.lockApp(lockedApp)
        
        return ProcessingResult.AppLocked(
            lockedApp = lockedApp,
            violationLog = violationLog
        )
    }
}

sealed class ProcessingResult {
    data object NoAction : ProcessingResult()
    data object AlreadyLocked : ProcessingResult()
    data object Whitelisted : ProcessingResult()
    
    data class AppLocked(
        val lockedApp: LockedApp,
        val violationLog: ViolationLog
    ) : ProcessingResult()
}
