package com.tab.expense.util

object CurrencyConverter {
    /**
     * Converts USD to MVR using the fixed rate
     * @param amount The amount in USD
     * @return The amount in MVR
     */
    fun usdToMvr(amount: Double): Double {
        return amount * Constants.USD_TO_MVR_RATE
    }

    /**
     * Converts MVR to USD using the fixed rate
     * @param amount The amount in MVR
     * @return The amount in USD
     */
    fun mvrToUsd(amount: Double): Double {
        return amount / Constants.USD_TO_MVR_RATE
    }

    /**
     * Formats amount with currency symbol
     * @param amount The amount to format
     * @param currency The currency (MVR or USD)
     * @return Formatted string with currency
     */
    fun formatAmount(amount: Double, currency: String = Constants.CURRENCY_MVR): String {
        return when (currency) {
            Constants.CURRENCY_USD -> "$%.2f".format(amount)
            else -> "MVR %.2f".format(amount)
        }
    }

    /**
     * Formats amount to 2 decimal places
     */
    fun formatAmountOnly(amount: Double): String {
        return "%.2f".format(amount)
    }
}
