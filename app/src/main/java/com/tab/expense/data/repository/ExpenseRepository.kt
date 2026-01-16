package com.tab.expense.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.tab.expense.data.local.database.ExpenseDao
import com.tab.expense.data.local.database.CategoryDao
import com.tab.expense.data.local.entity.Expense
import com.tab.expense.data.local.entity.Category
import com.tab.expense.data.remote.GoogleSheetsService
import com.tab.expense.util.Constants
import com.tab.expense.worker.SyncExpenseWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
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
        android.util.Log.d("ExpenseRepository", "=== INSERT EXPENSE START ===")
        android.util.Log.d("ExpenseRepository", "Expense details: desc=${expense.description}, amount=${expense.amountMVR}, date=${expense.date}")

        // Normalize date to midnight (ignore time)
        val normalizedExpense = expense.copy(
            date = normalizeToMidnight(expense.date),
            isSynced = false  // Mark as not synced yet
        )
        android.util.Log.d("ExpenseRepository", "Date normalized to midnight: ${normalizedExpense.date}")

        // Save to local DB immediately (fast, always works)
        val id = expenseDao.insertExpense(normalizedExpense)
        android.util.Log.d("ExpenseRepository", "✓ Expense saved to local DB with ID: $id")

        // Queue WorkManager job for reliable background sync
        android.util.Log.d("ExpenseRepository", "Queueing WorkManager sync job for expense $id")
        queueSyncWork(id)

        android.util.Log.d("ExpenseRepository", "=== INSERT EXPENSE END (ID: $id) ===")
        return id
    }

    /**
     * Queue a WorkManager job to sync the expense to Google Sheets
     *
     * Benefits over CoroutineScope:
     * - Survives app restarts and process deaths
     * - Automatic retry with exponential backoff
     * - Only runs when network is available
     * - Guaranteed execution
     */
    private fun queueSyncWork(expenseId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<SyncExpenseWorker>()
            .setInputData(
                workDataOf(SyncExpenseWorker.KEY_EXPENSE_ID to expenseId)
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "${SyncExpenseWorker.WORK_NAME_PREFIX}$expenseId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        android.util.Log.d("ExpenseRepository", "✓ WorkManager sync job queued for expense $expenseId")
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

    private suspend fun syncExpenseToSheets(expense: Expense): Boolean {
        try {
            android.util.Log.d("ExpenseRepository", "Attempting to sync expense: desc=${expense.description}, amount=${expense.amountMVR}")

            val spreadsheetId = getSpreadsheetId()
            if (spreadsheetId == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No spreadsheet ID configured")
                return false
            }

            val sheetName = getSheetName()
            if (sheetName == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No sheet name configured")
                return false
            }

            val credentials = getApiCredentials()
            if (credentials == null) {
                android.util.Log.w("ExpenseRepository", "Sync skipped: No API credentials configured")
                return false
            }

            android.util.Log.d("ExpenseRepository", "Initializing Google Sheets service")
            if (!googleSheetsService.initialize(credentials)) {
                android.util.Log.e("ExpenseRepository", "Failed to initialize Google Sheets service")
                return false
            }

            android.util.Log.d("ExpenseRepository", "Ensuring header row exists")
            googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

            android.util.Log.d("ExpenseRepository", "Appending expense to sheet: $sheetName")
            val success = googleSheetsService.appendExpense(spreadsheetId, sheetName, expense)
            if (success) {
                android.util.Log.d("ExpenseRepository", "Successfully synced expense to Sheets")
            } else {
                android.util.Log.e("ExpenseRepository", "Failed to append expense to sheet")
            }
            return success
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Exception during sync: ${e.message}", e)
            return false
        }
    }

    suspend fun refreshFromSheets() {
        try {
            android.util.Log.d("ExpenseRepository", "Refreshing from Sheets: clearing local DB")
            // Clear all local expenses
            expenseDao.deleteAllExpenses()

            // Fetch from Sheets
            val expenses = fetchExpensesFromSheets()
            android.util.Log.d("ExpenseRepository", "Fetched ${expenses.size} expenses from Sheets")

            // Insert all fetched expenses
            expenses.forEach { expense ->
                expenseDao.insertExpense(expense)
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error refreshing from Sheets: ${e.message}", e)
        }
    }

    private fun normalizeToMidnight(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
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
