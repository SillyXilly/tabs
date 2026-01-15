package com.tab.expense.service

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class ParsedExpense(
    val date: Long,
    val description: String,
    val amount: Double,
    val currency: String = "MVR"
)

object SmsParser {
    private const val TAG = "SmsParser"

    /**
     * Parse SMS message to extract expense information
     */
    fun parseSms(smsBody: String): ParsedExpense? {
        Log.d(TAG, "Parsing SMS: $smsBody")

        // Pattern 1: "You spent Rs.1,250.00 at MERCHANT_NAME on 15-Jan-2026"
        val pattern1 = Regex(
            """(?:spent|paid|purchase[d]?)\s+(?:Rs\.?|MVR|MRF)\s*([\d,]+\.?\d*)\s+(?:at|to|for)\s+([A-Za-z0-9\s&'-]+?)(?:\s+on\s+(\d{1,2}[-/]\w{3}[-/]\d{2,4}))?""",
            RegexOption.IGNORE_CASE
        )
        pattern1.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            val dateStr = match.groups[3]?.value
            val date = parseDate(dateStr)
            return ParsedExpense(date, merchant, amount, "MVR")
        }

        // Pattern 2: "Debit of USD 45.50 from your account for SHOP_NAME"
        val pattern2 = Regex(
            """(?:debit|debited|withdrawn?)\s+(?:of\s+)?(?:USD|US\$|\$)\s*([\d,]+\.?\d*)\s+(?:from|for|at)\s+(?:your\s+account\s+)?(?:for\s+)?([A-Za-z0-9\s&'-]+)""",
            RegexOption.IGNORE_CASE
        )
        pattern2.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            return ParsedExpense(System.currentTimeMillis(), merchant, amount, "USD")
        }

        // Pattern 3: "MVR 1250.00 debited for MERCHANT on 15/01/2026"
        val pattern3 = Regex(
            """(?:MVR|Rs\.?)\s*([\d,]+\.?\d*)\s+(?:debited|deducted|charged)\s+(?:for|at|to)\s+([A-Za-z0-9\s&'-]+?)(?:\s+on\s+(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}))?""",
            RegexOption.IGNORE_CASE
        )
        pattern3.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            val dateStr = match.groups[3]?.value
            val date = parseDate(dateStr)
            return ParsedExpense(date, merchant, amount, "MVR")
        }

        // Pattern 4: "Transaction of Rs 500 at MERCHANT_NAME"
        val pattern4 = Regex(
            """(?:transaction|payment)\s+of\s+(?:Rs\.?|MVR)\s*([\d,]+\.?\d*)\s+(?:at|to|for)\s+([A-Za-z0-9\s&'-]+)""",
            RegexOption.IGNORE_CASE
        )
        pattern4.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            return ParsedExpense(System.currentTimeMillis(), merchant, amount, "MVR")
        }

        // Pattern 5: "Your card ending 1234 was used for Rs.750.00 at SHOP"
        val pattern5 = Regex(
            """(?:card|account).*?(?:used|charged)\s+(?:for\s+)?(?:Rs\.?|MVR)\s*([\d,]+\.?\d*)\s+(?:at|to|for)\s+([A-Za-z0-9\s&'-]+)""",
            RegexOption.IGNORE_CASE
        )
        pattern5.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            return ParsedExpense(System.currentTimeMillis(), merchant, amount, "MVR")
        }

        // Pattern 6: Generic amount and merchant pattern
        val pattern6 = Regex(
            """(?:Rs\.?|MVR|MRF)\s*([\d,]+\.?\d*)\s+[^\d]*([A-Za-z0-9\s&'-]{3,30})""",
            RegexOption.IGNORE_CASE
        )
        pattern6.find(smsBody)?.let { match ->
            val amount = extractAmount(match.groups[1]?.value ?: return@let null) ?: return@let null
            val merchant = cleanMerchantName(match.groups[2]?.value ?: return@let null)
            return ParsedExpense(System.currentTimeMillis(), merchant, amount, "MVR")
        }

        Log.d(TAG, "No pattern matched for SMS")
        return null
    }

    /**
     * Extract amount from text (removes commas and parses)
     */
    private fun extractAmount(amountStr: String): Double? {
        return try {
            amountStr.replace(",", "").trim().toDoubleOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount: $amountStr", e)
            null
        }
    }

    /**
     * Extract and clean merchant name
     */
    private fun cleanMerchantName(merchant: String): String {
        return merchant
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\w\s&'-]"""), "")
            .take(50)
    }

    /**
     * Parse date from various formats
     */
    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) {
            return System.currentTimeMillis()
        }

        val formats = listOf(
            "dd-MMM-yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd/MM/yy",
            "dd-MM-yy"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Try next format
            }
        }

        // Default to current time if parsing fails
        return System.currentTimeMillis()
    }
}
