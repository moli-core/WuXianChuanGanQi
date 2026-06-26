package com.passingguest.myapplication.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.passingguest.myapplication.App
import com.passingguest.myapplication.data.entity.AccountRecord

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = (application as App).accountRepository
    private val categoryRepo = (application as App).categoryRepository

    private val _keyword = MediatorLiveData<String?>()
    private val _type = MediatorLiveData<String?>() // null = all
    private val _categoryId = MediatorLiveData<Long?>()
    private val _startDate = MediatorLiveData<Long?>()
    private val _endDate = MediatorLiveData<Long?>()

    val searchResults = MediatorLiveData<List<AccountRecord>>().apply {
        var keyword: String? = null
        var type: String? = null
        var categoryId: Long? = null
        var startDate: Long? = null
        var endDate: Long? = null

        val refresh = {
            value = null // trigger loading
            performSearch(keyword, type, categoryId, startDate, endDate)
        }

        addSource(_keyword) { keyword = it; refresh() }
        addSource(_type) { type = it; refresh() }
        addSource(_categoryId) { categoryId = it; refresh() }
        addSource(_startDate) { startDate = it; refresh() }
        addSource(_endDate) { endDate = it; refresh() }
    }

    private fun performSearch(
        keyword: String?,
        type: String?,
        categoryId: Long?,
        startDate: Long?,
        endDate: Long?
    ) {
        accountRepo.searchRecords(
            keyword = keyword?.takeIf { it.isNotBlank() },
            type = type?.takeIf { it != "all" },
            categoryId = categoryId,
            startDate = startDate,
            endDate = endDate
        ).observeForever { results ->
            searchResults.value = results ?: emptyList()
        }
    }

    fun setKeyword(keyword: String) {
        _keyword.value = keyword
    }

    fun setType(type: String?) {
        _type.value = type
    }

    fun setCategoryId(categoryId: Long?) {
        _categoryId.value = categoryId
    }

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }
}
