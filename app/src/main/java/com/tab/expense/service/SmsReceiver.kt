package com.tab.expense.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tab.expense.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (smsMessage in messages) {
                val sender = smsMessage.originatingAddress ?: continue
                val messageBody = smsMessage.messageBody ?: continue

                Log.d(TAG, "SMS received from: $sender")
                Log.d(TAG, "Message: $messageBody")

                // Process SMS in coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    processSms(context, sender, messageBody)
                }
            }
        }
    }

    private suspend fun processSms(context: Context, sender: String, messageBody: String) {
        // Check if sender is in allowed list
        if (!isAllowedSender(context, sender)) {
            Log.d(TAG, "Sender not in allowed list: $sender")
            return
        }

        // Parse SMS
        val parsedExpense = SmsParser.parseSms(messageBody)
        if (parsedExpense != null) {
            Log.d(TAG, "Successfully parsed expense: $parsedExpense")

            // Show notification
            NotificationService.showExpenseNotification(
                context = context,
                date = parsedExpense.date,
                description = parsedExpense.description,
                amount = parsedExpense.amount,
                currency = parsedExpense.currency,
                smsBody = messageBody
            )
        } else {
            Log.d(TAG, "Could not parse expense from SMS")
        }
    }

    private suspend fun isAllowedSender(context: Context, sender: String): Boolean {
        val allowedSendersKey = stringPreferencesKey(Constants.PREF_ALLOWED_SENDERS)
        val allowedSenders = context.dataStore.data
            .map { preferences ->
                preferences[allowedSendersKey] ?: ""
            }
            .first()

        if (allowedSenders.isBlank()) {
            // If no allowed senders configured, allow all
            return true
        }

        val senderList = allowedSenders.split(",").map { it.trim() }
        return senderList.any { allowedSender ->
            sender.contains(allowedSender) || allowedSender.contains(sender)
        }
    }
}
