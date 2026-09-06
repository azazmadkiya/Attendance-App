package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.AttendanceCalendarView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CashbookEntry
import com.example.data.Worker
import com.example.ui.theme.*
import com.example.util.WorkerPdfGenerator
import com.example.viewmodel.HaazriViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CombinedLedgerItem(
    val id: String,
    val date: String,
    val title: String,
    val category: String,
    val notes: String,
    val amount: Double,
    val isCredit: Boolean, // true = Earned (+), false = Paid (-)
    val cashbookEntryRef: CashbookEntry? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailsScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val worker by viewModel.selectedWorker.collectAsState()
    val attendanceHistory by viewModel.selectedWorkerAttendance.collectAsState()
    val monthlySummary by viewModel.selectedWorkerMonthlySummary.collectAsState()
    val cashbookEntries by viewModel.selectedWorkerCashbookEntries.collectAsState()

    if (worker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Worker details not found")
        }
        return
    }

    val currentWorker = worker!!

    // Report Filter State: "MONTH", "DATE_RANGE"
    var filterType by remember { mutableStateOf("MONTH") }

    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfMonthYearDisplay = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val today = remember { Date() }

    var selectedFilterMonth by remember { mutableStateOf(sdfMonth.format(today)) } // e.g. "2026-08"
    val formattedMonthYear = remember(selectedFilterMonth) {
        try {
            val parsed = sdfMonth.parse(selectedFilterMonth)
            if (parsed != null) sdfMonthYearDisplay.format(parsed) else selectedFilterMonth
        } catch (e: Exception) {
            selectedFilterMonth
        }
    }
    var customStartDate by remember { mutableStateOf("${selectedFilterMonth}-01") }
    var customEndDate by remember { mutableStateOf(sdfDate.format(today)) }

    // Filter attendance history based on filterType
    val filteredAttendanceHistory = remember(attendanceHistory, filterType, selectedFilterMonth, customStartDate, customEndDate) {
        when (filterType) {
            "MONTH" -> attendanceHistory.filter { it.date.startsWith(selectedFilterMonth) }
            "DATE_RANGE" -> attendanceHistory.filter { it.date >= customStartDate && it.date <= customEndDate }
            else -> attendanceHistory
        }
    }

    // Filter cashbook entries based on filterType
    val filteredCashbookEntries = remember(cashbookEntries, filterType, selectedFilterMonth, customStartDate, customEndDate) {
        when (filterType) {
            "MONTH" -> cashbookEntries.filter { it.date.startsWith(selectedFilterMonth) }
            "DATE_RANGE" -> cashbookEntries.filter { it.date >= customStartDate && it.date <= customEndDate }
            else -> cashbookEntries
        }
    }

    val reportPeriodTitle = when (filterType) {
        "MONTH" -> formattedMonthYear
        "DATE_RANGE" -> "$customStartDate to $customEndDate"
        else -> "Full History"
    }

    val presentCount = filteredAttendanceHistory.count { it.status == "P" }
    val absentCount = filteredAttendanceHistory.count { it.status == "A" }
    val halfDayCount = filteredAttendanceHistory.count { it.status == "1/2" }
    val offCount = filteredAttendanceHistory.count { it.status == "O" }
    val markedCount = presentCount + absentCount + halfDayCount + offCount

    val totalDaysCount = remember(filterType, selectedFilterMonth, customStartDate, customEndDate, filteredAttendanceHistory) {
        if (filterType == "MONTH") {
            try {
                val cal = Calendar.getInstance()
                val parsedDate = sdfMonth.parse(selectedFilterMonth) ?: today
                cal.time = parsedDate
                val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val isCurrentMonth = selectedFilterMonth == sdfMonth.format(today)
                if (isCurrentMonth) {
                    val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    val maxMarkedDay = filteredAttendanceHistory.mapNotNull {
                        it.date.split("-").getOrNull(2)?.toIntOrNull()
                    }.maxOrNull() ?: currentDayOfMonth
                    maxOf(currentDayOfMonth, maxMarkedDay, markedCount)
                } else {
                    maxDaysInMonth
                }
            } catch (e: Exception) {
                maxOf(markedCount, 1)
            }
        } else if (filterType == "DATE_RANGE") {
            try {
                val d1 = sdfDate.parse(customStartDate)
                val d2 = sdfDate.parse(customEndDate)
                if (d1 != null && d2 != null && !d2.before(d1)) {
                    val diffDays = ((d2.time - d1.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                    maxOf(diffDays, markedCount)
                } else {
                    maxOf(markedCount, 1)
                }
            } catch (e: Exception) {
                maxOf(markedCount, 1)
            }
        } else {
            maxOf(markedCount, 1)
        }
    }

    val pendingCount = maxOf(0, totalDaysCount - markedCount)

    // Map Attendance Earnings to Ledger Items
    val attendanceLedgerItems = filteredAttendanceHistory.filter { it.status == "P" || it.status == "1/2" || it.overtimeHours > 0 || it.customAmount > 0 }.map { rec ->
        val dailyWage = viewModel.getDailyWageBreakdown(currentWorker, rec)
        val statusName = when (rec.status) {
            "P" -> "Present"
            "A" -> "Absent"
            "1/2" -> "Half Day"
            "O" -> "Off"
            else -> rec.status
        }
        val customNote = if (rec.customAmount > 0) " • Manual Amount ₹${rec.customAmount.toInt()}" else ""
        CombinedLedgerItem(
            id = "att_${rec.id}",
            date = rec.date,
            title = "Daily Attendance ($statusName)",
            category = "Earned Wage",
            notes = "Check-in: ${rec.checkInTime.ifEmpty { "09:00 AM" }}${if (rec.isGeofenceVerified) " • Geofenced" else ""}$customNote",
            amount = dailyWage.netDailyWage,
            isCredit = true
        )
    }

    // Map Cashbook Payments to Ledger Items
    val cashbookLedgerItems = filteredCashbookEntries.map { entry ->
        CombinedLedgerItem(
            id = "cb_${entry.id}",
            date = entry.date,
            title = entry.category,
            category = entry.category,
            notes = "${entry.time} • ${entry.notes.ifEmpty { "Paid" }}",
            amount = entry.amount,
            isCredit = false,
            cashbookEntryRef = entry
        )
    }

    // Combined Unified Ledger List sorted by date (newest first)
    val combinedLedgerList = (attendanceLedgerItems + cashbookLedgerItems).sortedByDescending { it.date }

    val calculatedEarnedPay = attendanceLedgerItems.sumOf { it.amount }
    val totalPaid = cashbookEntries.sumOf { it.amount }
    val netBalance = calculatedEarnedPay - totalPaid

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditWorkerDialog by remember { mutableStateOf(false) }

    // Modify Worker Details Dialog
    if (showEditWorkerDialog) {
        ModifyWorkerDialog(
            worker = currentWorker,
            onDismiss = { showEditWorkerDialog = false },
            onSave = { updatedWorker ->
                viewModel.updateWorker(updatedWorker)
                showEditWorkerDialog = false
                Toast.makeText(context, "Worker details updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Payment Dialog state
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentCategory by remember { mutableStateOf("Cash Payment") } // Cash Payment, Advance Pay, Bank Pay (NEFT/TPT)
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentNotesInput by remember { mutableStateOf("") }
    var paymentDateInput by remember { mutableStateOf(sdfDate.format(today)) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }

    // View tab (0 = Attendance Logs, 1 = Account Ledger)
    var activeHistoryTab by remember { mutableStateOf(0) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Worker") },
            text = { Text("Are you sure you want to delete ${currentWorker.name}? This will remove all associated attendance logs and account ledger history permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteWorker(currentWorker)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Interactive Employee Payment / Advance Dialog
    if (showPaymentDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                sdfDate.parse(paymentDateInput)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPaymentDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        paymentDateInput = sdfDate.format(Date(it))
                    }
                    showPaymentDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (selectedPaymentCategory) {
                            "Advance Pay" -> Icons.Default.ReceiptLong
                            "Bank Pay (NEFT/TPT)" -> Icons.Default.AccountBalance
                            else -> Icons.Default.Payments
                        },
                        contentDescription = null,
                        tint = HaazriPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record $selectedPaymentCategory",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Worker: ${currentWorker.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (paymentError != null) {
                        Text(
                            text = paymentError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Amount
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("e.g. 2000") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_amount_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes / Reference
                    OutlinedTextField(
                        value = paymentNotesInput,
                        onValueChange = { paymentNotesInput = it },
                        label = { Text("Notes / Reference No. (Optional)") },
                        placeholder = { Text(if (selectedPaymentCategory.contains("NEFT")) "e.g. NEFT Ref 982310" else "e.g. Weekly wage advance") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_notes_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Date
                    OutlinedTextField(
                        value = paymentDateInput,
                        onValueChange = { },
                        label = { Text("Date") },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPaymentDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = paymentAmountInput.toDoubleOrNull()
                        if (amountVal == null || amountVal <= 0) {
                            paymentError = "Please enter a valid amount"
                            return@Button
                        }
                        viewModel.addCashbookEntry(
                            type = "EXPENSE",
                            amount = amountVal,
                            category = selectedPaymentCategory,
                            notes = paymentNotesInput.ifEmpty { "$selectedPaymentCategory for ${currentWorker.name}" },
                            workerId = currentWorker.id,
                            customDate = paymentDateInput
                        )
                        showPaymentDialog = false
                        paymentAmountInput = ""
                        paymentNotesInput = ""
                        paymentDateInput = sdfDate.format(today)
                        paymentError = null
                        activeHistoryTab = 1 // Switch to Employee Account Ledger tab
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                ) {
                    Text("Save Payment Entry", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .padding(16.dp)
    ) {
        // Profile Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC8E6C9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentWorker.name.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentWorker.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(currentWorker.phone.ifEmpty { "No phone number" }, fontSize = 14.sp, color = Color(0xFF64748B))
                        Text("${currentWorker.wageType} Wage: ${viewModel.maskAmount(currentWorker.wageRate)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E3A8A))
                    }

                    // Modify / Edit Worker Button
                    IconButton(
                        onClick = { showEditWorkerDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                            .testTag("edit_worker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modify Worker Details",
                            tint = Color(0xFF253B80),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                            .testTag("delete_worker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Worker",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: Cash Paid, Advance Pay, Bank Pay NEFT/TPT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cash Paid
                    OutlinedButton(
                        onClick = {
                            selectedPaymentCategory = "Cash Payment"
                            paymentAmountInput = ""
                            paymentNotesInput = ""
                            paymentDateInput = sdfDate.format(today)
                            paymentError = null
                            showPaymentDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cash_paid"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cash Paid", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A34A))
                    }

                    // Advance Pay
                    OutlinedButton(
                        onClick = {
                            selectedPaymentCategory = "Advance Pay"
                            paymentAmountInput = ""
                            paymentNotesInput = ""
                            paymentDateInput = sdfDate.format(today)
                            paymentError = null
                            showPaymentDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_advance_pay"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Advance Pay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD97706))
                    }

                    // Bank Pay NEFT/TPT
                    Button(
                        onClick = {
                            selectedPaymentCategory = "Bank Pay (NEFT/TPT)"
                            paymentAmountInput = ""
                            paymentNotesInput = ""
                            paymentDateInput = sdfDate.format(today)
                            paymentError = null
                            showPaymentDialog = true
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_bank_pay"),
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bank Pay NEFT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date-Wise & Month-Wise Report Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report Filter:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                        }

                        // Filter Mode Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterModeChip(text = "Month-Wise", isSelected = filterType == "MONTH") { filterType = "MONTH" }
                            FilterModeChip(text = "Date-Wise", isSelected = filterType == "DATE_RANGE") { filterType = "DATE_RANGE" }
                        }
                    }

                    // Month-Wise Filter Controls
                    if (filterType == "MONTH") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val parts = selectedFilterMonth.split("-")
                                    if (parts.size == 2) {
                                        var year = parts[0].toInt()
                                        var month = parts[1].toInt() - 1
                                        if (month < 1) { month = 12; year -= 1 }
                                        selectedFilterMonth = String.format(Locale.US, "%04d-%02d", year, month)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = Color(0xFF1E3A8A))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedMonthYear,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val parts = selectedFilterMonth.split("-")
                                    if (parts.size == 2) {
                                        var year = parts[0].toInt()
                                        var month = parts[1].toInt() + 1
                                        if (month > 12) { month = 1; year += 1 }
                                        selectedFilterMonth = String.format(Locale.US, "%04d-%02d", year, month)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF1E3A8A))
                            }
                        }
                    }

                    // Date-Wise Custom Range Filter Controls
                    if (filterType == "DATE_RANGE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customStartDate,
                                onValueChange = { newValue -> customStartDate = newValue },
                                label = { Text("Start Date", fontSize = 10.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            OutlinedTextField(
                                value = customEndDate,
                                onValueChange = { newValue -> customEndDate = newValue },
                                label = { Text("End Date", fontSize = 10.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Export Filtered PDF Statement Button
                Button(
                    onClick = {
                        WorkerPdfGenerator.generateAndPrintPdf(
                            context = context,
                            worker = currentWorker,
                            attendanceHistory = filteredAttendanceHistory,
                            cashbookEntries = filteredCashbookEntries,
                            viewModel = viewModel,
                            reportPeriodTitle = reportPeriodTitle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_generate_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate PDF Report ($reportPeriodTitle)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Export Filtered CSV Statement Button
                Button(
                    onClick = {
                        WorkerPdfGenerator.exportWorkerDetailsCsv(
                            context = context,
                            worker = currentWorker,
                            attendanceHistory = filteredAttendanceHistory,
                            cashbookEntries = filteredCashbookEntries,
                            reportPeriodTitle = reportPeriodTitle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_generate_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download Excel / CSV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Attendance Report Summary: Total, Present, Absent, Half Day, Pending (As in Reference Image)
        AttendanceReportSummaryCard(
            total = totalDaysCount,
            present = presentCount,
            absent = absentCount,
            halfDay = halfDayCount,
            pending = pendingCount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Employee Account Ledger Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Earned Pay", viewModel.maskAmount(calculatedEarnedPay), Color(0xFFE0E7FF), Color(0xFF1E3A8A), Modifier.weight(1f))
            StatCard("Total Paid/Advance", viewModel.maskAmount(totalPaid), Color(0xFFFEF3C7), Color(0xFFB45309), Modifier.weight(1f))
            StatCard(
                title = "Net Balance",
                value = viewModel.maskAmount(netBalance),
                bgColor = if (netBalance <= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                textColor = if (netBalance <= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History Log Section Tabs (Attendance vs Employee Account Ledger)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeHistoryTab == 0) HaazriPrimary else Color.Transparent)
                    .clickable { activeHistoryTab = 0 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_attendance_logs"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Attendance Logs (${attendanceHistory.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (activeHistoryTab == 0) Color.White else Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeHistoryTab == 1) HaazriPrimary else Color.Transparent)
                    .clickable { activeHistoryTab = 1 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_account_ledger"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Account Ledger (${combinedLedgerList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (activeHistoryTab == 1) Color.White else Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeHistoryTab == 0) {
            // ATTENDANCE LOGS TAB
            if (filterType == "MONTH") {
                val cal = java.util.Calendar.getInstance()
                try {
                    val sdfMonthParse = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                    cal.time = sdfMonthParse.parse(selectedFilterMonth) ?: java.util.Date()
                } catch (e: Exception) {}
                
                AttendanceCalendarView(
                    year = cal.get(java.util.Calendar.YEAR),
                    month = cal.get(java.util.Calendar.MONTH),
                    attendanceRecords = filteredAttendanceHistory
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (attendanceHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "No Attendance Logs",
                        description = "Attendance records and check-in logs for ${currentWorker.name} will appear here once marked.",
                        testTag = "empty_worker_history_state"
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(attendanceHistory) { rec ->
                        val dailyWage = viewModel.getDailyWageBreakdown(currentWorker, rec)

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.date, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                                    Text("Check-in: ${rec.checkInTime.ifEmpty { "09:00 AM" }} · Daily Net: ${viewModel.maskAmount(dailyWage.netDailyWage)}", fontSize = 12.sp, color = Color(0xFF64748B))
                                    if (rec.notes.isNotBlank()) {
                                        Text("📝 Note: ${rec.notes}", fontSize = 11.sp, color = Color(0xFF253B80), fontWeight = FontWeight.Medium)
                                    }
                                }

                                if (rec.isGeofenceVerified) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Geofenced", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                val (bg, txt) = when (rec.status) {
                                    "P" -> PresentGreenBg to PresentGreenText
                                    "A" -> AbsentRedBg to AbsentRedText
                                    "1/2" -> HalfOrangeBg to HalfOrangeText
                                    else -> OffGrayBg to OffGrayText
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (rec.status == "P") "Present" else if (rec.status == "A") "Absent" else if (rec.status == "1/2") "Half Day" else "Off",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = txt
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // EMPLOYEE ACCOUNT LEDGER TAB (Includes Attendance Earned Logs + Cashbook Payments)
            if (combinedLedgerList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = "No Account Ledger Entries",
                        description = "Attendance earnings and recorded payments for ${currentWorker.name} will appear here.",
                        testTag = "empty_worker_ledger_state"
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(combinedLedgerList, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isCredit) Color(0xFFDCFCE7)
                                            else when (item.category) {
                                                "Advance Pay" -> Color(0xFFFEF3C7)
                                                "Bank Pay (NEFT/TPT)" -> Color(0xFFDBEAFE)
                                                else -> Color(0xFFFEE2E2)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isCredit) Icons.Default.CheckCircle
                                        else when (item.category) {
                                            "Advance Pay" -> Icons.Default.ReceiptLong
                                            "Bank Pay (NEFT/TPT)" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        tint = if (item.isCredit) Color(0xFF15803D)
                                        else when (item.category) {
                                            "Advance Pay" -> Color(0xFFB45309)
                                            "Bank Pay (NEFT/TPT)" -> Color(0xFF1E3A8A)
                                            else -> Color(0xFFB91C1C)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        if (item.isCredit) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Earned",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.date} • ${item.notes}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Text(
                                    text = if (item.isCredit) "+ ${viewModel.maskAmount(item.amount)}" else "- ${viewModel.maskAmount(item.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (item.isCredit) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )

                                if (item.cashbookEntryRef != null) {
                                    IconButton(
                                        onClick = { viewModel.deleteCashbookEntry(item.cashbookEntryRef) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = textColor, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun FilterModeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) HaazriPrimary else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) HaazriPrimary else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
fun AttendanceReportSummaryCard(
    total: Int,
    present: Int,
    absent: Int,
    halfDay: Int,
    pending: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("worker_attendance_report_summary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Total Card (Blue)
            AttendanceStatItem(
                count = total,
                label = "Total",
                bgColor = Color(0xFFEFF6FF),
                textColor = Color(0xFF1E3A8A),
                modifier = Modifier.weight(1f)
            )

            // Present Card (Green)
            AttendanceStatItem(
                count = present,
                label = "Present",
                bgColor = Color(0xFFEAF8EC),
                textColor = Color(0xFF15803D),
                modifier = Modifier.weight(1f)
            )

            // Absent Card (Red)
            AttendanceStatItem(
                count = absent,
                label = "Absent",
                bgColor = Color(0xFFFEE2E2),
                textColor = Color(0xFFB91C1C),
                modifier = Modifier.weight(1f)
            )

            // Half Day Card (Orange)
            AttendanceStatItem(
                count = halfDay,
                label = "Half Day",
                bgColor = Color(0xFFFEF3C7),
                textColor = Color(0xFFC2410C),
                modifier = Modifier.weight(1f)
            )

            // Pending Card (Grey/Slate)
            AttendanceStatItem(
                count = pending,
                label = "Pending",
                bgColor = Color(0xFFF1F5F9),
                textColor = Color(0xFF475569),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AttendanceStatItem(
    count: Int,
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModifyWorkerDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onSave: (Worker) -> Unit
) {
    var name by remember { mutableStateOf(worker.name) }
    var phone by remember { mutableStateOf(worker.phone) }
    var wageType by remember { mutableStateOf(worker.wageType) }
    var wageRate by remember { mutableStateOf(if (worker.wageRate > 0) worker.wageRate.toInt().toString() else "") }
    var overtimeRate by remember { mutableStateOf(if (worker.overtimeRate > 0) worker.overtimeRate.toInt().toString() else "") }
    var upiId by remember { mutableStateOf(worker.upiId) }
    var notes by remember { mutableStateOf(worker.notes) }

    // Advanced wage settings
    var isWageRulesExpanded by remember { mutableStateOf(false) }
    var hajariMultiplier by remember { mutableStateOf(worker.hajariMultiplier) }
    var overtimeMultiplier by remember { mutableStateOf(worker.overtimeMultiplier) }
    var lateFine by remember { mutableStateOf(if (worker.lateFine > 0) worker.lateFine.toInt().toString() else "") }
    var lateGracePeriod by remember { mutableStateOf(if (worker.lateGracePeriodMinutes > 0) worker.lateGracePeriodMinutes.toString() else "") }
    var halfDayPayFactor by remember { mutableStateOf(worker.halfDayPayFactor.toString()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF253B80),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Modify Worker Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Update profile, wages & calculation rules",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp)
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Name field
                Text("Worker Name *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone field
                Text("Phone Number", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("10-digit mobile number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_phone_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Wage Type
                Text("Wage Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Daily", "Weekly", "Monthly").forEach { type ->
                        val isSelected = wageType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { wageType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF253B80) else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wage Rate
                Text(
                    text = when (wageType) {
                        "Daily" -> "Daily Wage Rate (₹ / Day) *"
                        "Weekly" -> "Weekly Wage Rate (₹ / Week) *"
                        else -> "Monthly Salary (₹ / Month) *"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = wageRate,
                    onValueChange = {
                        wageRate = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. 500") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_wage_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Overtime Rate
                Text("Overtime Rate (₹ / Hour)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = overtimeRate,
                    onValueChange = { overtimeRate = it },
                    placeholder = { Text("e.g. 60") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF64748B)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_overtime_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // UPI ID
                Text("UPI ID / Payment VPA", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    placeholder = { Text("e.g. 9825012345@upi") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_upi_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Advanced Wage Rules Accordion
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isWageRulesExpanded = !isWageRulesExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF253B80), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Advanced Rules & Multipliers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
                            }
                            Icon(
                                imageVector = if (isWageRulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }

                        if (isWageRulesExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Hajari Multiplier
                            Text("Hajari / Shift Multiplier", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Off", "2x", "3.5x", "4.75x").forEach { mult ->
                                    val isSel = hajariMultiplier == mult
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { hajariMultiplier = mult },
                                        label = { Text(mult, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF253B80),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Late Fine
                            Text("Late Penalty Fine (₹)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = lateFine,
                                onValueChange = { lateFine = it },
                                placeholder = { Text("e.g. 50") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Late Grace Period
                            Text("Grace Period (Minutes)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = lateGracePeriod,
                                onValueChange = { lateGracePeriod = it },
                                placeholder = { Text("e.g. 15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes / Remarks
                Text("Notes / Role / Address", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("e.g. Master Tailor, Section B") },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_worker_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Worker name cannot be empty"
                        return@Button
                    }
                    val parsedWageRate = wageRate.toDoubleOrNull() ?: 0.0
                    val parsedOvertimeRate = overtimeRate.toDoubleOrNull() ?: 0.0
                    val parsedLateFine = lateFine.toDoubleOrNull() ?: 0.0
                    val parsedGrace = lateGracePeriod.toIntOrNull() ?: 0
                    val parsedHalfDay = halfDayPayFactor.toDoubleOrNull() ?: 0.5

                    val updatedWorker = worker.copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        wageType = wageType,
                        wageRate = parsedWageRate,
                        overtimeRate = parsedOvertimeRate,
                        upiId = upiId.trim(),
                        hajariMultiplier = hajariMultiplier,
                        overtimeMultiplier = overtimeMultiplier,
                        lateFine = parsedLateFine,
                        lateGracePeriodMinutes = parsedGrace,
                        halfDayPayFactor = parsedHalfDay,
                        notes = notes.trim()
                    )
                    onSave(updatedWorker)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B80)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_worker_changes_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}
