package com.tab.expense.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun SmsPermissionCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasReadSms by remember { mutableStateOf(false) }
    var hasReceiveSms by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        hasReceiveSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    val hasAccess = hasReadSms && hasReceiveSms

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAccess) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SMS Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasAccess) {
                            "Enabled - SMS expenses will be detected"
                        } else {
                            "Required for SMS expense detection"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (hasAccess) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = if (hasAccess) "Enabled" else "Disabled",
                    tint = if (hasAccess) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            if (!hasAccess) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Grant SMS permissions to automatically detect banking SMS messages and create expense entries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Permissions")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✓ Banking SMS messages will automatically create expense entries that you can review and save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NotificationPermissionCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
    }

    // Don't show card on older Android versions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Permission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasPermission) {
                            "Enabled - App can show notifications"
                        } else {
                            "Required to show expense notifications"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (hasPermission) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = if (hasPermission) "Enabled" else "Disabled",
                    tint = if (hasPermission) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Grant notification permission to receive alerts when expenses are detected from SMS or bank notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Permission")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✓ You will receive notifications when expenses are automatically detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
