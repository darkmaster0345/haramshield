package com.haramshield.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security utilities for PIN hashing and verification
 */
@Singleton
class SecurityUtils @Inject constructor() {
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generate a random salt for PIN hashing
     */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
    
    /**
     * Hash a PIN with SHA-256 and salt
     */
    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        
        // Combine salt and PIN
        val combined = ByteArray(saltBytes.size + pinBytes.size)
        System.arraycopy(saltBytes, 0, combined, 0, saltBytes.size)
        System.arraycopy(pinBytes, 0, combined, saltBytes.size, pinBytes.size)
        
        // Hash with SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        
        // Multiple iterations for added security
        var hash = combined
        repeat(HASH_ITERATIONS) {
            hash = digest.digest(hash)
        }
        
        return Base64.getEncoder().encodeToString(hash)
    }
    
    /**
     * Verify a PIN against a stored hash
     */
    fun verifyPin(pin: String, salt: String, storedHash: String): Boolean {
        val computedHash = hashPin(pin, salt)
        return computedHash == storedHash
    }
    
    /**
     * Validate PIN format (4-6 digits)
     */
    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all { it.isDigit() }
    }
    
    /**
     * Check if PIN is too simple (e.g., all same digits, sequential)
     */
    fun isPinTooSimple(pin: String): Boolean {
        // All same digits (e.g., 1111, 0000)
        if (pin.all { it == pin[0] }) return true
        
        // Sequential ascending (e.g., 1234)
        val ascending = pin.indices.all { i -> 
            i == 0 || pin[i].digitToInt() == pin[i - 1].digitToInt() + 1 
        }
        if (ascending) return true
        
        // Sequential descending (e.g., 4321)
        val descending = pin.indices.all { i -> 
            i == 0 || pin[i].digitToInt() == pin[i - 1].digitToInt() - 1 
        }
        return descending
    }
    
    /**
     * Verify if the app is signed with the expected key.
     * Prevents tampered APKs from running.
     */
    fun verifyAppSignature(context: Context): Boolean {
        // Skip verification in debug builds to allow development
        if (com.haramshield.BuildConfig.DEBUG) {
            Timber.d("Debug build - skipping signature verification")
            return true
        }
        
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures != null) {
                for (signature in signatures) {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hashBytes = digest.digest(signature.toByteArray())
                    val hashString = Base64.getEncoder().encodeToString(hashBytes)
                    
                    Timber.d("APP_SIGNATURE: $hashString")
                    
                    if (hashString == EXPECTED_SIGNATURE_HASH) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Signature verification failed")
        }
        
        return false // Return true if you want to bypass in Debug mode
    }

    companion object {
        private const val SALT_LENGTH = 32
        private const val HASH_ITERATIONS = 10000
        private const val PIN_MIN_LENGTH = 4
        private const val PIN_MAX_LENGTH = 6
        
        // Placeholder for Release Key Hash (SHA-256). User must update this after generating keys.
        // Run app once, check Logcat for "APP_SIGNATURE", and paste value here.
        private const val EXPECTED_SIGNATURE_HASH = "REPLACE_WITH_YOUR_ACTUAL_SHA256_HASH" 
    }
}
