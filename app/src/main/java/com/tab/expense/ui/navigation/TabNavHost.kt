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
            navController.navigate("entry?date=${event.date}&description=${event.description}&amount=${event.amount}")
            // Clear the event after navigation
            // Note: We don't clear here to avoid re-navigation issues
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
            route = "entry?date={date}&description={description}&amount={amount}",
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
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val description = backStackEntry.arguments?.getString("description")
            val amount = backStackEntry.arguments?.getFloat("amount") ?: 0f

            ManualEntryScreen(
                navController = navController,
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
