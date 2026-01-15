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
            val spreadsheetId = getSpreadsheetId() ?: return
            val sheetName = getSheetName() ?: return
            val credentials = getApiCredentials() ?: return

            if (!googleSheetsService.initialize(credentials)) {
                return
            }

            googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

            val success = googleSheetsService.appendExpense(spreadsheetId, sheetName, expense)
            if (success) {
                expenseDao.markAsSynced(expense.id)
            }
        } catch (e: Exception) {
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
        expenses.forEach { expense ->
            // Check if expense already exists (by date, category, description, amount)
            val existing = expenseDao.getAllExpenses().first().find {
                it.date == expense.date &&
                it.category == expense.category &&
                it.description == expense.description &&
                it.amountMVR == expense.amountMVR
            }
            if (existing == null) {
                expenseDao.insertExpense(expense)
            }
        }
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
