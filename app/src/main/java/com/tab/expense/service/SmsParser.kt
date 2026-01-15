package com.tab.expense.service

import android.util.Log
import com.tab.expense.util.CurrencyConverter
import java.text.SimpleDateFormat
import java.util.*

data class ParsedExpense(
    val date: Long,
    val description: String,
    val amount: Double,
    val currency: String = "MVR",
    val originalAmount: Double? = null,
    val originalCurrency: String? = null
)

object SmsParser {
    private const val TAG = "SmsParser"

    fun parseSms(smsBody: String): ParsedExpense? {
        Log.d(TAG, "Parsing SMS: $smsBody")

        // Pattern 0: Bank SMS format
        val bankPattern = Regex(
            """Transaction\s+from\s+\d+\s+on\s+(\d{1,2}/\d{1,2}/\d{2})\s+at\s+[\d:]+\s+for\s+(MVR|USD)([\d,]+\.?\d*)\s+at\s+(.+?)\s+was\s+processed""",
            RegexOption.IGNORE_CASE
        )
        bankPattern.find(smsBody)?.let { match ->
            val dateStr = match.groups[1]?.value
            val currency = match.groups[2]?.value ?: "MVR"
            val amountStr = match.groups[3]?.value ?: return@let null
            val merchantRaw = match.groups[4]?.value ?: return@let null

            val originalAmount = extractAmount(amountStr) ?: return@let null
            val merchant = formatMerchantName(merchantRaw)
            val date = parseBankDate(dateStr)

            val (finalAmount, finalCurrency) = if (currency.equals("USD", ignoreCase = true)) {
                Pair(CurrencyConverter.usdToMvr(originalAmount), "MVR")
            } else {
                Pair(originalAmount, "MVR")
            }

            Log.d(TAG, "Bank pattern matched: merchant=$merchant, amount=$finalAmount")
            return ParsedExpense(
                date = date,
                description = merchant,
                amount = finalAmount,
                currency = finalCurrency,
                originalAmount = originalAmount,
                originalCurrency = currency
            )
        }

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

    private fun cleanMerchantName(merchant: String): String {
        return merchant
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\w\s&'-]"""), "")
            .take(50)
    }

    private fun formatMerchantName(merchant: String): String {
        return merchant
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

    private fun parseBankDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) {
            return System.currentTimeMillis()
        }

        return try {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
            sdf.isLenient = false
            val date = sdf.parse(dateStr)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bank date: $dateStr", e)
            System.currentTimeMillis()
        }
    }

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
                continue
            }
        }

        return System.currentTimeMillis()
    }
}
