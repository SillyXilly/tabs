package com.tab.expense.util

object Constants {
    // Currency
    const val CURRENCY_MVR = "MVR"
    const val CURRENCY_USD = "USD"
    const val USD_TO_MVR_RATE = 15.42

    // Default values
    const val DEFAULT_CURRENCY = CURRENCY_MVR

    // Preferences keys
    const val PREF_SPREADSHEET_ID = "spreadsheet_id"
    const val PREF_SHEET_NAME = "sheet_name"
    const val PREF_API_CREDENTIALS = "api_credentials"
    const val PREF_ALLOWED_SENDERS = "allowed_senders"
    const val PREF_DEFAULT_CURRENCY = "default_currency"

    // Notification
    const val NOTIFICATION_REQUEST_CODE = 1001
    const val EXPENSE_NOTIFICATION_ID = 2001

    // Intent extras
    const val EXTRA_EXPENSE_DATE = "extra_expense_date"
    const val EXTRA_EXPENSE_DESCRIPTION = "extra_expense_description"
    const val EXTRA_EXPENSE_AMOUNT = "extra_expense_amount"
    const val EXTRA_SMS_BODY = "extra_sms_body"

    // Categories
    val DEFAULT_CATEGORIES = listOf(
        "food",
        "transport",
        "shopping",
        "health",
        "entertainment",
        "bills",
        "other"
    )

    // Date formats
    const val DATE_FORMAT_DISPLAY = "MMMM dd, yyyy"
    const val DATE_FORMAT_SHORT = "MMM dd"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
}
