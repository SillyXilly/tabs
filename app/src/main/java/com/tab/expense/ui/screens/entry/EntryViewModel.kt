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
    val expenseId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val selectedCategory: Category? = null,
    val categories: List<Category> = emptyList(),
    val description: String = "",
    val selectedCurrency: String = Constants.CURRENCY_MVR,
    val amount: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false
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

    fun loadExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                val expense = repository.getExpenseById(expenseId)
                if (expense != null) {
                    // Find matching category
                    val category = _uiState.value.categories.find {
                        it.name.equals(expense.category, ignoreCase = true)
                    }

                    _uiState.value = _uiState.value.copy(
                        expenseId = expenseId,
                        date = expense.date,
                        selectedCategory = category ?: _uiState.value.categories.firstOrNull(),
                        description = expense.description,
                        selectedCurrency = expense.originalCurrency,
                        amount = expense.originalAmount.toString()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load expense: ${e.message}"
                )
            }
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
                    id = state.expenseId ?: 0,
                    date = state.date,
                    category = state.selectedCategory?.name ?: "Other",
                    description = state.description,
                    amountMVR = amountMVR,
                    originalCurrency = state.selectedCurrency,
                    originalAmount = amountValue
                )

                if (state.expenseId != null) {
                    // Update existing expense
                    repository.updateExpense(expense)
                } else {
                    // Insert new expense
                    repository.insertExpense(expense)
                }

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

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun deleteExpense() {
        val state = _uiState.value
        val expenseId = state.expenseId ?: return

        _uiState.value = state.copy(
            isLoading = true,
            showDeleteDialog = false,
            error = null
        )

        viewModelScope.launch {
            try {
                repository.deleteExpenseById(expenseId)
                _uiState.value = state.copy(
                    isLoading = false,
                    isDeleted = true
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to delete expense: ${e.message}"
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

    fun prefillData(date: Long? = null, description: String? = null, amount: Double? = null) {
        _uiState.value = _uiState.value.copy(
            date = date ?: _uiState.value.date,
            description = description ?: _uiState.value.description,
            amount = amount?.toString() ?: _uiState.value.amount
        )
    }
}
