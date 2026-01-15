package com.tab.expense.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tab.expense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val spreadsheetId: String = "",
    val sheetName: String = "",
    val apiCredentials: String = "",
    val allowedSenders: String = "",
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: TestResult? = null,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class TestResult {
    object Success : TestResult()
    data class Failure(val message: String) : TestResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val spreadsheetId = repository.getSpreadsheetId() ?: ""
            val sheetName = repository.getSheetName() ?: ""
            val apiCredentials = repository.getApiCredentials() ?: ""
            val allowedSenders = repository.getAllowedSenders() ?: ""

            _uiState.value = _uiState.value.copy(
                spreadsheetId = spreadsheetId,
                sheetName = sheetName,
                apiCredentials = apiCredentials,
                allowedSenders = allowedSenders,
                isLoading = false
            )
        }
    }

    fun updateSpreadsheetId(id: String) {
        _uiState.value = _uiState.value.copy(spreadsheetId = id)
    }

    fun updateSheetName(name: String) {
        _uiState.value = _uiState.value.copy(sheetName = name)
    }

    fun updateApiCredentials(credentials: String) {
        _uiState.value = _uiState.value.copy(apiCredentials = credentials)
    }

    fun updateAllowedSenders(senders: String) {
        _uiState.value = _uiState.value.copy(allowedSenders = senders)
    }

    fun saveSettings() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            try {
                repository.saveSpreadsheetId(state.spreadsheetId)
                repository.saveSheetName(state.sheetName)
                repository.saveApiCredentials(state.apiCredentials)
                repository.saveAllowedSenders(state.allowedSenders)

                _uiState.value = state.copy(
                    isLoading = false,
                    saveSuccess = true
                )

                // Reset success flag after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(saveSuccess = false)
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to save settings: ${e.message}"
                )
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value

        if (state.spreadsheetId.isBlank() || state.sheetName.isBlank() || state.apiCredentials.isBlank()) {
            _uiState.value = state.copy(
                error = "Please fill in all Google Sheets fields before testing"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isTesting = true, testResult = null, error = null)

            try {
                // Save settings first
                repository.saveSpreadsheetId(state.spreadsheetId)
                repository.saveSheetName(state.sheetName)
                repository.saveApiCredentials(state.apiCredentials)

                // Test connection
                val success = repository.testSheetsConnection()

                _uiState.value = state.copy(
                    isTesting = false,
                    testResult = if (success) {
                        TestResult.Success
                    } else {
                        TestResult.Failure("Connection failed. Please check your credentials.")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isTesting = false,
                    testResult = TestResult.Failure("Error: ${e.message}")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
}
