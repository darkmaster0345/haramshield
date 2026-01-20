package com.haramshield.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data like PIN using EncryptedSharedPreferences
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Store the hashed PIN
     */
    fun setHashedPin(hashedPin: String) {
        encryptedPrefs.edit().putString(KEY_HASHED_PIN, hashedPin).apply()
    }
    
    /**
     * Get the stored hashed PIN
     */
    fun getHashedPin(): String? {
        return encryptedPrefs.getString(KEY_HASHED_PIN, null)
    }
    
    /**
     * Check if PIN has been set
     */
    fun isPinSet(): Boolean {
        return encryptedPrefs.contains(KEY_HASHED_PIN)
    }
    
    /**
     * Clear the PIN
     */
    fun clearPin() {
        encryptedPrefs.edit().remove(KEY_HASHED_PIN).apply()
    }
    
    /**
     * Store the PIN salt
     */
    fun setSalt(salt: String) {
        encryptedPrefs.edit().putString(KEY_SALT, salt).apply()
    }
    
    /**
     * Get the stored salt
     */
    fun getSalt(): String? {
        return encryptedPrefs.getString(KEY_SALT, null)
    }
    
    /**
     * Store device admin enabled status
     */
    fun setDeviceAdminEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_DEVICE_ADMIN_ENABLED, enabled).apply()
    }
    
    /**
     * Check if device admin is enabled
     */
    fun isDeviceAdminEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_DEVICE_ADMIN_ENABLED, false)
    }
    
    /**
     * Clear all secure preferences
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_FILE_NAME = "haramshield_secure_prefs"
        private const val KEY_HASHED_PIN = "hashed_pin"
        private const val KEY_SALT = "salt"
        private const val KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled"
    }
}
