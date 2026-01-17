package com.tab.expense.service

import android.util.Log
import com.tab.expense.util.CurrencyConverter

object NotificationParser {
    private const val TAG = "NotificationParser"

    /**
     * Parse BML Mobile "Funds Transferred" notification
     *
     * Expected format:
     * "You have sent MVR 1.0 from 7730*2789 to Fary"
     * "You have sent USD 10.50 from 7730*2789 to John Doe"
     *
     * @param title Notification title (e.g., "Funds Transferred")
     * @param text Notification text content
     * @param timestamp Notification post time in milliseconds
     * @return ParsedExpense or null if parsing fails
     */
    fun parseTransferNotification(
        title: String,
        text: String,
        timestamp: Long
    ): ParsedExpense? {
        Log.d(TAG, "Parsing transfer notification")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")

        // Pattern: "You have sent MVR 1.0 from 7730*2789 to Fary"
        // Groups: 1=currency, 2=amount, 3=account, 4=recipient
        val transferPattern = Regex(
            """You have sent\s+(MVR|USD)\s*([\d,]+\.?\d*)\s+from\s+([\d*]+)\s+to\s+(.+)""",
            RegexOption.IGNORE_CASE
        )

        transferPattern.find(text)?.let { match ->
            val currency = match.groups[1]?.value ?: "MVR"
            val amountStr = match.groups[2]?.value ?: return@let null
            val accountNumber = match.groups[3]?.value
            val recipient = match.groups[4]?.value?.trim() ?: return@let null

            val originalAmount = extractAmount(amountStr) ?: return@let null

            // Convert USD to MVR if needed
            val (finalAmount, finalCurrency) = if (currency.equals("USD", ignoreCase = true)) {
                Pair(CurrencyConverter.usdToMvr(originalAmount), "MVR")
            } else {
                Pair(originalAmount, "MVR")
            }

            // Format recipient name
            val formattedRecipient = formatRecipientName(recipient)

            Log.d(TAG, "✓ Transfer pattern matched")
            Log.d(TAG, "  Recipient: $formattedRecipient")
            Log.d(TAG, "  Amount: $finalAmount $finalCurrency")
            Log.d(TAG, "  Original: $originalAmount $currency")
            Log.d(TAG, "  Account: $accountNumber")

            return ParsedExpense(
                date = timestamp,
                description = formattedRecipient,
                amount = finalAmount,
                currency = finalCurrency,
                originalAmount = originalAmount,
                originalCurrency = currency
            )
        }

        Log.d(TAG, "✗ Transfer pattern did not match")
        Log.d(TAG, "  Expected format: 'You have sent MVR X.XX from ACCOUNT to NAME'")
        return null
    }

    private fun extractAmount(amountStr: String): Double? {
        return try {
            amountStr.replace(",", "").trim().toDoubleOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount: $amountStr", e)
            null
        }
    }

    private fun formatRecipientName(recipient: String): String {
        return recipient
            .trim()
            .replace(Regex("""\s+"""), " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
            .take(50)
    }
}
