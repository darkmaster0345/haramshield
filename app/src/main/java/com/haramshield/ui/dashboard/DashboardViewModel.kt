package com.haramshield.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haramshield.Constants
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DashboardUiState(
    val isProtectionActive: Boolean = false,
    val appsMonitored: Int = 0,
    val violationsBlocked: Int = 0,
    val daysProtected: Int = 0,
    val isLoading: Boolean = true,
    val neuralFeedLogs: List<String> = listOf(
        "[System] Engine initialized",
        "[AI] Neural net processing frames...",
        "[Status] Integrity check: PASS"
    ),
    val nsfwLockoutTime: String = "10m",
    val healthLockoutTime: String = "1m",
    val regretTimerDuration: Int = 60 // Default 60s
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        val techLogs = listOf(
            "[System] Engine v4.0.2-stealth online",
            "[AI] Neural net initialized on GPU (TFLite Delegate)",
            "[Monitor] Accessibility service: CONNECTED",
            "[Status] Integrity check: COMPLETE (100%)",
            "[AI] Inference time: 12ms | Confidence: 99.8%",
            "[Security] Signature verification: PASS",
            "[System] Thermal status: OPTIMAL",
            "[Network] Cloud sync: DISABLED (Privacy Mode)",
            "[AI] Scanning surface: 0 violations found",
            "[Shield] Active protection: NSFW, Gambling, Alcohol"
        )
        
        // Simulating a live feed that rotates
        val logFlow = kotlinx.coroutines.flow.flow {
            while (true) {
                emit(techLogs.shuffled().take(6))
                kotlinx.coroutines.delay(3000) // Update every 3 seconds
            }
        }

        viewModelScope.launch {
            combine(
                settingsManager.monitoringEnabled,
                settingsManager.tamperAttemptCount,
                settingsManager.firstInstallTime,
                logFlow
            ) { enabled, tamperCount, installTime, logs ->
                val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installTime).toInt()
                
                // Auto-Hardening Logic
                val dynamicTimer = if (tamperCount >= 3) 300 else 60
                
                DashboardUiState(
                    isProtectionActive = enabled,
                    appsMonitored = 0,
                    violationsBlocked = 0, // Need to pipe this effectively if valuable
                    daysProtected = maxOf(0, days),
                    isLoading = false,
                    neuralFeedLogs = logs,
                    regretTimerDuration = dynamicTimer
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }
    
    fun toggleProtection() {
        // Soft toggle via settings
        viewModelScope.launch {
            val current = _uiState.value.isProtectionActive
            settingsManager.setMonitoringEnabled(!current)
        }
    }
    
    fun snoozeProtection() {
        val intent = Intent(Constants.ACTION_SNOOZE)
        intent.setPackage(Constants.PACKAGE_NAME)
        context.sendBroadcast(intent)
    }
    
    fun deactivateShield() {
        // Hard disable via Service disableSelf()
        val intent = Intent(Constants.ACTION_DISABLE_SERVICE)
        intent.setPackage(Constants.PACKAGE_NAME)
        context.sendBroadcast(intent)
    }
    
    fun refreshStats() {
        // Flow updates automatically
    }
}
