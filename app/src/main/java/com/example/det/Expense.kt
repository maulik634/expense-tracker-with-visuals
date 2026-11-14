package com.example.det

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: String,
    val date: Long
)

// This data class was missing, causing "Unresolved reference: CategoryTotal"
data class CategoryTotal(val category: String, val total: Double)
