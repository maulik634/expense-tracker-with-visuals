package com.example.det

import androidx.lifecycle.LiveData

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpenses(): LiveData<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesForToday(startOfDay: Long, endOfDay: Long): LiveData<List<Expense>> {
        return expenseDao.getExpensesForToday(startOfDay, endOfDay)
    }

    fun getCategoryTotals(): LiveData<List<CategoryTotal>> = expenseDao.getCategoryTotals()

    suspend fun insert(expense: Expense) {
        expenseDao.insert(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.update(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.delete(expense)
    }
}
