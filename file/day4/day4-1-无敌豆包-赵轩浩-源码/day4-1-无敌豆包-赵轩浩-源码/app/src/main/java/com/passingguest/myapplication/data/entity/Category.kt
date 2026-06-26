package com.passingguest.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val iconResId: Int? = null,
    val sortOrder: Int = 0
) {
    companion object {
        // Expense categories
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"

        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "餐饮", "交通", "购物", "娱乐", "住房",
            "通讯", "医疗", "教育", "其他"
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            "工资", "奖金", "兼职", "投资收益", "红包", "其他"
        )
    }
}
