package com.tab.expense.di

import android.content.Context
import com.tab.expense.data.local.database.AppDatabase
import com.tab.expense.data.local.database.CategoryDao
import com.tab.expense.data.local.database.ExpenseDao
import com.tab.expense.data.remote.GoogleSheetsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideGoogleSheetsService(@ApplicationContext context: Context): GoogleSheetsService {
        return GoogleSheetsService(context)
    }
}
