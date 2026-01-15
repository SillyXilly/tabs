package com.tab.expense.data.remote

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.tab.expense.data.local.entity.Expense
import com.tab.expense.util.CurrencyConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

class GoogleSheetsService(
    private val context: Context
) {
    private val TAG = "GoogleSheetsService"
    private var sheetsService: Sheets? = null

    /**
     * Initialize Google Sheets service with credentials
     */
    suspend fun initialize(credentialsJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonInputStream = ByteArrayInputStream(credentialsJson.toByteArray())
            val credentials = GoogleCredentials
                .fromStream(jsonInputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            sheetsService = Sheets.Builder(
                httpTransport,
                jsonFactory,
                HttpCredentialsAdapter(credentials)
            )
                .setApplicationName("Tab Expense Tracker")
                .build()

            Log.d(TAG, "Google Sheets service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Sheets service", e)
            false
        }
    }

    /**
     * Test connection to Google Sheets
     */
    suspend fun testConnection(spreadsheetId: String, sheetName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val range = "$sheetName!A1:A1"
            sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            Log.d(TAG, "Connection test successful")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    /**
     * Append expense to Google Sheet
     */
    suspend fun appendExpense(
        spreadsheetId: String,
        sheetName: String,
        expense: Expense
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized")
                return@withContext false
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(expense.date))

            val values = listOf(
                listOf(
                    date,
                    expense.category,
                    expense.description,
                    expense.amountMVR,
                    expense.originalCurrency,
                    expense.originalAmount
                )
            )

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:F"

            sheetsService?.spreadsheets()?.values()
                ?.append(spreadsheetId, range, body)
                ?.setValueInputOption("USER_ENTERED")
                ?.execute()

            Log.d(TAG, "Expense appended successfully: ${expense.description}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append expense", e)
            false
        }
    }

    /**
     * Append multiple expenses to Google Sheet
     */
    suspend fun appendExpenses(
        spreadsheetId: String,
        sheetName: String,
        expenses: List<Expense>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized")
                return@withContext false
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val values = expenses.map { expense ->
                val date = dateFormat.format(Date(expense.date))
                listOf(
                    date,
                    expense.category,
                    expense.description,
                    expense.amountMVR,
                    expense.originalCurrency,
                    expense.originalAmount
                )
            }

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:F"

            sheetsService?.spreadsheets()?.values()
                ?.append(spreadsheetId, range, body)
                ?.setValueInputOption("USER_ENTERED")
                ?.execute()

            Log.d(TAG, "Appended ${expenses.size} expenses successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append expenses", e)
            false
        }
    }

    /**
     * Create header row in sheet if it doesn't exist
     */
    suspend fun ensureHeaderRow(
        spreadsheetId: String,
        sheetName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized")
                return@withContext false
            }

            // Check if first row exists
            val range = "$sheetName!A1:F1"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()

            if (response?.getValues().isNullOrEmpty()) {
                // Create header row
                val headers = listOf(
                    listOf("Date", "Category", "Description", "Amount (MVR)", "Currency", "Original Amount")
                )

                val body = ValueRange().setValues(headers)
                sheetsService?.spreadsheets()?.values()
                    ?.update(spreadsheetId, range, body)
                    ?.setValueInputOption("USER_ENTERED")
                    ?.execute()

                Log.d(TAG, "Header row created")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure header row", e)
            false
        }
    }
}
