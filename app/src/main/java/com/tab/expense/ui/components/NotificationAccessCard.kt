package com.tab.expense.ui.components

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
import com.tab.expense.util.NotificationAccessHelper

@Composable
fun NotificationAccessCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(false) }

    // Check permission status on composition and when returning from settings
    LaunchedEffect(Unit) {
        hasAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
    }

    // Recheck when the composable becomes visible again
    DisposableEffect(Unit) {
        onDispose {
            // Recheck when coming back
            hasAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
        }
    }

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
                        text = "Notification Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasAccess) {
                            "Enabled - Bank transfers will be detected"
                        } else {
                            "Required for bank transfer detection"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status indicator
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
                    text = "Grant notification access to automatically detect bank transfers from BML Mobile app and create expense entries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        NotificationAccessHelper.openNotificationAccessSettings(context)
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
                    Text("Grant Access")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tip: After granting access, return to this screen to verify the status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Light
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✓ Your BML Mobile transfer notifications will automatically create expense entries that you can review and save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
