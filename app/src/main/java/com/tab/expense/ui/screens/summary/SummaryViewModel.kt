package com.tab.expense.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tab.expense.data.local.entity.Expense
import com.tab.expense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SummaryUiState(
    val expenses: List<Expense> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<SummaryUiState> = combine(
        _selectedMonth,
        _selectedYear,
        _isRefreshing,
        repository.getAllExpenses()
    ) { month, year, isRefreshing, allExpenses ->
        val (startDate, endDate) = getMonthDateRange(month, year)

        val filteredExpenses = allExpenses.filter { expense ->
            expense.date in startDate..endDate
        }

        val total = filteredExpenses.sumOf { it.amountMVR }

        SummaryUiState(
            expenses = filteredExpenses,
            monthlyTotal = total,
            isRefreshing = isRefreshing,
            selectedMonth = month,
            selectedYear = year
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState(isLoading = true)
    )

    fun selectMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun syncToSheets() {
        viewModelScope.launch {
            repository.syncUnsyncedExpenses()
        }
    }

    fun refreshExpenses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncExpensesFromSheets()
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun getMonthDateRange(month: Int, year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of month
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}
