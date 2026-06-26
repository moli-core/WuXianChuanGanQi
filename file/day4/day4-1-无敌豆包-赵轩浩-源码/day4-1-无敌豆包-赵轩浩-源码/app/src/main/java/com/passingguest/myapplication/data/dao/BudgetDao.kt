package com.passingguest.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.passingguest.myapplication.data.entity.Budget

data class BudgetWithSpent(
    val budgetId: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val yearMonth: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val type: String
)

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun getBudgetsByMonth(yearMonth: String): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryId IS NULL")
    fun getOverallBudget(yearMonth: String): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    fun getCategoryBudget(yearMonth: String, categoryId: Long): LiveData<List<Budget>>

    @Query("""
        SELECT
            b.id as budgetId,
            b.categoryId as categoryId,
            c.name as categoryName,
            b.yearMonth as yearMonth,
            b.amount as budgetAmount,
            COALESCE(SUM(CASE WHEN r.type = b.type THEN r.amount ELSE 0 END), 0) as spentAmount,
            b.type as type
        FROM budgets b
        LEFT JOIN categories c ON b.categoryId = c.id
        LEFT JOIN account_records r ON (b.categoryId IS NULL OR r.categoryId = b.categoryId)
            AND strftime('%Y-%m', r.date / 1000, 'unixepoch') = b.yearMonth
        WHERE b.yearMonth = :yearMonth AND b.type = :type
        GROUP BY b.id
    """)
    fun getBudgetsWithSpent(yearMonth: String, type: String): LiveData<List<BudgetWithSpent>>
}
