package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import com.example.ui.components.EmptyStateView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CashbookEntry
import com.example.viewmodel.HaazriViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashbookScreen(viewModel: HaazriViewModel) {
    val entries by viewModel.cashbookEntries.collectAsState()
    val isAmountsHidden by viewModel.isAmountsHidden.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Calculate totals
    val totalIncome = entries.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = entries.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val closingBalance = totalIncome - totalExpense

    // Month display string
    val currentMonthStr = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month Selector Bar (Screenshot 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Previous month */ }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.DarkGray)
                }
                Text(
                    text = currentMonthStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                IconButton(onClick = { /* Next month */ }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.DarkGray)
                }
            }

            Divider(color = Color(0xFFE2E8F0))

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Summary Card (Exact Screenshot 5 design)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)), // Soft periwinkle/blue card
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opening balance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        Text("₹0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Closing balance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(viewModel.maskAmount(closingBalance), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Income & Expense Sub-Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Box
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Income", fontSize = 12.sp, color = Color.Gray)
                                    Text(viewModel.maskAmount(totalIncome), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        // Expense Box
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFEBEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Expense", fontSize = 12.sp, color = Color.Gray)
                                    Text(viewModel.maskAmount(totalExpense), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transactions Body
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = "No Cashbook Transactions",
                        description = "Log site expenses, cash advances, material receipts, or payments to maintain clear financial records.",
                        actionLabel = "+ Add First Entry",
                        onActionClick = { showAddDialog = true },
                        testTag = "empty_cashbook_state"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        CashbookEntryCard(entry = entry, isAmountsHidden = isAmountsHidden, onDelete = { viewModel.deleteCashbookEntry(entry) })
                    }
                }
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFF253B80),
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Entry", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .testTag("cashbook_fab")
        )

        // Dialog to Add Cashbook Entry
        if (showAddDialog) {
            AddCashbookEntryDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type, amount, category, notes, date ->
                    viewModel.addCashbookEntry(type, amount, category, notes, null, date)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CashbookEntryCard(
    entry: CashbookEntry,
    isAmountsHidden: Boolean = false,
    onDelete: () -> Unit
) {
    val isIncome = entry.type == "INCOME"
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.notes.isNotBlank()) entry.notes else entry.category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "${entry.date} · ${entry.time}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            val amountText = if (isAmountsHidden) "₹••••" else "₹${entry.amount.toInt()}"
            Text(
                text = "${if (isIncome) "+" else "-"} $amountText",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashbookEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, amount: Double, category: String, notes: String, date: String) -> Unit
) {
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var type by remember { mutableStateOf("INCOME") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Payment") }
    var notes by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(sdfDate.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                sdfDate.parse(dateInput)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateInput = sdfDate.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Cashbook Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .padding(3.dp)
                ) {
                    listOf("INCOME", "EXPENSE").forEach { t ->
                        val isSelected = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) (if (t == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)) else Color.Transparent, RoundedCornerShape(18.dp))
                                .clickable { type = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (t == "INCOME") "Income (+)" else "Expense (-)",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cashbook_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Description / Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onAdd(type, amt, category, notes, dateInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B80))
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
