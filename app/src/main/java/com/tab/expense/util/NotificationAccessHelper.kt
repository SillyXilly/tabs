package com.tab.expense.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object NotificationAccessHelper {
    private const val TAG = "NotificationAccessHelper"

    /**
     * Check if notification listener permission is granted
     *
     * @param context Application context
     * @return true if notification access is granted, false otherwise
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        if (flat.isNullOrEmpty()) {
            return false
        }

        val names = flat.split(":").toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null) {
                if (packageName == cn.packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Open notification access settings page
     *
     * @param context Application context
     */
    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened notification access settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification access settings", e)
        }
    }
}
