package com.tab.expense.ui.screens.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tab.expense.data.local.entity.Category
import com.tab.expense.data.local.entity.Expense
import com.tab.expense.data.repository.ExpenseRepository
import com.tab.expense.util.Constants
import com.tab.expense.util.CurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryUiState(
    val date: Long = System.currentTimeMillis(),
    val selectedCategory: Category? = null,
    val categories: List<Category> = emptyList(),
    val description: String = "",
    val selectedCurrency: String = Constants.CURRENCY_MVR,
    val amount: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    selectedCategory = categories.firstOrNull()
                )
            }
        }
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun selectCurrency(currency: String) {
        _uiState.value = _uiState.value.copy(selectedCurrency = currency)
    }

    fun updateAmount(amount: String) {
        // Only allow numbers and one decimal point
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.value = _uiState.value.copy(amount = amount)
        }
    }

    fun saveExpense() {
        val state = _uiState.value

        // Validation
        if (state.description.isBlank()) {
            _uiState.value = state.copy(error = "Please enter a description")
            return
        }

        if (state.amount.isBlank() || state.amount.toDoubleOrNull() == null) {
            _uiState.value = state.copy(error = "Please enter a valid amount")
            return
        }

        val amountValue = state.amount.toDouble()
        if (amountValue <= 0) {
            _uiState.value = state.copy(error = "Amount must be greater than 0")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Convert to MVR if needed
                val amountMVR = if (state.selectedCurrency == Constants.CURRENCY_USD) {
                    CurrencyConverter.usdToMvr(amountValue)
                } else {
                    amountValue
                }

                val expense = Expense(
                    date = state.date,
                    category = state.selectedCategory?.name ?: "Other",
                    description = state.description,
                    amountMVR = amountMVR,
                    originalCurrency = state.selectedCurrency,
                    originalAmount = amountValue
                )

                repository.insertExpense(expense)

                _uiState.value = state.copy(
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to save expense: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = EntryUiState(
            categories = _uiState.value.categories,
            selectedCategory = _uiState.value.categories.firstOrNull()
        )
    }
}
