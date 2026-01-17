package com.tab.expense.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tab.expense.ui.components.NotificationAccessCard
import com.tab.expense.ui.components.SmsPermissionCard
import com.tab.expense.ui.components.NotificationPermissionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show test result
    LaunchedEffect(uiState.testResult) {
        when (val result = uiState.testResult) {
            is TestResult.Success -> {
                snackbarHostState.showSnackbar("✅ Connected to Google Sheets successfully!")
                viewModel.clearTestResult()
            }
            is TestResult.Failure -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearTestResult()
            }
            null -> {}
        }
    }

    // Category dialogs
    if (uiState.showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.hideAddCategoryDialog() },
            onAdd = { name, icon -> viewModel.addCategory(name, icon) }
        )
    }

    uiState.showEditCategoryDialog?.let { category ->
        EditCategoryDialog(
            category = category,
            onDismiss = { viewModel.hideEditCategoryDialog() },
            onUpdate = { name, icon -> viewModel.updateCategory(category, name, icon) },
            onDelete = { viewModel.deleteCategory(category) }
        )
    }

    // Show save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Settings saved successfully")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Permissions Section
            SectionHeader("Permissions")
            Spacer(modifier = Modifier.height(15.dp))

            NotificationAccessCard()

            Spacer(modifier = Modifier.height(15.dp))

            SmsPermissionCard()

            Spacer(modifier = Modifier.height(15.dp))

            NotificationPermissionCard()

            Spacer(modifier = Modifier.height(30.dp))

            // Google Sheets Integration Section
            SectionHeader("Google Sheets Integration")
            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Spreadsheet ID",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.spreadsheetId,
                onValueChange = { viewModel.updateSpreadsheetId(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Sheet Name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.sheetName,
                onValueChange = { viewModel.updateSheetName(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Expenses") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "API Credentials (JSON)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.apiCredentials,
                onValueChange = { viewModel.updateApiCredentials(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste your service account JSON here") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isTesting
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Test Connection")
                    }
                }

                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // SMS Automation Section
            SectionHeader("SMS Automation")
            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Allowed Sender Numbers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.allowedSenders,
                onValueChange = { viewModel.updateAllowedSenders(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+960 7301000, +960 9876543") },
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text(
                        text = "Comma-separated phone numbers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Categories Section
            SectionHeader("Manage Categories")
            Spacer(modifier = Modifier.height(15.dp))

            uiState.categories.forEach { category ->
                CategoryItem(
                    category = category,
                    onClick = { viewModel.showEditCategoryDialog(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { viewModel.showAddCategoryDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Add Category")
            }

            Spacer(modifier = Modifier.height(30.dp))

            // App Preferences Section
            SectionHeader("App Preferences")
            Spacer(modifier = Modifier.height(15.dp))

            SettingsItem(
                title = "Default Currency",
                subtitle = "MVR (Maldivian Rufiyaa)"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                title = "Theme",
                subtitle = "System Default"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: com.tab.expense.data.local.entity.Category,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = category.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(15.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Emoji Icon") },
                    placeholder = { Text("🎯") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && icon.isNotBlank()) {
                        onAdd(name, icon)
                    }
                },
                enabled = name.isNotBlank() && icon.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: com.tab.expense.data.local.entity.Category,
    onDismiss: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var icon by remember { mutableStateOf(category.icon) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete \"${category.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Emoji Icon") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(15.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Category")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && icon.isNotBlank()) {
                        onUpdate(name, icon)
                    }
                },
                enabled = name.isNotBlank() && icon.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
