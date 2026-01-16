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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

        requestPermissionsIfNeeded()

        setContent {
            TabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    handleIntentAndNavigate()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            android.util.Log.d("MainActivity", "onNewIntent called with expense data")
            setIntent(it)
            // Recreate to trigger recomposition with new intent
            recreate()
        }
    }

    @Composable
    private fun handleIntentAndNavigate() {
        TabNavHost(
            openExpenseConfirmation = intent.getBooleanExtra("open_expense_confirmation", false),
            expenseDate = intent.getLongExtra(Constants.EXTRA_EXPENSE_DATE, System.currentTimeMillis()),
            expenseDescription = intent.getStringExtra(Constants.EXTRA_EXPENSE_DESCRIPTION),
            expenseAmount = intent.getDoubleExtra(Constants.EXTRA_EXPENSE_AMOUNT, 0.0)
        )
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
