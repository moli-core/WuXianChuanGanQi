package com.passingguest.myapplication.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.passingguest.myapplication.App
import com.passingguest.myapplication.data.dao.DateGroupSummary
import com.passingguest.myapplication.data.dao.MonthlySummary
import com.passingguest.myapplication.data.entity.AccountRecord
import com.passingguest.myapplication.data.entity.Category
import com.passingguest.myapplication.ui.adapter.DisplayItem
import com.passingguest.myapplication.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = (application as App).accountRepository
    private val categoryRepo = (application as App).categoryRepository

    private val currentMonthStart = DateUtils.getStartOfMonth(System.currentTimeMillis())
    private val currentMonthEnd = DateUtils.getEndOfMonth(System.currentTimeMillis())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _startDate = MutableLiveData(currentMonthStart)
    private val _endDate = MutableLiveData(currentMonthEnd)

    val periodSummary: LiveData<MonthlySummary> = _startDate.let { start ->
        _endDate.let { end ->
            val mediator = MediatorLiveData<MonthlySummary>()
            mediator.addSource(start) {
                mediator.value = null
                loadSummary()
            }
            mediator.addSource(end) {
                mediator.value = null
                loadSummary()
            }
            mediator
        }
    }

    // Room LiveData 源 — 响应式监听数据库变化
    private var recordsSource: LiveData<List<AccountRecord>>? = null
    private var summarySource: LiveData<List<DateGroupSummary>>? = null
    private var categoriesSource: LiveData<List<Category>>? = null

    val displayItems = MediatorLiveData<List<DisplayItem>>().apply {
        var records: List<AccountRecord> = emptyList()
        var categories: List<Category> = emptyList()
        var summaries: List<DateGroupSummary> = emptyList()

        val refresh = {
            value = buildDisplayItems(records, categories, summaries)
        }

        // 监听日期变化，重新加载数据
        addSource(_startDate) {
            loadRecords()
        }
        addSource(_endDate) {
            loadRecords()
        }
    }

    init {
        loadCategories()
        loadRecords()
    }

    /** 响应式监听指定日期范围的账单数据 */
    private fun loadRecords() {
        val start = _startDate.value ?: currentMonthStart
        val end = _endDate.value ?: currentMonthEnd

        // 移除旧的 Room 源
        recordsSource?.let { displayItems.removeSource(it) }
        summarySource?.let { displayItems.removeSource(it) }

        // 创建新的 Room LiveData（Room 会在数据库变化时自动推送更新）
        val newRecords = accountRepo.getRecordsByDateRange(start, end)
        val newSummaries = accountRepo.getDateGroupSummary(start, end)

        // 监听 Room LiveData — 响应式更新！
        displayItems.addSource(newRecords) { list ->
            // 从 displayItems 内部变量获取最新分类和小计，重建列表
            val cats = categoriesSource?.value ?: emptyList()
            val summs = summarySource?.value ?: emptyList()
            displayItems.value = buildDisplayItems(list, cats, summs)
        }
        displayItems.addSource(newSummaries) { list ->
            val recs = recordsSource?.value ?: emptyList()
            val cats = categoriesSource?.value ?: emptyList()
            displayItems.value = buildDisplayItems(recs, cats, list)
        }

        recordsSource = newRecords
        summarySource = newSummaries
    }

    /** 响应式监听分类数据 */
    private fun loadCategories() {
        categoriesSource?.let { displayItems.removeSource(it) }

        val newCats = categoryRepo.getAllCategories()
        displayItems.addSource(newCats) { cats ->
            val recs = recordsSource?.value ?: emptyList()
            val summs = summarySource?.value ?: emptyList()
            displayItems.value = buildDisplayItems(recs, cats ?: emptyList(), summs)
        }

        categoriesSource = newCats
    }

    private fun loadSummary() {
        val start = _startDate.value ?: currentMonthStart
        val end = _endDate.value ?: currentMonthEnd
        // Simplified summary loading
    }

    private fun buildDisplayItems(
        records: List<AccountRecord>,
        categories: List<Category>,
        summaries: List<DateGroupSummary>
    ): List<DisplayItem> {
        val items = mutableListOf<DisplayItem>()
        val categoryMap = categories.associateBy { it.id }
        val summaryMap = summaries.associateBy { DateUtils.getStartOfDay(it.date) }

        val groupedRecords = records.groupBy { DateUtils.getStartOfDay(it.date) }
        val sortedDates = groupedRecords.keys.sortedDescending()

        sortedDates.forEach { dateStart ->
            val summary = summaryMap[dateStart]
            items.add(
                DisplayItem(
                    type = DisplayItem.TYPE_HEADER,
                    date = dateStart,
                    totalIncome = summary?.totalIncome ?: 0.0,
                    totalExpense = summary?.totalExpense ?: 0.0
                )
            )
            groupedRecords[dateStart]?.forEach { record ->
                val category = categoryMap[record.categoryId]
                items.add(
                    DisplayItem(
                        type = DisplayItem.TYPE_RECORD,
                        date = record.date,
                        recordId = record.id,
                        categoryName = category?.name ?: "未知",
                        categoryIcon = (category?.name?.firstOrNull()?.toString() ?: "?"),
                        amount = record.amount,
                        note = record.note,
                        recordType = record.type
                    )
                )
            }
        }

        return items
    }

    fun deleteRecord(record: AccountRecord) {
        scope.launch {
            accountRepo.delete(record)
        }
    }

    suspend fun getRecordById(id: Long): AccountRecord? {
        return accountRepo.getRecordById(id)
    }

    override fun onCleared() {
        super.onCleared()
        recordsSource?.let { displayItems.removeSource(it) }
        summarySource?.let { displayItems.removeSource(it) }
        categoriesSource?.let { displayItems.removeSource(it) }
    }
}
