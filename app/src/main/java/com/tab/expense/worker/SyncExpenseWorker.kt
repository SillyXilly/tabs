package com.tab.expense.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tab.expense.data.local.database.ExpenseDao
import com.tab.expense.data.local.preferences.dataStore
import com.tab.expense.data.remote.GoogleSheetsService
import com.tab.expense.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * WorkManager worker for reliably syncing expenses to Google Sheets
 *
 * Benefits:
 * - Survives app restarts and process deaths
 * - Automatic retry with exponential backoff
 * - Only runs when network is available
 * - Respects battery optimization
 */
@HiltWorker
class SyncExpenseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val googleSheetsService: GoogleSheetsService
) : CoroutineWorker(context, params) {

    private val TAG = "SyncExpenseWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "=== SYNC WORKER STARTED ===")

            // Get expense ID from input data
            val expenseId = inputData.getLong(KEY_EXPENSE_ID, -1L)
            if (expenseId == -1L) {
                Log.e(TAG, "No expense ID provided")
                return Result.failure()
            }

            Log.d(TAG, "Syncing expense ID: $expenseId")

            // Get the expense
            val expense = expenseDao.getExpenseById(expenseId)
            if (expense == null) {
                Log.e(TAG, "Expense not found: $expenseId")
                return Result.failure()
            }

            // Get Google Sheets configuration
            val spreadsheetId = getSpreadsheetId()
            if (spreadsheetId == null) {
                Log.w(TAG, "No spreadsheet ID configured - sync skipped")
                return Result.failure()
            }

            val sheetName = getSheetName()
            if (sheetName == null) {
                Log.w(TAG, "No sheet name configured - sync skipped")
                return Result.failure()
            }

            val credentials = getApiCredentials()
            if (credentials == null) {
                Log.w(TAG, "No API credentials configured - sync skipped")
                return Result.failure()
            }

            Log.d(TAG, "Initializing Google Sheets service")
            if (!googleSheetsService.initialize(credentials)) {
                Log.e(TAG, "Failed to initialize Google Sheets service")
                return Result.retry()
            }

            Log.d(TAG, "Ensuring header row exists")
            googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

            Log.d(TAG, "Appending expense to sheet")
            val success = googleSheetsService.appendExpense(spreadsheetId, sheetName, expense)

            return if (success) {
                Log.d(TAG, "✓ Successfully synced expense $expenseId to Google Sheets")

                // Mark as synced
                expenseDao.updateExpense(expense.copy(isSynced = true))

                Log.d(TAG, "=== SYNC WORKER COMPLETED SUCCESSFULLY ===")
                Result.success()
            } else {
                Log.e(TAG, "✗ Failed to sync expense to Google Sheets")
                Log.d(TAG, "=== SYNC WORKER FAILED - WILL RETRY ===")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception during sync: ${e.message}", e)
            Log.d(TAG, "=== SYNC WORKER FAILED WITH EXCEPTION - WILL RETRY ===")
            // Retry on exception (network issues, etc.)
            Result.retry()
        }
    }

    private suspend fun getSpreadsheetId(): String? {
        return applicationContext.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SPREADSHEET_ID)]
        }.first()
    }

    private suspend fun getSheetName(): String? {
        return applicationContext.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_SHEET_NAME)]
        }.first()
    }

    private suspend fun getApiCredentials(): String? {
        return applicationContext.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Constants.PREF_API_CREDENTIALS)]
        }.first()
    }

    companion object {
        const val KEY_EXPENSE_ID = "expense_id"
        const val WORK_NAME_PREFIX = "sync_expense_"
    }
}
