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
        android.util.Log.d("EntryViewModel", "=== LOAD EXPENSE START ===")
        android.util.Log.d("EntryViewModel", "Loading expense ID: $expenseId")

        viewModelScope.launch {
            try {
                val expense = repository.getExpenseById(expenseId)
                if (expense != null) {
                    android.util.Log.d("EntryViewModel", "✓ Expense found: desc=${expense.description}, amount=${expense.originalAmount}, currency=${expense.originalCurrency}")

                    // Find matching category
                    val category = _uiState.value.categories.find {
                        it.name.equals(expense.category, ignoreCase = true)
                    }
                    android.util.Log.d("EntryViewModel", "Category matched: ${category?.name ?: "none, using first"}")

                    _uiState.value = _uiState.value.copy(
                        expenseId = expenseId,
                        date = expense.date,
                        selectedCategory = category ?: _uiState.value.categories.firstOrNull(),
                        description = expense.description,
                        selectedCurrency = expense.originalCurrency,
                        amount = expense.originalAmount.toString()
                    )
                    android.util.Log.d("EntryViewModel", "✓ UI state updated with expense data")
                } else {
                    android.util.Log.w("EntryViewModel", "✗ Expense not found for ID: $expenseId")
                }
                android.util.Log.d("EntryViewModel", "=== LOAD EXPENSE END ===")
            } catch (e: Exception) {
                android.util.Log.e("EntryViewModel", "✗ Failed to load expense: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load expense: ${e.message}"
                )
                android.util.Log.d("EntryViewModel", "=== LOAD EXPENSE END (ERROR) ===")
            }
        }
    }

    fun saveExpense() {
        val state = _uiState.value

        android.util.Log.d("EntryViewModel", "=== SAVE EXPENSE START ===")
        android.util.Log.d("EntryViewModel", "Mode: ${if (state.expenseId != null) "EDIT" else "NEW"}")
        android.util.Log.d("EntryViewModel", "Expense ID: ${state.expenseId}")

        // Validation
        if (state.description.isBlank()) {
            android.util.Log.w("EntryViewModel", "✗ Validation failed: Description is blank")
            _uiState.value = state.copy(error = "Please enter a description")
            return
        }

        if (state.amount.isBlank() || state.amount.toDoubleOrNull() == null) {
            android.util.Log.w("EntryViewModel", "✗ Validation failed: Invalid amount")
            _uiState.value = state.copy(error = "Please enter a valid amount")
            return
        }

        val amountValue = state.amount.toDouble()
        if (amountValue <= 0) {
            android.util.Log.w("EntryViewModel", "✗ Validation failed: Amount must be > 0")
            _uiState.value = state.copy(error = "Amount must be greater than 0")
            return
        }

        android.util.Log.d("EntryViewModel", "✓ Validation passed")
        android.util.Log.d("EntryViewModel", "Data: desc=${state.description}, amount=$amountValue ${state.selectedCurrency}, category=${state.selectedCategory?.name}")

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Convert to MVR if needed
                val amountMVR = if (state.selectedCurrency == Constants.CURRENCY_USD) {
                    CurrencyConverter.usdToMvr(amountValue)
                } else {
                    amountValue
                }
                android.util.Log.d("EntryViewModel", "Amount in MVR: $amountMVR")

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
                    android.util.Log.d("EntryViewModel", "Calling repository.updateExpense()...")
                    repository.updateExpense(expense)
                    android.util.Log.d("EntryViewModel", "✓ Update completed")
                } else {
                    // Insert new expense
                    android.util.Log.d("EntryViewModel", "Calling repository.insertExpense()...")
                    repository.insertExpense(expense)
                    android.util.Log.d("EntryViewModel", "✓ Insert completed")
                }

                _uiState.value = state.copy(
                    isLoading = false,
                    isSaved = true
                )
                android.util.Log.d("EntryViewModel", "✓ Save successful, navigating back")
                android.util.Log.d("EntryViewModel", "=== SAVE EXPENSE END ===")
            } catch (e: Exception) {
                android.util.Log.e("EntryViewModel", "✗ Save failed: ${e.message}", e)
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to save expense: ${e.message}"
                )
                android.util.Log.d("EntryViewModel", "=== SAVE EXPENSE END (ERROR) ===")
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
        val expenseId = state.expenseId

        android.util.Log.d("EntryViewModel", "=== DELETE EXPENSE START ===")
        android.util.Log.d("EntryViewModel", "Expense ID: $expenseId")

        if (expenseId == null) {
            android.util.Log.w("EntryViewModel", "✗ Cannot delete: No expense ID")
            return
        }

        _uiState.value = state.copy(
            isLoading = true,
            showDeleteDialog = false,
            error = null
        )

        viewModelScope.launch {
            try {
                android.util.Log.d("EntryViewModel", "Calling repository.deleteExpenseById($expenseId)...")
                repository.deleteExpenseById(expenseId)
                android.util.Log.d("EntryViewModel", "✓ Delete completed")

                _uiState.value = state.copy(
                    isLoading = false,
                    isDeleted = true
                )
                android.util.Log.d("EntryViewModel", "✓ Delete successful, navigating back")
                android.util.Log.d("EntryViewModel", "=== DELETE EXPENSE END ===")
            } catch (e: Exception) {
                android.util.Log.e("EntryViewModel", "✗ Delete failed: ${e.message}", e)
                _uiState.value = state.copy(
                    isLoading = false,
                    error = "Failed to delete expense: ${e.message}"
                )
                android.util.Log.d("EntryViewModel", "=== DELETE EXPENSE END (ERROR) ===")
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
