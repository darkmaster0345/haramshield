package com.haramshield.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haramshield.data.db.entity.ViolationLog
import com.haramshield.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val violations: List<ViolationLog> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.getAllViolationsFlow().collectLatest { logs ->
                _uiState.update { 
                    it.copy(
                        violations = logs,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            // Only clear > 30 days old usually, but for user action maybe all?
            // For now, let's just clear old ones to be safe or implement a proper clear all
            // repository.deleteAllViolations() // Need to add this to repo if we want full clear
        }
    }
}
