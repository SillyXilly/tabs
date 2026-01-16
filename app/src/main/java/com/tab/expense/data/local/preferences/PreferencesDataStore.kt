package com.tab.expense.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Singleton DataStore instance to prevent multiple DataStore instances
 * for the same file.
 *
 * This solves the error:
 * "There are multiple DataStores active for the same file"
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
