package com.passingguest.myapplication.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index("categoryId"), Index("yearMonth")]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null, // null = overall budget, non-null = category-specific
    val yearMonth: String, // "2026-06" format
    val amount: Double,
    val type: String // "INCOME" or "EXPENSE"
)
