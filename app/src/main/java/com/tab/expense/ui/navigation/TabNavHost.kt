package com.tab.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tab.expense.ui.screens.entry.ManualEntryScreen
import com.tab.expense.ui.screens.settings.SettingsScreen
import com.tab.expense.ui.screens.summary.SummaryScreen

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
            // Navigate to expense confirmation screen with pre-filled data
            navController.navigate("entry?date=$expenseDate&description=$expenseDescription&amount=$expenseAmount")
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
        ) {
            // TODO: Handle pre-filled data from SMS notification
            ManualEntryScreen(navController = navController)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
