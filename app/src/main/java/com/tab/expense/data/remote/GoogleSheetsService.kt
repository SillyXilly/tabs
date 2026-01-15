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
            Log.d(TAG, "Initializing Google Sheets service...")
            Log.d(TAG, "Credentials length: ${credentialsJson.length}")

            // Clean up the JSON string - remove any extra whitespace and ensure proper formatting
            val cleanedJson = credentialsJson.trim()
            Log.d(TAG, "First 50 chars: ${cleanedJson.take(50)}")
            Log.d(TAG, "Last 50 chars: ${cleanedJson.takeLast(50)}")

            val jsonInputStream = ByteArrayInputStream(cleanedJson.toByteArray(Charsets.UTF_8))
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
            Log.e(TAG, "Failed to initialize Google Sheets service: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error details: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Test connection to Google Sheets
     */
    suspend fun testConnection(spreadsheetId: String, sheetName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing connection...")
            Log.d(TAG, "Spreadsheet ID: $spreadsheetId")
            Log.d(TAG, "Sheet name: $sheetName")

            if (sheetsService == null) {
                Log.e(TAG, "Sheets service is null!")
                return@withContext false
            }

            val range = "$sheetName!A1:A1"
            Log.d(TAG, "Attempting to read range: $range")

            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            Log.d(TAG, "Connection test successful. Response: ${response?.getValues()?.size ?: 0} rows")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error details: ${e.localizedMessage}")
            if (e.message?.contains("404") == true) {
                Log.e(TAG, "Spreadsheet not found or service account doesn't have access")
            } else if (e.message?.contains("403") == true) {
                Log.e(TAG, "Permission denied - service account needs Editor access")
            }
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
                    expense.amountMVR
                )
            )

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:D"

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
                    expense.amountMVR
                )
            }

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:D"

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
     * Fetch recent expenses from Google Sheet
     */
    suspend fun fetchRecentExpenses(
        spreadsheetId: String,
        sheetName: String,
        limit: Int = 30
    ): List<Expense> = withContext(Dispatchers.IO) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized")
                return@withContext emptyList()
            }

            // Fetch data (skip header row, get last N rows)
            val range = "$sheetName!A2:D"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            val values = response?.getValues() ?: return@withContext emptyList()

            Log.d(TAG, "Fetched ${values.size} rows from sheet")

            // Take last N rows and convert to Expense objects
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expenses = values.takeLast(limit).mapNotNull { row ->
                try {
                    if (row.size >= 4) {
                        val dateStr = row[0].toString()
                        val category = row[1].toString()
                        val description = row[2].toString()
                        val amount = row[3].toString().toDoubleOrNull() ?: 0.0

                        val date = try {
                            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        Expense(
                            id = 0, // Will be auto-generated by Room
                            date = date,
                            category = category,
                            description = description,
                            amountMVR = amount,
                            originalCurrency = "MVR",
                            originalAmount = amount,
                            isSynced = true // Mark as synced since it came from sheets
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse row: ${row.joinToString()}", e)
                    null
                }
            }.reversed() // Reverse to show newest first

            Log.d(TAG, "Successfully parsed ${expenses.size} expenses")
            expenses
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expenses", e)
            emptyList()
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
            val range = "$sheetName!A1:D1"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()

            if (response?.getValues().isNullOrEmpty()) {
                // Create header row
                val headers = listOf(
                    listOf("Date", "Category", "Description", "Amount")
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
