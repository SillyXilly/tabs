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
     * Always re-creates the service to ensure fresh state
     */
    suspend fun initialize(credentialsJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Google Sheets service...")
            Log.d(TAG, "Credentials length: ${credentialsJson.length}")

            // Clean up the JSON string - remove any extra whitespace and ensure proper formatting
            val cleanedJson = credentialsJson.trim()

            val jsonInputStream = ByteArrayInputStream(cleanedJson.toByteArray(Charsets.UTF_8))
            val credentials = GoogleCredentials
                .fromStream(jsonInputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            // Always create a fresh service instance
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
            sheetsService = null
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
     * Append expense to Google Sheet (with ID support)
     */
    suspend fun appendExpense(
        spreadsheetId: String,
        sheetName: String,
        expense: Expense
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== APPEND EXPENSE TO SHEETS START ===")
            Log.d(TAG, "Expense ID: ${expense.id}, desc: ${expense.description}")

            if (sheetsService == null) {
                Log.e(TAG, "✗ Sheets service not initialized")
                return@withContext false
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(expense.date))

            // Include ID as first column
            val values = listOf(
                listOf(
                    expense.id.toString(),  // Column A: ID
                    date,                    // Column B: Date
                    expense.category,        // Column C: Category
                    expense.description,     // Column D: Description
                    expense.amountMVR        // Column E: Amount
                )
            )

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:E"  // Updated range to include ID column

            sheetsService?.spreadsheets()?.values()
                ?.append(spreadsheetId, range, body)
                ?.setValueInputOption("USER_ENTERED")
                ?.execute()

            Log.d(TAG, "✓ Expense appended successfully: ID=${expense.id}, ${expense.description}")
            Log.d(TAG, "=== APPEND EXPENSE TO SHEETS END ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to append expense: ${e.message}", e)
            Log.d(TAG, "=== APPEND EXPENSE TO SHEETS END (ERROR) ===")
            false
        }
    }

    /**
     * Append multiple expenses to Google Sheet (with ID support)
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
                    expense.id.toString(),  // Column A: ID
                    date,                    // Column B: Date
                    expense.category,        // Column C: Category
                    expense.description,     // Column D: Description
                    expense.amountMVR        // Column E: Amount
                )
            }

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A:E"  // Updated range to include ID column

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
     * Update existing expense in Google Sheet by ID
     * Finds the row with matching ID and updates it
     */
    suspend fun updateExpense(
        spreadsheetId: String,
        sheetName: String,
        expense: Expense
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== UPDATE EXPENSE IN SHEETS START ===")
            Log.d(TAG, "Looking for expense ID: ${expense.id}")

            if (sheetsService == null) {
                Log.e(TAG, "✗ Sheets service not initialized")
                return@withContext false
            }

            // Find the row with this ID
            val rowIndex = findRowByExpenseId(spreadsheetId, sheetName, expense.id)
            if (rowIndex == -1) {
                Log.w(TAG, "✗ Expense ID ${expense.id} not found in sheet, will append instead")
                // If not found, append it as new
                return@withContext appendExpense(spreadsheetId, sheetName, expense)
            }

            Log.d(TAG, "✓ Found expense at row $rowIndex, updating...")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(expense.date))

            val values = listOf(
                listOf(
                    expense.id.toString(),
                    date,
                    expense.category,
                    expense.description,
                    expense.amountMVR
                )
            )

            val body = ValueRange().setValues(values)
            val range = "$sheetName!A$rowIndex:E$rowIndex"

            sheetsService?.spreadsheets()?.values()
                ?.update(spreadsheetId, range, body)
                ?.setValueInputOption("USER_ENTERED")
                ?.execute()

            Log.d(TAG, "✓ Expense updated successfully at row $rowIndex")
            Log.d(TAG, "=== UPDATE EXPENSE IN SHEETS END ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to update expense: ${e.message}", e)
            Log.d(TAG, "=== UPDATE EXPENSE IN SHEETS END (ERROR) ===")
            false
        }
    }

    /**
     * Delete expense from Google Sheet by ID
     * Finds the row with matching ID and deletes it
     */
    suspend fun deleteExpense(
        spreadsheetId: String,
        sheetName: String,
        expenseId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DELETE EXPENSE FROM SHEETS START ===")
            Log.d(TAG, "Looking for expense ID: $expenseId")

            if (sheetsService == null) {
                Log.e(TAG, "✗ Sheets service not initialized")
                return@withContext false
            }

            // Find the row with this ID
            val rowIndex = findRowByExpenseId(spreadsheetId, sheetName, expenseId)
            if (rowIndex == -1) {
                Log.w(TAG, "✗ Expense ID $expenseId not found in sheet")
                return@withContext false
            }

            Log.d(TAG, "✓ Found expense at row $rowIndex, deleting...")

            // Get sheet ID first
            val sheetId = getSheetId(spreadsheetId, sheetName)
            if (sheetId == null) {
                Log.e(TAG, "✗ Could not find sheet ID for sheet: $sheetName")
                return@withContext false
            }

            // Delete the row using batchUpdate
            val requests = listOf(
                mapOf(
                    "deleteDimension" to mapOf(
                        "range" to mapOf(
                            "sheetId" to sheetId,
                            "dimension" to "ROWS",
                            "startIndex" to (rowIndex - 1),  // 0-indexed
                            "endIndex" to rowIndex           // exclusive
                        )
                    )
                )
            )

            val requestBody = com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                .setRequests(requests.map {
                    com.google.api.services.sheets.v4.model.Request().also { req -> req.setUnknownKeys(it) }
                })

            sheetsService?.spreadsheets()?.batchUpdate(spreadsheetId, requestBody)?.execute()

            Log.d(TAG, "✓ Expense deleted successfully from row $rowIndex")
            Log.d(TAG, "=== DELETE EXPENSE FROM SHEETS END ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to delete expense: ${e.message}", e)
            Log.d(TAG, "=== DELETE EXPENSE FROM SHEETS END (ERROR) ===")
            false
        }
    }

    /**
     * Find row index by expense ID
     * Returns -1 if not found
     */
    private suspend fun findRowByExpenseId(
        spreadsheetId: String,
        sheetName: String,
        expenseId: Long
    ): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching for expense ID: $expenseId")

            // Get all IDs from column A
            val range = "$sheetName!A:A"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            val values = response?.getValues() ?: return@withContext -1

            // Find row with matching ID (skip header row)
            values.drop(1).forEachIndexed { index, row ->
                if (row.isNotEmpty()) {
                    val id = row[0].toString().toLongOrNull()
                    if (id == expenseId) {
                        val rowIndex = index + 2  // +1 for 0-index, +1 for header row
                        Log.d(TAG, "✓ Found ID $expenseId at row $rowIndex")
                        return@withContext rowIndex
                    }
                }
            }

            Log.d(TAG, "✗ ID $expenseId not found in sheet")
            -1
        } catch (e: Exception) {
            Log.e(TAG, "Error finding expense ID: ${e.message}", e)
            -1
        }
    }

    /**
     * Get sheet ID by sheet name (needed for delete operations)
     */
    private suspend fun getSheetId(
        spreadsheetId: String,
        sheetName: String
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService?.spreadsheets()?.get(spreadsheetId)?.execute()
            val sheet = spreadsheet?.sheets?.find { it.properties.title == sheetName }
            sheet?.properties?.sheetId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sheet ID: ${e.message}", e)
            null
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

            // Fetch data (skip header row, get all columns including ID)
            val range = "$sheetName!A2:E"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            val values = response?.getValues() ?: return@withContext emptyList()

            Log.d(TAG, "Fetched ${values.size} rows from sheet")

            // Take last N rows and convert to Expense objects
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expenses = values.takeLast(limit).mapNotNull { row ->
                try {
                    if (row.size >= 5) {
                        val idStr = row[0].toString()
                        val dateStr = row[1].toString()
                        val category = row[2].toString()
                        val description = row[3].toString()
                        val amount = row[4].toString().toDoubleOrNull() ?: 0.0

                        val id = idStr.toLongOrNull() ?: 0L
                        val date = try {
                            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        Expense(
                            id = id, // Use ID from sheet
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
     * Create header row in sheet if it doesn't exist (with ID column)
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
            val range = "$sheetName!A1:E1"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()

            if (response?.getValues().isNullOrEmpty()) {
                // Create header row with ID column
                val headers = listOf(
                    listOf("ID", "Date", "Category", "Description", "Amount")
                )

                val body = ValueRange().setValues(headers)
                sheetsService?.spreadsheets()?.values()
                    ?.update(spreadsheetId, range, body)
                    ?.setValueInputOption("USER_ENTERED")
                    ?.execute()

                Log.d(TAG, "Header row created with ID column")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure header row", e)
            false
        }
    }
}
