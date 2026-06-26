package com.passingguest.myapplication.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.passingguest.myapplication.data.dao.AccountRecordDao
import com.passingguest.myapplication.data.dao.BudgetDao
import com.passingguest.myapplication.data.dao.CategoryDao
import com.passingguest.myapplication.data.entity.AccountRecord
import com.passingguest.myapplication.data.entity.Budget
import com.passingguest.myapplication.data.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [AccountRecord::class, Category::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountRecordDao(): AccountRecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "account_book.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDefaultCategories(database.categoryDao())
                            }
                        }
                    }
                })
                .build()
        }

        private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val categories = mutableListOf<Category>()
            var sortOrder = 0

            // Expense categories
            Category.DEFAULT_EXPENSE_CATEGORIES.forEach { name ->
                categories.add(
                    Category(
                        name = name,
                        type = Category.TYPE_EXPENSE,
                        sortOrder = sortOrder++
                    )
                )
            }

            // Income categories
            Category.DEFAULT_INCOME_CATEGORIES.forEach { name ->
                categories.add(
                    Category(
                        name = name,
                        type = Category.TYPE_INCOME,
                        sortOrder = sortOrder++
                    )
                )
            }

            categoryDao.insertAll(categories)
        }
    }
}
