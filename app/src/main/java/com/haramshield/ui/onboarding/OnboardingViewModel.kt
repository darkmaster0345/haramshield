package com.haramshield.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val isUsageStatsGranted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionHelper: PermissionHelper,
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    // Check permissions on resume
    fun checkPermissions() {
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = permissionHelper.isAccessibilityServiceEnabled(),
                isOverlayEnabled = permissionHelper.canDrawOverlays(),
                isNotificationEnabled = permissionHelper.areNotificationsEnabled(),
                isDeviceAdminActive = permissionHelper.isDeviceAdminActive(),
                isUsageStatsGranted = permissionHelper.hasUsageStatsPermission()
            )
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsManager.setOnboardingCompleted(true)
            // Auto-enable monitoring if core permissions are granted
            if (permissionHelper.isAccessibilityServiceEnabled()) {
                settingsManager.setMonitoringEnabled(true)
            }
        }
    }
}
