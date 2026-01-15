package com.tab.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun TabNavHost(
    openExpenseConfirmation: Boolean = false,
    expenseDate: Long = 0L,
    expenseDescription: String? = null,
    expenseAmount: Double = 0.0
) {
    val navController = rememberNavController()

    LaunchedEffect(openExpenseConfirmation) {
        if (openExpenseConfirmation) {
            // Navigate to expense confirmation screen
            navController.navigate("entry?date=$expenseDate&description=$expenseDescription&amount=$expenseAmount")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "summary"
    ) {
        composable("summary") {
            // SummaryScreen(navController)
            PlaceholderScreen("Summary Screen - Coming Soon")
        }

        composable(
            route = "entry?date={date}&description={description}&amount={amount}",
            arguments = listOf(
                navArgument("date") { type = NavType.LongType; defaultValue = 0L },
                navArgument("description") { type = NavType.StringType; nullable = true },
                navArgument("amount") { type = NavType.FloatType; defaultValue = 0f }
            )
        ) {
            // ManualEntryScreen(navController, ...)
            PlaceholderScreen("Entry Screen - Coming Soon")
        }

        composable("settings") {
            // SettingsScreen(navController)
            PlaceholderScreen("Settings Screen - Coming Soon")
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}
