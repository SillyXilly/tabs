package com.tab.expense.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tab.expense.data.local.database.ExpenseDao
import com.tab.expense.data.local.database.CategoryDao
import com.tab.expense.data.local.entity.Expense
import com.tab.expense.data.local.entity.Category
import com.tab.expense.data.remote.GoogleSheetsService
import com.tab.expense.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class ExpenseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val googleSheetsService: GoogleSheetsService
) {
    // Expense operations
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getTotalByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalByDateRange(startDate, endDate)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long {
        val id = expenseDao.insertExpense(expense)
        syncExpenseToSheets(expense.copy(id = id))
        return id
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }


    // Sync operations
    suspend fun syncUnsyncedExpenses(): Boolean {
        val unsynced = expenseDao.getUnsyncedExpenses()
        if (unsynced.isEmpty()) return true

        val spreadsheetId = getSpreadsheetId() ?: return false
        val sheetName = getSheetName() ?: return false
        val credentials = getApiCredentials() ?: return false

        if (!googleSheetsService.initialize(credentials)) {
            return false
        }

        googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

        val success = googleSheetsService.appendExpenses(spreadsheetId, sheetName, unsynced)
        if (success) {
            expenseDao.markMultipleAsSynced(unsynced.map { it.id })
        }
        return success
    }

    private suspend fun syncExpenseToSheets(expense: Expense) {
        try {
            android.util.Log.d("ExpenseRepository", "Attempting to sync expense: id=${expense.id}, desc=${expense.description}, amount=${expense.amountMVR}")

            val spreadsheetId = getSpreadsheetId()
            if (spreadsheetId == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No spreadsheet ID configured")
                return
            }

            val sheetName = getSheetName()
            if (sheetName == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No sheet name configured")
                return
            }

            val credentials = getApiCredentials()
            if (credentials == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No API credentials configured")
                return
            }

            android.util.Log.d("ExpenseRepository", "Initializing Google Sheets service")
            if (!googleSheetsService.initialize(credentials)) {
                android.util.Log.e("ExpenseRepository", "Failed to initialize Google Sheets service")
                return
            }

            android.util.Log.d("ExpenseRepository", "Ensuring header row exists")
            googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

            android.util.Log.d("ExpenseRepository", "Appending expense to sheet: $sheetName")
            val success = googleSheetsService.appendExpense(spreadsheetId, sheetName, expense)
            if (success) {
                android.util.Log.d("ExpenseRepository", "Successfully synced expense ${expense.id}")
                expenseDao.markAsSynced(expense.id)
            } else {
                android.util.Log.e("ExpenseRepository", "Failed to append expense to sheet")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Exception during sync: ${e.message}", e)
            // Expense will remain unsynced and will be synced later
        }
    }

    // Settings operations
    suspend fun saveSpreadsheetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SPREADSHEET_ID)] = id
        }
    }

    suspend fun getSpreadsheetId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SPREADSHEET_ID)]
        }.first()
    }

    suspend fun saveSheetName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SHEET_NAME)] = name
        }
    }

    suspend fun getSheetName(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SHEET_NAME)]
        }.first()
    }

    suspend fun saveApiCredentials(credentials: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_API_CREDENTIALS)] = credentials
        }
    }

    suspend fun getApiCredentials(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_API_CREDENTIALS)]
        }.first()
    }

    suspend fun saveAllowedSenders(senders: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_ALLOWED_SENDERS)] = senders
        }
    }

    suspend fun getAllowedSenders(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_ALLOWED_SENDERS)]
        }.first()
    }

    suspend fun testSheetsConnection(): Boolean {
        val spreadsheetId = getSpreadsheetId() ?: return false
        val sheetName = getSheetName() ?: return false
        val credentials = getApiCredentials() ?: return false

        if (!googleSheetsService.initialize(credentials)) {
            return false
        }

        return googleSheetsService.testConnection(spreadsheetId, sheetName)
    }

    suspend fun fetchExpensesFromSheets(): List<Expense> {
        val spreadsheetId = getSpreadsheetId() ?: return emptyList()
        val sheetName = getSheetName() ?: return emptyList()
        val credentials = getApiCredentials() ?: return emptyList()

        if (!googleSheetsService.initialize(credentials)) {
            return emptyList()
        }

        return googleSheetsService.fetchRecentExpenses(spreadsheetId, sheetName, limit = 30)
    }

    suspend fun syncExpensesFromSheets() {
        val expenses = fetchExpensesFromSheets()
        val allLocalExpenses = expenseDao.getAllExpenses().first()

        expenses.forEach { expense ->
            // Check if expense already exists (by date, category, description, amount)
            // We consider it a duplicate if:
            // 1. Same date (within same day)
            // 2. Same category
            // 3. Same description
            // 4. Same amount
            // This prevents duplicates from being inserted when fetching from Sheets
            val isDuplicate = allLocalExpenses.any { local ->
                local.category == expense.category &&
                local.description.equals(expense.description, ignoreCase = true) &&
                kotlin.math.abs(local.amountMVR - expense.amountMVR) < 0.01 &&
                isSameDay(local.date, expense.date)
            }

            if (!isDuplicate) {
                // Mark as synced since it came from Sheets
                expenseDao.insertExpense(expense.copy(isSynced = true))
            }
        }
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: String): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getAllCategories().first()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                Category("food", "Food", "🍔", 1),
                Category("transport", "Transport", "🚕", 2),
                Category("shopping", "Shopping", "🛒", 3),
                Category("health", "Health", "💊", 4),
                Category("entertainment", "Entertainment", "🎮", 5),
                Category("bills", "Bills", "🏠", 6),
                Category("other", "Other", "📱", 7)
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}
