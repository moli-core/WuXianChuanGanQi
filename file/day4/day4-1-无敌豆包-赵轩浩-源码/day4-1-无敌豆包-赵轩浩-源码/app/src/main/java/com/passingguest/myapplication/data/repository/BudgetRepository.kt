package com.passingguest.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.passingguest.myapplication.data.dao.BudgetDao
import com.passingguest.myapplication.data.dao.BudgetWithSpent
import com.passingguest.myapplication.data.entity.Budget

class BudgetRepository(private val dao: BudgetDao) {

    suspend fun insert(budget: Budget): Long = dao.insert(budget)

    suspend fun update(budget: Budget) = dao.update(budget)

    suspend fun delete(budget: Budget) = dao.delete(budget)

    fun getBudgetsByMonth(yearMonth: String): LiveData<List<Budget>> =
        dao.getBudgetsByMonth(yearMonth)

    fun getOverallBudget(yearMonth: String): LiveData<List<Budget>> =
        dao.getOverallBudget(yearMonth)

    fun getCategoryBudget(yearMonth: String, categoryId: Long): LiveData<List<Budget>> =
        dao.getCategoryBudget(yearMonth, categoryId)

    fun getBudgetsWithSpent(yearMonth: String, type: String): LiveData<List<BudgetWithSpent>> =
        dao.getBudgetsWithSpent(yearMonth, type)
}
