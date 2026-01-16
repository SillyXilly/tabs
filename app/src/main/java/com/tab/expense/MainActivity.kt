package com.tab.expense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tab.expense.ui.navigation.TabNavHost
import com.tab.expense.ui.theme.TabTheme
import com.tab.expense.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingNavEvent(
    val date: Long,
    val description: String?,
    val amount: Double
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private val _pendingNavigation = MutableStateFlow<PendingNavEvent?>(null)
        val pendingNavigation = _pendingNavigation.asStateFlow()

        /**
         * Clear the pending navigation event after it has been handled
         * This prevents re-navigation on recomposition
         */
        fun clearPendingNavigation() {
            android.util.Log.d("MainActivity", "Clearing pending navigation event")
            _pendingNavigation.value = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // All permissions requested together
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (smsGranted && receiveSmsGranted && notificationGranted) {
            // All permissions granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Log app version for debugging
        android.util.Log.d("MainActivity", "========================================")
        android.util.Log.d("MainActivity", "Tab Expense Tracker - Version 1.0.2-worker-fix")
        android.util.Log.d("MainActivity", "WorkManager Return Statement Fix Applied")
        android.util.Log.d("MainActivity", "========================================")

        requestPermissionsIfNeeded()

        // Handle initial intent on cold start
        handleIncomingIntent(intent)

        setContent {
            TabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TabNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            android.util.Log.d("MainActivity", "onNewIntent called - using StateFlow navigation")
            setIntent(it)
            // DON'T recreate()! Use StateFlow to trigger navigation instead
            handleIncomingIntent(it)
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("open_expense_confirmation", false)) {
                android.util.Log.d("MainActivity", "Emitting pending navigation event")
                _pendingNavigation.value = PendingNavEvent(
                    date = it.getLongExtra(Constants.EXTRA_EXPENSE_DATE, System.currentTimeMillis()),
                    description = it.getStringExtra(Constants.EXTRA_EXPENSE_DESCRIPTION),
                    amount = it.getDoubleExtra(Constants.EXTRA_EXPENSE_AMOUNT, 0.0)
                )
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        // Check SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request all permissions together
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
