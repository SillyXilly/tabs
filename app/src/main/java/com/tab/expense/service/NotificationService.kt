package com.tab.expense.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tab.expense.MainActivity
import com.tab.expense.R
import com.tab.expense.TabApplication
import com.tab.expense.util.Constants
import com.tab.expense.util.CurrencyConverter

object NotificationService {

    fun showExpenseNotification(
        context: Context,
        date: Long,
        description: String,
        amount: Double,
        currency: String,
        smsBody: String,
        originalAmount: Double? = null,
        originalCurrency: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            // Use SINGLE_TOP to reuse existing activity instead of clearing task
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constants.EXTRA_EXPENSE_DATE, date)
            putExtra(Constants.EXTRA_EXPENSE_DESCRIPTION, description)
            putExtra(Constants.EXTRA_EXPENSE_AMOUNT, amount)
            putExtra(Constants.EXTRA_SMS_BODY, smsBody)
            putExtra("open_expense_confirmation", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayAmount = if (originalAmount != null && originalCurrency != null) {
            CurrencyConverter.formatAmount(originalAmount, originalCurrency)
        } else {
            CurrencyConverter.formatAmount(amount, currency)
        }

        val convertedAmount = if (originalAmount != null && originalCurrency != null && originalCurrency != currency) {
            "\n(${CurrencyConverter.formatAmount(amount, currency)} MVR)"
        } else {
            ""
        }

        val notification = NotificationCompat.Builder(context, TabApplication.EXPENSE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💳 Expense Detected")
            .setContentText("$displayAmount at $description")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Amount: $displayAmount$convertedAmount\nMerchant: $description\n\nTap to review and save")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.EXPENSE_NOTIFICATION_ID, notification)
    }
}
