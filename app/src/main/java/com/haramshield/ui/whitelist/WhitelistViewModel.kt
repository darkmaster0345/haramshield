package com.haramshield.ui.whitelist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haramshield.domain.usecase.ManageWhitelistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val label: String,
    val isWhitelisted: Boolean
)

data class WhitelistUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val manageWhitelistUseCase: ManageWhitelistUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()
    
    init {
        // Observe whitelist changes
        viewModelScope.launch {
            manageWhitelistUseCase.getWhitelistedPackageNamesFlow().collectLatest { whitelistedPkgs ->
                val currentApps = _uiState.value.allApps.map { app ->
                    app.copy(isWhitelisted = whitelistedPkgs.contains(app.packageName))
                }
                
                _uiState.update { 
                    it.copy(
                        allApps = currentApps,
                        filteredApps = filterApps(currentApps, it.searchQuery)
                    )
                }
            }
        }
    }
    
    fun loadInstalledApps(packageManager: PackageManager) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val apps = withContext(Dispatchers.IO) {
                val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val whitelisted = manageWhitelistUseCase.getAllWhitelistedApps().map { it.packageName }.toSet()
                
                installed
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 } // Filter system apps broadly, keep user apps
                    .map { appInfo ->
                        AppInfo(
                            packageName = appInfo.packageName,
                            label = packageManager.getApplicationLabel(appInfo).toString(),
                            isWhitelisted = whitelisted.contains(appInfo.packageName)
                        )
                    }
                    .sortedBy { it.label }
            }
            
            _uiState.update { 
                it.copy(
                    allApps = apps,
                    filteredApps = filterApps(apps, it.searchQuery),
                    isLoading = false
                )
            }
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredApps = filterApps(it.allApps, query)
            )
        }
    }
    
    fun toggleWhitelist(app: AppInfo, isWhitelisted: Boolean) {
        viewModelScope.launch {
            if (isWhitelisted) {
                manageWhitelistUseCase.addToWhitelist(app.packageName, app.label)
            } else {
                manageWhitelistUseCase.removeFromWhitelist(app.packageName)
            }
        }
    }
    
    private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        return apps.filter { 
            it.label.contains(query, ignoreCase = true) || 
            it.packageName.contains(query, ignoreCase = true) 
        }
    }
}
