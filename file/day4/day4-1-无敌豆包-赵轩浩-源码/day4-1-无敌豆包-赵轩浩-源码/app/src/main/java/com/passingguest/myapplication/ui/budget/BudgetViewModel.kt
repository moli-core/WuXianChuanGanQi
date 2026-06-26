package com.passingguest.myapplication.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.passingguest.myapplication.App
import com.passingguest.myapplication.data.entity.Budget
import com.passingguest.myapplication.data.entity.Category
import com.passingguest.myapplication.ui.adapter.BudgetDisplayItem
import com.passingguest.myapplication.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetRepo = (application as App).budgetRepository
    private val accountRepo = (application as App).accountRepository
    private val categoryRepo = (application as App).categoryRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val categories: LiveData<List<Category>> = categoryRepo.getAllCategories()

    private val currentYearMonth = DateUtils.getCurrentYearMonth()

    private val _overallBudgetAmount = MediatorLiveData<Double>().apply { value = 0.0 }
    private val _overallSpentAmount = MediatorLiveData<Double>().apply { value = 0.0 }

    val overallBudgetProgress = MediatorLiveData<Pair<Double, Double>>().apply {
        addSource(_overallBudgetAmount) { updateOverallProgress() }
        addSource(_overallSpentAmount) { updateOverallProgress() }
    }

    private fun MediatorLiveData<Pair<Double, Double>>.updateOverallProgress() {
        value = Pair(_overallBudgetAmount.value ?: 0.0, _overallSpentAmount.value ?: 0.0)
    }

    val budgetItems = MediatorLiveData<List<BudgetDisplayItem>>().apply {
        var budgets: List<Budget> = emptyList()
        var cats: List<Category> = emptyList()

        val refresh = {
            value = buildBudgetItems(budgets, cats)
        }

        // Observe budgets and calculate spent amounts
        addSource(budgetRepo.getBudgetsByMonth(currentYearMonth)) { budgetList ->
            budgets = budgetList
            refresh()
            calculateOverallSpent(budgetList)
        }

        addSource(categoryRepo.getAllCategories()) { categoryList ->
            cats = categoryList
            refresh()
        }
    }

    init {
        loadOverallBudget()
        loadCategoryBudgets()
    }

    private fun loadOverallBudget() {
        budgetRepo.getOverallBudget(currentYearMonth).observeForever { budgets ->
            _overallBudgetAmount.value = budgets.firstOrNull()?.amount ?: 0.0
        }
    }

    private fun loadCategoryBudgets() {
        // This is handled by the MediatorLiveData
    }

    private fun calculateOverallSpent(budgets: List<Budget>) {
        scope.launch {
            val start = DateUtils.getStartOfMonthFromString(currentYearMonth)
            val end = DateUtils.getEndOfMonthFromString(currentYearMonth)
            val spent = accountRepo.getTotalByTypeAndPeriod("EXPENSE", start, end)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _overallSpentAmount.value = spent
            }
        }
    }

    private fun buildBudgetItems(
        budgets: List<Budget>,
        categories: List<Category>
    ): List<BudgetDisplayItem> {
        val categoryMap = categories.associateBy { it.id }
        val items = mutableListOf<BudgetDisplayItem>()

        budgets.filter { it.categoryId != null }.forEach { budget ->
            val category = categoryMap[budget.categoryId]
            val spent = calculateCategorySpent(budget.categoryId!!)
            items.add(
                BudgetDisplayItem(
                    budgetId = budget.id,
                    categoryId = budget.categoryId,
                    categoryName = category?.name ?: "未知",
                    categoryIcon = (category?.name?.firstOrNull()?.toString() ?: "?"),
                    budgetAmount = budget.amount,
                    spentAmount = spent
                )
            )
        }

        return items
    }

    private fun calculateCategorySpent(categoryId: Long): Double {
        // Simplified - in production would query the DAO
        return 0.0
    }

    fun setOverallBudget(amount: Double) {
        scope.launch {
            val existingBudgets = budgetRepo.getOverallBudget(currentYearMonth)
            // Cannot easily get value from LiveData synchronously
            val budget = Budget(
                yearMonth = currentYearMonth,
                amount = amount,
                type = "EXPENSE"
            )
            budgetRepo.insert(budget)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _overallBudgetAmount.value = amount
            }
        }
    }

    fun setCategoryBudget(categoryId: Long, amount: Double) {
        scope.launch {
            val budget = Budget(
                categoryId = categoryId,
                yearMonth = currentYearMonth,
                amount = amount,
                type = "EXPENSE"
            )
            budgetRepo.insert(budget)
        }
    }
}
