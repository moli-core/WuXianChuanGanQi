package com.passingguest.myapplication

import android.app.Application
import com.passingguest.myapplication.data.db.AppDatabase
import com.passingguest.myapplication.data.repository.AccountRepository
import com.passingguest.myapplication.data.repository.BudgetRepository
import com.passingguest.myapplication.data.repository.CategoryRepository

class App : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var accountRepository: AccountRepository
        private set

    lateinit var categoryRepository: CategoryRepository
        private set

    lateinit var budgetRepository: BudgetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        accountRepository = AccountRepository(database.accountRecordDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        budgetRepository = BudgetRepository(database.budgetDao())
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
