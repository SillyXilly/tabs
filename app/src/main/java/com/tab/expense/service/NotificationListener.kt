package com.tab.expense.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {

    companion object {
        const val BML_PACKAGE_NAME = "mv.com.bml.mib"
        const val TAG = "NotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { notification ->
            processNotification(notification)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "========================================")
        Log.d(TAG, "✓ Notification Listener Service Connected")
        Log.d(TAG, "  Listening for BML Mobile notifications")
        Log.d(TAG, "  Package: $BML_PACKAGE_NAME")
        Log.d(TAG, "========================================")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "✗ Notification Listener Service Disconnected")
    }

    private fun processNotification(sbn: StatusBarNotification) {
        // 1. Filter by package name
        if (sbn.packageName != BML_PACKAGE_NAME) {
            return
        }

        // 2. Extract notification details
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = listOf(title, text, subText, bigText)
            .filter { it.isNotEmpty() }
            .joinToString(" | ")

        Log.d(TAG, "===== BML Notification Detected =====")
        Log.d(TAG, "Package: ${sbn.packageName}")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")
        if (subText.isNotEmpty()) Log.d(TAG, "SubText: $subText")
        if (bigText.isNotEmpty()) Log.d(TAG, "BigText: $bigText")
        Log.d(TAG, "Timestamp: ${sbn.postTime}")
        Log.d(TAG, "====================================")

        // 3. Filter for "Funds Transferred" and "You have sent"
        if (!title.contains("Funds Transferred", ignoreCase = true)) {
            Log.d(TAG, "Not a funds transfer notification, skipping")
            return
        }

        if (!text.contains("You have sent", ignoreCase = true)) {
            Log.d(TAG, "Not a sent transaction (might be received), skipping")
            return
        }

        // 4. Parse the notification text
        val parsedExpense = NotificationParser.parseTransferNotification(
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        if (parsedExpense != null) {
            Log.d(TAG, "✓ Successfully parsed expense: $parsedExpense")

            // 5. Show expense notification
            NotificationService.showExpenseNotification(
                context = this,
                date = parsedExpense.date,
                description = parsedExpense.description,
                amount = parsedExpense.amount,
                currency = parsedExpense.currency,
                smsBody = text,
                originalAmount = parsedExpense.originalAmount,
                originalCurrency = parsedExpense.originalCurrency
            )
        } else {
            Log.d(TAG, "✗ Could not parse expense from notification")
            Log.d(TAG, "  This might be a new format - please report this!")
        }
    }
}
