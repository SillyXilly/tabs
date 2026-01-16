package com.tab.expense.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tab.expense.data.local.database.ExpenseDao
import com.tab.expense.data.remote.GoogleSheetsService
import com.tab.expense.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Periodic worker to sync all unsynced expenses to Google Sheets
 *
 * This provides a safety net for:
 * - Expenses that failed to sync on first attempt
 * - App crashes/restarts during sync
 * - Network issues that occurred during initial sync
 *
 * Runs every 15 minutes (configurable)
 */
@HiltWorker
class PeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val googleSheetsService: GoogleSheetsService
) : CoroutineWorker(context, params) {

    private val TAG = "PeriodicSyncWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "=== PERIODIC SYNC STARTED ===")

            // Get all unsynced expenses
            val unsyncedExpenses = expenseDao.getUnsyncedExpenses()

            if (unsyncedExpenses.isEmpty()) {
                Log.d(TAG, "No unsynced expenses found")
                return Result.success()
            }

            Log.d(TAG, "Found ${unsyncedExpenses.size} unsynced expenses")

            // Get Google Sheets configuration
            val spreadsheetId = getSpreadsheetId()
            if (spreadsheetId == null) {
                Log.w(TAG, "No spreadsheet ID configured - sync skipped")
                return Result.success() // Don't retry, config issue
            }

            val sheetName = getSheetName()
            if (sheetName == null) {
                Log.w(TAG, "No sheet name configured - sync skipped")
                return Result.success() // Don't retry, config issue
            }

            val credentials = getApiCredentials()
            if (credentials == null) {
                Log.w(TAG, "No API credentials configured - sync skipped")
                return Result.success() // Don't retry, config issue
            }

            Log.d(TAG, "Initializing Google Sheets service")
            if (!googleSheetsService.initialize(credentials)) {
                Log.e(TAG, "Failed to initialize Google Sheets service")
                return Result.retry()
            }

            Log.d(TAG, "Ensuring header row exists")
            googleSheetsService.ensureHeaderRow(spreadsheetId, sheetName)

            // Sync all unsynced expenses
            var successCount = 0
            var failCount = 0

            for (expense in unsyncedExpenses) {
                Log.d(TAG, "Syncing expense ID: ${expense.id}")
                val success = googleSheetsService.appendExpense(spreadsheetId, sheetName, expense)

                if (success) {
                    // Mark as synced
                    expenseDao.updateExpense(expense.copy(isSynced = true))
                    successCount++
                    Log.d(TAG, "✓ Synced expense ${expense.id}")
                } else {
                    failCount++
                    Log.e(TAG, "✗ Failed to sync expense ${expense.id}")
                }
            }

            Log.d(TAG, "=== PERIODIC SYNC COMPLETED: $successCount synced, $failCount failed ===")

            // If all failed, retry
            return if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception during periodic sync: ${e.message}", e)
            return Result.retry()
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
        const val WORK_NAME = "periodic_sync_expenses"
    }
}
