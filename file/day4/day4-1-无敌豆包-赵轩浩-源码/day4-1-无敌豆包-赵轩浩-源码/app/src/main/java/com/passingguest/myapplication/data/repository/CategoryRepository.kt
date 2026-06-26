package com.passingguest.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.passingguest.myapplication.data.dao.CategoryDao
import com.passingguest.myapplication.data.entity.Category

class CategoryRepository(private val dao: CategoryDao) {

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    fun getAllCategories(): LiveData<List<Category>> = dao.getAllCategories()

    fun getCategoriesByType(type: String): LiveData<List<Category>> = dao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Long): Category? = dao.getCategoryById(id)
}
