package com.tab.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tab.expense.MainActivity
import com.tab.expense.ui.screens.entry.ManualEntryScreen
import com.tab.expense.ui.screens.settings.SettingsScreen
import com.tab.expense.ui.screens.summary.SummaryScreen

@Composable
fun TabNavHost() {
    val navController = rememberNavController()

    // Observe pending navigation events from MainActivity
    val pendingNav by MainActivity.pendingNavigation.collectAsState()

    LaunchedEffect(pendingNav) {
        pendingNav?.let { event ->
            android.util.Log.d("TabNavHost", "Navigating to entry with prefilled data")
            // Navigate to entry screen with prefilled data
            navController.navigate("entry?date=${event.date}&description=${event.description}&amount=${event.amount}") {
                // Clear any existing entry screens to avoid stack buildup
                launchSingleTop = true
            }
            // CRITICAL: Clear the event after navigation to prevent re-navigation on recomposition
            MainActivity.clearPendingNavigation()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "summary"
    ) {
        composable("summary") {
            SummaryScreen(navController = navController)
        }

        composable(
            route = "entry?date={date}&description={description}&amount={amount}&expenseId={expenseId}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("description") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("amount") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("expenseId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val description = backStackEntry.arguments?.getString("description")
            val amount = backStackEntry.arguments?.getFloat("amount") ?: 0f
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L

            ManualEntryScreen(
                navController = navController,
                expenseId = if (expenseId > 0) expenseId else null,
                prefilledDate = if (date > 0) date else null,
                prefilledDescription = description,
                prefilledAmount = if (amount > 0) amount.toDouble() else null
            )
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
