package com.example.det

import android.app.Application
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import com.example.det.theme.DetTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

val expenseCategories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Other")
val filterCategories = listOf("All") + expenseCategories

// --- NAVIGATION SETUP ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Expenses : Screen("expenses", "Expenses", Icons.AutoMirrored.Filled.List)
    object Stats : Screen("stats", "Stats", Icons.Default.BarChart)
}

val bottomNavItems = listOf(Screen.Expenses, Screen.Stats)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DetTheme {
                ExpenseAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAppRoot() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Expense Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
                // All 'actions' for the download button have been removed.
            )
        },
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}


// --- NAVIGATION COMPOSABLES ---
@Composable
fun AppBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Expenses.route,
        modifier = modifier
    ) {
        composable(Screen.Expenses.route) {
            ExpenseScreen()
        }
        composable(Screen.Stats.route) {
            StatsScreen()
        }
    }
}

// --- SCREEN COMPOSABLES ---

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(LocalContext.current.applicationContext as Application))
    val categoryTotals by viewModel.categoryTotals.observeAsState(initial = emptyList())

    val barData = categoryTotals.mapIndexed { index, categoryTotal ->
        BarData(
            point = Point(index.toFloat(), categoryTotal.total.toFloat()),
            label = categoryTotal.category.take(3),
            color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        )
    }

    val maxAmount = categoryTotals.maxOfOrNull { it.total }?.toFloat() ?: 100f

    val xAxisData = AxisData.Builder()
        .axisStepSize(30.dp)
        .bottomPadding(10.dp)
        .axisLabelAngle(0f)
        .labelData { i -> barData.getOrNull(i)?.label ?: "" }
        .build()

    val yAxisData = AxisData.Builder()
        .steps(5)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i -> String.format(Locale.getDefault(), "₹%.0f", (maxAmount / 5) * i) }
        .build()

    val barChartData = BarChartData(
        chartData = barData,
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        barStyle = BarStyle(barWidth = 25.dp),
        showYAxis = true,
        showXAxis = true,
        horizontalExtraSpace = 20.dp
    )

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (barData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expense data available. Add expenses to see the chart.")
            }
        } else {
            Text("Total Spending by Category", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
            BarChart(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                barChartData = barChartData
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(context.applicationContext as Application))
    val expenses by viewModel.filteredExpenses.observeAsState(initial = emptyList())
    val totalForToday by viewModel.totalForToday.observeAsState()

    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SummaryBar(total = totalForToday ?: 0.0)

            CategoryFilter(
                categories = filterCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                    val categoryToFilter = if (category == "All") null else category
                    viewModel.selectCategory(categoryToFilter)
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (expenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (selectedCategory == "All") "No expenses yet. Tap '+' to add one!"
                            else "No expenses found for the category: \"$selectedCategory\""
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses) { expense ->
                            ExpenseItem(
                                expense = expense,
                                onDelete = { expenseToDelete = expense },
                                onClick = {
                                    expenseToEdit = expense
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                expenseToEdit = null
                showEditDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Expense")
        }
    }

    if (showEditDialog) {
        ExpenseDialog(
            expenseToEdit = expenseToEdit,
            onDismiss = { showEditDialog = false },
            onConfirm = { description, amount, category, date ->
                if (expenseToEdit == null) {
                    val newExpense = Expense(description = description, amount = amount, category = category, date = date)
                    viewModel.insert(newExpense)
                } else {
                    val updatedExpense = expenseToEdit!!.copy(description = description, amount = amount, category = category, date = date)
                    viewModel.update(updatedExpense)
                }
                showEditDialog = false
            }
        )
    }

    if (expenseToDelete != null) {
        DeleteConfirmationDialog(
            expense = expenseToDelete!!,
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                viewModel.delete(it)
                expenseToDelete = null
            }
        )
    }
}


// --- HELPER COMPOSABLES ---

fun Long.toFormattedDateString(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = (category == selectedCategory),
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun SummaryBar(total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Total for Today:", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = String.format(Locale.getDefault(), "₹%.2f", total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.description, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = expense.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "₹%.2f", expense.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = expense.date.toFormattedDateString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Expense", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    expenseToEdit: Expense?,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String, date: Long) -> Unit
) {
    var description by remember { mutableStateOf(expenseToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: expenseCategories.first()) }
    var expanded by remember { mutableStateOf(false) }
    var date by remember { mutableLongStateOf(expenseToEdit?.date ?: System.currentTimeMillis()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val isEditing = expenseToEdit != null
    val dialogTitle = if (isEditing) "Edit Expense" else "Add New Expense"
    val buttonText = if (isEditing) "Save" else "Add"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(dialogTitle, style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        expenseCategories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item) },
                                onClick = {
                                    category = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Date: ${date.toFormattedDateString()}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val amountDouble = amount.toDoubleOrNull()
                        if (description.isBlank()) {
                            Toast.makeText(context, "Description cannot be empty", Toast.LENGTH_SHORT).show()
                        } else if (amount.isBlank() || amountDouble == null || amountDouble <= 0) {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(description, amountDouble, category, date)
                        }
                    }) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete the expense: \"${expense.description}\"?") },
        confirmButton = {
            Button(
                onClick = { onConfirm(expense) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Yes, Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
