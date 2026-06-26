package com.passingguest.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.passingguest.myapplication.data.entity.AccountRecord

data class DateGroupSummary(
    val date: Long,
    val totalIncome: Double,
    val totalExpense: Double
)

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val total: Double
)

data class MonthlySummary(
    val yearMonth: String,
    val totalIncome: Double,
    val totalExpense: Double
)

@Dao
interface AccountRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AccountRecord): Long

    @Update
    suspend fun update(record: AccountRecord)

    @Delete
    suspend fun delete(record: AccountRecord)

    @Query("SELECT * FROM account_records ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<AccountRecord>>

    @Query("SELECT * FROM account_records WHERE id = :id")
    suspend fun getRecordById(id: Long): AccountRecord?

    @Query("""
        SELECT * FROM account_records
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getRecordsByDateRange(startDate: Long, endDate: Long): LiveData<List<AccountRecord>>

    @Query("""
        SELECT date,
               SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM account_records
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date DESC
    """)
    fun getDateGroupSummary(startDate: Long, endDate: Long): LiveData<List<DateGroupSummary>>

    @Query("""
        SELECT categoryId, c.name as categoryName, SUM(r.amount) as total
        FROM account_records r
        JOIN categories c ON r.categoryId = c.id
        WHERE r.type = :type AND r.date BETWEEN :startDate AND :endDate
        GROUP BY r.categoryId
        ORDER BY total DESC
    """)
    fun getCategorySummary(type: String, startDate: Long, endDate: Long): LiveData<List<CategorySummary>>

    @Query("""
        SELECT '' as yearMonth,
               SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM account_records
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun getPeriodSummary(startDate: Long, endDate: Long): LiveData<MonthlySummary>

    @Query("""
        SELECT * FROM account_records
        WHERE (:keyword IS NULL OR note LIKE '%' || :keyword || '%')
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC
    """)
    fun searchRecords(
        keyword: String? = null,
        type: String? = null,
        categoryId: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): LiveData<List<AccountRecord>>

    @Query("DELETE FROM account_records")
    suspend fun deleteAll()

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM account_records
        WHERE date BETWEEN :startDate AND :endDate AND type = :type
    """)
    suspend fun getTotalByTypeAndPeriod(type: String, startDate: Long, endDate: Long): Double
}
