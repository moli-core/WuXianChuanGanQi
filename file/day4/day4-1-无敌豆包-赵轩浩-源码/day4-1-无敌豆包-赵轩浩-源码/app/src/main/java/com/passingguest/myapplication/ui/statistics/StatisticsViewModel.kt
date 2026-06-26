package com.passingguest.myapplication.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.passingguest.myapplication.App
import com.passingguest.myapplication.data.dao.CategorySummary
import com.passingguest.myapplication.data.dao.MonthlySummary
import com.passingguest.myapplication.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = (application as App).accountRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _periodType = MutableLiveData("this_month") // "this_month" or "last_month"

    private val _startDate = MutableLiveData(DateUtils.getStartOfMonth(System.currentTimeMillis()))
    private val _endDate = MutableLiveData(DateUtils.getEndOfMonth(System.currentTimeMillis()))

    val periodSummary = MediatorLiveData<MonthlySummary>().apply {
        var start: Long = _startDate.value ?: 0L
        var end: Long = _endDate.value ?: 0L
        addSource(_startDate) {
            start = it ?: return@addSource
            loadSummary(start, end)
        }
        addSource(_endDate) {
            end = it ?: return@addSource
            loadSummary(start, end)
        }
    }

    private val _incomeCategorySummary = MutableLiveData<List<CategorySummary>>()
    val incomeCategorySummary: LiveData<List<CategorySummary>> = _incomeCategorySummary

    private val _expenseCategorySummary = MutableLiveData<List<CategorySummary>>()
    val expenseCategorySummary: LiveData<List<CategorySummary>> = _expenseCategorySummary

    init {
        setPeriod("this_month")
    }

    fun setPeriod(period: String) {
        _periodType.value = period
        val now = System.currentTimeMillis()
        val (start, end) = when (period) {
            "last_month" -> {
                val lastMonth = DateUtils.getStartOfMonth(now) - 1
                DateUtils.getStartOfMonth(lastMonth) to DateUtils.getEndOfMonth(lastMonth)
            }
            else -> {
                DateUtils.getStartOfMonth(now) to DateUtils.getEndOfMonth(now)
            }
        }
        _startDate.value = start
        _endDate.value = end
        loadCategorySummaries(start, end)
    }

    fun getPeriodLabel(): String {
        return when (_periodType.value) {
            "last_month" -> DateUtils.formatYearMonthFromString(
                DateUtils.getYearMonth(DateUtils.getStartOfMonth(System.currentTimeMillis()) - 1)
            )
            else -> DateUtils.formatYearMonthFromString(DateUtils.getCurrentYearMonth())
        }
    }

    private fun loadSummary(start: Long, end: Long) {
        scope.launch {
            val summary = accountRepo.getPeriodSummary(start, end)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                periodSummary.value = summary.value
            }
        }
    }

    private fun loadCategorySummaries(start: Long, end: Long) {
        scope.launch {
            val income = accountRepo.getCategorySummary("INCOME", start, end)
            val expense = accountRepo.getCategorySummary("EXPENSE", start, end)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _incomeCategorySummary.value = income.value ?: emptyList()
                _expenseCategorySummary.value = expense.value ?: emptyList()
            }
        }
    }
}
