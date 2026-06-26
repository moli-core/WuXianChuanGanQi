package com.passingguest.myapplication.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.passingguest.myapplication.App
import com.passingguest.myapplication.data.entity.AccountRecord
import com.passingguest.myapplication.data.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AddEditRecordViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = (application as App).accountRepository
    private val categoryRepo = (application as App).categoryRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val expenseCategories: LiveData<List<Category>> = categoryRepo.getCategoriesByType(Category.TYPE_EXPENSE)
    val incomeCategories: LiveData<List<Category>> = categoryRepo.getCategoriesByType(Category.TYPE_INCOME)

    private var editingRecord: AccountRecord? = null

    suspend fun loadRecord(recordId: Long): AccountRecord? {
        editingRecord = accountRepo.getRecordById(recordId)
        return editingRecord
    }

    fun saveRecord(
        type: String,
        categoryId: Long,
        amount: Double,
        date: Long,
        note: String?,
        onResult: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                val record = editingRecord?.copy(
                    type = type,
                    categoryId = categoryId,
                    amount = amount,
                    date = date,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                ) ?: AccountRecord(
                    type = type,
                    categoryId = categoryId,
                    amount = amount,
                    date = date,
                    note = note
                )

                if (editingRecord != null) {
                    accountRepo.update(record)
                } else {
                    accountRepo.insert(record)
                }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }
}
