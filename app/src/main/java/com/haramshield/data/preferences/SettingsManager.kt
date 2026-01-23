package com.haramshield.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.haramshield.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "haramshield_settings")

/**
 * Settings manager using DataStore for app configuration persistence
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    // ==================== Detection Categories ====================
    
    val nsfwEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NSFW_ENABLED] ?: true }
    
    val alcoholEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.ALCOHOL_ENABLED] ?: true }
    
    val tobaccoEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.TOBACCO_ENABLED] ?: true }
    
    val gamblingEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.GAMBLING_ENABLED] ?: true }
    
    // ==================== Detection Sensitivity ====================
    
    val nsfwThreshold: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NSFW_THRESHOLD] ?: Constants.DEFAULT_NSFW_THRESHOLD }
    
    val objectThreshold: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.OBJECT_THRESHOLD] ?: Constants.DEFAULT_OBJECT_THRESHOLD }
    
    // ==================== Lockout Settings ====================
    
    val nsfwLockoutTime: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NSFW_LOCKOUT_TIME] ?: "10m" }
    
    val healthLockoutTime: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.HEALTH_LOCKOUT_TIME] ?: "1m" }

    // ==================== Screenshot Interval ====================
    
    val screenshotIntervalMs: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SCREENSHOT_INTERVAL_MS] ?: Constants.DEFAULT_SCREENSHOT_INTERVAL_MS }
    
    // ==================== App State ====================
    
    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    
    val monitoringEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.MONITORING_ENABLED] ?: false }
    
    val isSnoozed: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_SNOOZED] ?: false }

    val firstInstallTime: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.FIRST_INSTALL_TIME] ?: System.currentTimeMillis() }

    val tamperAttemptCount: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.TAMPER_ATTEMPT_COUNT] ?: 0 }
    
    // ==================== Setters ====================
    
    suspend fun setNsfwEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NSFW_ENABLED] = enabled }
    }
    
    suspend fun setAlcoholEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ALCOHOL_ENABLED] = enabled }
    }
    
    suspend fun setTobaccoEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.TOBACCO_ENABLED] = enabled }
    }
    
    suspend fun setGamblingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.GAMBLING_ENABLED] = enabled }
    }
    
    suspend fun setNsfwThreshold(threshold: Float) {
        dataStore.edit { it[Keys.NSFW_THRESHOLD] = threshold.coerceIn(
            Constants.MIN_DETECTION_THRESHOLD, 
            Constants.MAX_DETECTION_THRESHOLD
        ) }
    }
    
    suspend fun setObjectThreshold(threshold: Float) {
        dataStore.edit { it[Keys.OBJECT_THRESHOLD] = threshold.coerceIn(
            Constants.MIN_DETECTION_THRESHOLD,
            Constants.MAX_DETECTION_THRESHOLD
        ) }
    }

    suspend fun setNsfwLockoutTime(time: String) {
        dataStore.edit { it[Keys.NSFW_LOCKOUT_TIME] = time }
    }

    suspend fun setHealthLockoutTime(time: String) {
        dataStore.edit { it[Keys.HEALTH_LOCKOUT_TIME] = time }
    }
    
    suspend fun setScreenshotIntervalMs(intervalMs: Long) {
        dataStore.edit { it[Keys.SCREENSHOT_INTERVAL_MS] = intervalMs.coerceIn(
            Constants.MIN_SCREENSHOT_INTERVAL_MS,
            Constants.MAX_SCREENSHOT_INTERVAL_MS
        ) }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
    
    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    suspend fun setIsSnoozed(snoozed: Boolean) {
        dataStore.edit { it[Keys.IS_SNOOZED] = snoozed }
    }
    
    // ==================== Snooze Timestamp ====================
    
    val snoozeUntil: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SNOOZE_UNTIL] ?: 0L }
    
    suspend fun setSnoozeUntil(timestamp: Long) {
        dataStore.edit { it[Keys.SNOOZE_UNTIL] = timestamp }
    }
    
    suspend fun clearSnooze() {
        dataStore.edit { 
            it[Keys.IS_SNOOZED] = false
            it[Keys.SNOOZE_UNTIL] = 0L
        }
    }
    
    suspend fun setFirstInstallTime(time: Long) {
        dataStore.edit { it[Keys.FIRST_INSTALL_TIME] = time }
    }

    suspend fun incrementTamperAttemptCount() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.TAMPER_ATTEMPT_COUNT] ?: 0
            preferences[Keys.TAMPER_ATTEMPT_COUNT] = current + 1
        }
    }
    
    suspend fun resetTamperAttemptCount() {
        dataStore.edit { it[Keys.TAMPER_ATTEMPT_COUNT] = 0 }
    }
    
    private object Keys {
        val NSFW_ENABLED = booleanPreferencesKey("nsfw_enabled")
        val ALCOHOL_ENABLED = booleanPreferencesKey("alcohol_enabled")
        val TOBACCO_ENABLED = booleanPreferencesKey("tobacco_enabled")
        val GAMBLING_ENABLED = booleanPreferencesKey("gambling_enabled")
        val NSFW_THRESHOLD = floatPreferencesKey("nsfw_threshold")
        val OBJECT_THRESHOLD = floatPreferencesKey("object_threshold")
        val NSFW_LOCKOUT_TIME = stringPreferencesKey("nsfw_lockout_time")
        val HEALTH_LOCKOUT_TIME = stringPreferencesKey("health_lockout_time")
        val SCREENSHOT_INTERVAL_MS = longPreferencesKey("screenshot_interval_ms")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val IS_SNOOZED = booleanPreferencesKey("is_snoozed")
        val SNOOZE_UNTIL = longPreferencesKey("snooze_until")
        val FIRST_INSTALL_TIME = longPreferencesKey("first_install_time")
        val TAMPER_ATTEMPT_COUNT = intPreferencesKey("tamper_attempt_count")
        val CUSTOM_BLOCKED_WORDS = stringSetPreferencesKey("custom_blocked_words")
    }
    
    // ==================== Custom Blocklist ====================
    
    val customBlockedWords: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CUSTOM_BLOCKED_WORDS] ?: emptySet() }
        
    suspend fun addBlockedWord(word: String) {
        dataStore.edit { preferences ->
            val currentParams = preferences[Keys.CUSTOM_BLOCKED_WORDS] ?: emptySet()
            preferences[Keys.CUSTOM_BLOCKED_WORDS] = currentParams + word.lowercase().trim()
        }
    }
    
    suspend fun removeBlockedWord(word: String) {
        dataStore.edit { preferences ->
            val currentParams = preferences[Keys.CUSTOM_BLOCKED_WORDS] ?: emptySet()
            preferences[Keys.CUSTOM_BLOCKED_WORDS] = currentParams - word.lowercase().trim()
        }
    }
}
