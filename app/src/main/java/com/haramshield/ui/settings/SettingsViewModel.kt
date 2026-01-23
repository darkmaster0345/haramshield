package com.haramshield.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haramshield.data.preferences.SecurePreferences
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val nsfwEnabled: Boolean = true,
    val alcoholEnabled: Boolean = true,
    val tobaccoEnabled: Boolean = true,
    val gamblingEnabled: Boolean = true,
    val sensitivity: Float = 0.7f,
    val lockoutDurationMinutes: Long = 15,
    val isLoading: Boolean = true,
    val customBlockedWords: Set<String> = emptySet()
)

data class PinState(
    val isPinSet: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val securePreferences: SecurePreferences,
    private val securityUtils: SecurityUtils
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _pinState = MutableStateFlow(PinState())
    val pinState: StateFlow<PinState> = _pinState.asStateFlow()
    
    init {
        // Collect settings from DataStore
        viewModelScope.launch {
            combine(
                settingsManager.nsfwEnabled,
                settingsManager.alcoholEnabled,
                settingsManager.tobaccoEnabled,
                settingsManager.gamblingEnabled,
                settingsManager.nsfwThreshold
            ) { nsfw, alcohol, tobacco, gambling, threshold ->
                SettingsUiState(
                    nsfwEnabled = nsfw,
                    alcoholEnabled = alcohol,
                    tobaccoEnabled = tobacco,
                    gamblingEnabled = gambling,
                    sensitivity = threshold
                )
            }.combine(settingsManager.customBlockedWords) { state, customWords ->
                state.copy(
                    customBlockedWords = customWords,
                    isLoading = false
                )
            }.collectLatest { newState ->
                _uiState.update { currentState ->
                    newState.copy(lockoutDurationMinutes = currentState.lockoutDurationMinutes)
                }
            }
        }
        
        // lockoutDurationMinutes is now handled per-category in ProcessDetectionUseCase
        // No need for a separate collector
        
        
        // Check PIN state
        checkPinStatus()
    }
    
    private fun checkPinStatus() {
        val isSet = securePreferences.isPinSet()
        _pinState.update { it.copy(isPinSet = isSet) }
    }
    
    fun setNsfwEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setNsfwEnabled(enabled) }
    }
    
    fun setAlcoholEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setAlcoholEnabled(enabled) }
    }
    
    fun setTobaccoEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setTobaccoEnabled(enabled) }
    }
    
    fun setGamblingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setGamblingEnabled(enabled) }
    }
    
    fun setSensitivity(value: Float) {
        viewModelScope.launch { settingsManager.setNsfwThreshold(value) }
    }
    
    // Lockout duration is now per-category (NSFW vs Health) - see setNsfwLockoutTime/setHealthLockoutTime in DashboardViewModel
    
    fun setPin(newPin: String, confirmPin: String, oldPin: String): Boolean {
        if (newPin != confirmPin) return false
        if (!securityUtils.isValidPinFormat(newPin)) return false
        if (securityUtils.isPinTooSimple(newPin)) return false
        
        // Verify old PIN if set
        if (securePreferences.isPinSet()) {
            val salt = securePreferences.getSalt() ?: return false
            val storedHash = securePreferences.getHashedPin() ?: return false
            if (!securityUtils.verifyPin(oldPin, salt, storedHash)) {
                return false
            }
        }
        
        val salt = securityUtils.generateSalt()
        val hashedPin = securityUtils.hashPin(newPin, salt)
        
        securePreferences.setSalt(salt)
        securePreferences.setHashedPin(hashedPin)
        checkPinStatus()
        return true
    }
    
    fun disableMonitoring() {
        viewModelScope.launch {
            settingsManager.setMonitoringEnabled(false)
        }
    }
    
    fun addBlockedWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch { settingsManager.addBlockedWord(word) }
    }
    
    fun removeBlockedWord(word: String) {
        viewModelScope.launch { settingsManager.removeBlockedWord(word) }
    }
}
