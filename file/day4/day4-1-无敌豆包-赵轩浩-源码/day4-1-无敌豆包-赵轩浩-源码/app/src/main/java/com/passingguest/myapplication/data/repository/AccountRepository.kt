package com.passingguest.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.passingguest.myapplication.data.dao.AccountRecordDao
import com.passingguest.myapplication.data.dao.CategorySummary
import com.passingguest.myapplication.data.dao.DateGroupSummary
import com.passingguest.myapplication.data.dao.MonthlySummary
import com.passingguest.myapplication.data.entity.AccountRecord

class AccountRepository(private val dao: AccountRecordDao) {

    suspend fun insert(record: AccountRecord): Long = dao.insert(record)

    suspend fun update(record: AccountRecord) = dao.update(record)

    suspend fun delete(record: AccountRecord) = dao.delete(record)

    fun getAllRecords(): LiveData<List<AccountRecord>> = dao.getAllRecords()

    suspend fun getRecordById(id: Long): AccountRecord? = dao.getRecordById(id)

    fun getRecordsByDateRange(startDate: Long, endDate: Long): LiveData<List<AccountRecord>> =
        dao.getRecordsByDateRange(startDate, endDate)

    fun getDateGroupSummary(startDate: Long, endDate: Long): LiveData<List<DateGroupSummary>> =
        dao.getDateGroupSummary(startDate, endDate)

    fun getCategorySummary(type: String, startDate: Long, endDate: Long): LiveData<List<CategorySummary>> =
        dao.getCategorySummary(type, startDate, endDate)

    fun getPeriodSummary(startDate: Long, endDate: Long): LiveData<MonthlySummary> =
        dao.getPeriodSummary(startDate, endDate)

    fun searchRecords(
        keyword: String? = null,
        type: String? = null,
        categoryId: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): LiveData<List<AccountRecord>> =
        dao.searchRecords(keyword, type, categoryId, minAmount, maxAmount, startDate, endDate)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun getTotalByTypeAndPeriod(type: String, startDate: Long, endDate: Long): Double =
        dao.getTotalByTypeAndPeriod(type, startDate, endDate)
}
