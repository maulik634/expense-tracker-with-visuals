package com.example.det

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val _allExpenses: LiveData<List<Expense>>
    private val _categoryFilter = MutableLiveData<String?>(null)

    val categoryTotals: LiveData<List<CategoryTotal>>
    val totalForToday: LiveData<Double>

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        _allExpenses = repository.getAllExpenses()
        categoryTotals = repository.getCategoryTotals()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val expensesForToday = repository.getExpensesForToday(startOfDay, endOfDay)

        // THIS IS THE CORRECTED SECTION THAT REMOVES "TRANSFORMATIONS"
        totalForToday = expensesForToday.map { expenses ->
            expenses.sumOf { it.amount }
        }
    }

    val filteredExpenses: LiveData<List<Expense>> = MediatorLiveData<List<Expense>>().apply {
        addSource(_allExpenses) { expenses ->
            value = filterExpenses(expenses, _categoryFilter.value)
        }
        addSource(_categoryFilter) { category ->
            value = filterExpenses(_allExpenses.value, category)
        }
    }

    private fun filterExpenses(expenses: List<Expense>?, category: String?): List<Expense> {
        if (expenses == null) return emptyList()
        return if (category == null) {
            expenses
        } else {
            expenses.filter { it.category == category }
        }
    }

    fun selectCategory(category: String?) {
        _categoryFilter.value = category
    }

    fun insert(expense: Expense) = viewModelScope.launch {
        repository.insert(expense)
    }

    fun update(expense: Expense) = viewModelScope.launch {
        repository.update(expense)
    }

    fun delete(expense: Expense) = viewModelScope.launch {
        repository.delete(expense)
    }
}
