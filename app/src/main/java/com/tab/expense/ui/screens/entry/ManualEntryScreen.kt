package com.tab.expense.ui.screens.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tab.expense.ui.components.CurrencyToggle
import com.tab.expense.util.Constants
import com.tab.expense.util.CurrencyConverter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    navController: NavController,
    viewModel: EntryViewModel = hiltViewModel(),
    expenseId: Long? = null,
    prefilledDate: Long? = null,
    prefilledDescription: String? = null,
    prefilledAmount: Double? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load existing expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        }
    }

    // Pre-fill data from SMS notification (only if not editing)
    LaunchedEffect(prefilledDate, prefilledDescription, prefilledAmount) {
        if (expenseId == null && (prefilledDate != null || prefilledDescription != null || prefilledAmount != null)) {
            viewModel.prefillData(
                date = prefilledDate,
                description = prefilledDescription,
                amount = prefilledAmount
            )
        }
    }

    // Navigate back when saved or deleted
    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            navController.popBackStack()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteExpense() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.expenseId != null) "Edit Expense" else "Add Expense",
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

            // Date Field
            Text(
                text = "Date",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formatDate(uiState.date),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                ),
                enabled = false
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Dropdown
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            CategoryDropdown(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What did you buy?") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Currency Toggle
            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            CurrencyToggle(
                selectedCurrency = uiState.selectedCurrency,
                onCurrencySelected = { viewModel.selectCurrency(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 USD amounts will be converted to MVR (×${Constants.USD_TO_MVR_RATE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Field
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                prefix = {
                    Text(
                        text = if (uiState.selectedCurrency == Constants.CURRENCY_USD) "$" else "MVR ",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            // Show conversion preview
            if (uiState.selectedCurrency == Constants.CURRENCY_USD && uiState.amount.isNotBlank()) {
                uiState.amount.toDoubleOrNull()?.let { amount ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "≈ ${CurrencyConverter.formatAmount(CurrencyConverter.usdToMvr(amount))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveExpense() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (uiState.expenseId != null) "Save Changes" else "Add Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Delete Button (only show when editing)
            if (uiState.expenseId != null) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.showDeleteDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Delete Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.tab.expense.data.local.entity.Category>,
    selectedCategory: com.tab.expense.data.local.entity.Category?,
    onCategorySelected: (com.tab.expense.data.local.entity.Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "Select category",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
