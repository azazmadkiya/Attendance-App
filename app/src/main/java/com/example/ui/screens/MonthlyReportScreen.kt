package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.WageCalculator
import com.example.util.WorkerPdfGenerator
import com.example.viewmodel.HaazriViewModel
import com.example.viewmodel.ScreenState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(viewModel: HaazriViewModel) {
    val workers by viewModel.workers.collectAsState()
    val allRecords by viewModel.allAttendanceRecords.collectAsState()
    val defaultMonthYear by viewModel.selectedMonthYear.collectAsState()
    val context = LocalContext.current

    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfDateDisplay = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()) }

    // Filter Mode: "MONTH" (Month-Wise) or "DATE_RANGE" (Date To Date)
    var filterMode by remember { mutableStateOf("MONTH") }

    var currentMonthStr by remember(defaultMonthYear) { mutableStateOf(defaultMonthYear) }

    // Date to Date state
    val today = remember { Calendar.getInstance() }
    val firstDayOfMonth = remember {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c
    }

    var fromDateStr by remember { mutableStateOf(sdfDate.format(firstDayOfMonth.time)) }
    var toDateStr by remember { mutableStateOf(sdfDate.format(today.time)) }
    var searchQuery by remember { mutableStateOf("") }

    // Formatted Titles for display and reports
    val activePeriodTitle = remember(filterMode, currentMonthStr, fromDateStr, toDateStr) {
        if (filterMode == "MONTH") {
            currentMonthStr
        } else {
            "$fromDateStr to $toDateStr"
        }
    }

    val displayPeriodText = remember(activePeriodTitle) {
        WorkerPdfGenerator.formatDisplayPeriod(activePeriodTitle)
    }

    // Helper to format individual date for UI card
    fun formatDateForUi(dateStr: String): String {
        return try {
            val d = sdfDate.parse(dateStr)
            if (d != null) sdfDateDisplay.format(d) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    // Calculate summaries based on current filter mode
    val currentSummaries = remember(workers, allRecords, filterMode, currentMonthStr, fromDateStr, toDateStr) {
        val recordsByWorker = allRecords.groupBy { it.workerId }
        workers.map { worker ->
            val allWorkerRecords = recordsByWorker[worker.id] ?: emptyList()
            val filteredRecords = when (filterMode) {
                "MONTH" -> allWorkerRecords.filter { it.date.startsWith(currentMonthStr) }
                "DATE_RANGE" -> allWorkerRecords.filter { it.date >= fromDateStr && it.date <= toDateStr }
                else -> allWorkerRecords
            }
            WageCalculator.calculateWageForRecords(worker, activePeriodTitle, filteredRecords)
        }
    }

    val filteredSummaries = remember(currentSummaries, searchQuery) {
        if (searchQuery.isBlank()) currentSummaries
        else currentSummaries.filter { it.workerName.contains(searchQuery, ignoreCase = true) }
    }

    val totalWorkers = currentSummaries.size
    val totalWagePayroll = currentSummaries.sumOf { it.netMonthlyWage }
    val totalPresentDays = currentSummaries.sumOf { it.totalPresentDays }
    val totalHalfDays = currentSummaries.sumOf { it.totalHalfDays }
    val totalOtHours = currentSummaries.sumOf { it.totalOvertimeHours }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All Employees Report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("कर्मचारियों की रिपोर्ट • Month & Date Filter", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.activeScreen.value = ScreenState.MAIN_TABS }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3A8A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Filter Mode Selector (Month-Wise vs Date To Date)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Month-Wise Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (filterMode == "MONTH") Color(0xFF1E3A8A) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { filterMode = "MONTH" }
                                .testTag("filter_mode_month")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (filterMode == "MONTH") Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Month-Wise",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (filterMode == "MONTH") Color.White else Color(0xFF475569)
                                )
                            }
                        }

                        // Date To Date Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (filterMode == "DATE_RANGE") Color(0xFF1E3A8A) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { filterMode = "DATE_RANGE" }
                                .testTag("filter_mode_date_range")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = if (filterMode == "DATE_RANGE") Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Date To Date",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (filterMode == "DATE_RANGE") Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month-Wise Controls
                    if (filterMode == "MONTH") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val cal = Calendar.getInstance()
                                            val parsed = sdfMonth.parse(currentMonthStr)
                                            if (parsed != null) {
                                                cal.time = parsed
                                                cal.add(Calendar.MONTH, -1)
                                                currentMonthStr = sdfMonth.format(cal.time)
                                            }
                                        } catch (e: Exception) {
                                            // fallback
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF1E3A8A))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayPeriodText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            val cal = Calendar.getInstance()
                                            val parsed = sdfMonth.parse(currentMonthStr)
                                            if (parsed != null) {
                                                cal.time = parsed
                                                cal.add(Calendar.MONTH, 1)
                                                currentMonthStr = sdfMonth.format(cal.time)
                                            }
                                        } catch (e: Exception) {
                                            // fallback
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF1E3A8A))
                                }
                            }
                        }
                    }

                    // Date To Date Controls
                    if (filterMode == "DATE_RANGE") {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // From Date Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            try {
                                                val parsed = sdfDate.parse(fromDateStr)
                                                if (parsed != null) cal.time = parsed
                                            } catch (e: Exception) { }

                                            val dpd = DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance()
                                                    newCal.set(year, month, dayOfMonth)
                                                    fromDateStr = sdfDate.format(newCal.time)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            )
                                            dpd.show()
                                        }
                                        .testTag("from_date_picker")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("FROM DATE (से)", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(formatDateForUi(fromDateStr), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        }
                                    }
                                }

                                // To Date Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            try {
                                                val parsed = sdfDate.parse(toDateStr)
                                                if (parsed != null) cal.time = parsed
                                            } catch (e: Exception) { }

                                            val dpd = DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance()
                                                    newCal.set(year, month, dayOfMonth)
                                                    toDateStr = sdfDate.format(newCal.time)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            )
                                            dpd.show()
                                        }
                                        .testTag("to_date_picker")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("TO DATE (तक)", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(formatDateForUi(toDateStr), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Date Range Quick Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickPresetChip(text = "This Month") {
                                    val c1 = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                                    val c2 = Calendar.getInstance()
                                    fromDateStr = sdfDate.format(c1.time)
                                    toDateStr = sdfDate.format(c2.time)
                                }
                                QuickPresetChip(text = "Last 30 Days") {
                                    val c1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                                    val c2 = Calendar.getInstance()
                                    fromDateStr = sdfDate.format(c1.time)
                                    toDateStr = sdfDate.format(c2.time)
                                }
                                QuickPresetChip(text = "Last 7 Days") {
                                    val c1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                                    val c2 = Calendar.getInstance()
                                    fromDateStr = sdfDate.format(c1.time)
                                    toDateStr = sdfDate.format(c2.time)
                                }
                                QuickPresetChip(text = "Prev Month") {
                                    val c1 = Calendar.getInstance().apply {
                                        add(Calendar.MONTH, -1)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    val c2 = Calendar.getInstance().apply {
                                        add(Calendar.MONTH, -1)
                                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                    }
                                    fromDateStr = sdfDate.format(c1.time)
                                    toDateStr = sdfDate.format(c2.time)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Executive Summary Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Staff", fontSize = 11.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
                        Text("$totalWorkers workers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("$totalPresentDays P • $totalHalfDays Half", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Net Payroll", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                        Text(viewModel.maskAmount(totalWagePayroll), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Text("OT: $totalOtHours hrs total", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Export Actions (PDF Master Report & Excel CSV)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        WorkerPdfGenerator.generateMonthlyPayrollPdf(
                            context = context,
                            monthYear = activePeriodTitle,
                            summaries = currentSummaries,
                            viewModel = viewModel
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_all_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download All PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        WorkerPdfGenerator.exportMonthlySummaryCsv(
                            context = context,
                            monthYear = activePeriodTitle,
                            summaries = currentSummaries
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_all_excel_btn")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Excel / CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar for quick staff lookup
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search worker by name in report...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF94A3B8),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Staff Breakdown (${filteredSummaries.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF334155)
                )
                Text(
                    text = displayPeriodText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3A8A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // List of All Staff Breakdown
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSummaries, key = { it.workerId }) { summary ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedWorkerId.value = summary.workerId
                                viewModel.activeScreen.value = ScreenState.WORKER_DETAILS
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(summary.workerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                    Text(
                                        text = "${summary.wageType} Rate: ${viewModel.maskAmount(summary.baseWageRate)}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Salary", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = viewModel.maskAmount(summary.netMonthlyWage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("${summary.totalPresentDays} P", color = Color(0xFF166534), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("${summary.totalHalfDays} Half", color = Color(0xFF92400E), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("${summary.totalAbsentDays} A", color = Color(0xFF991B1B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    if (summary.totalOvertimeHours > 0) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("${summary.totalOvertimeHours}h OT", color = Color(0xFF1E40AF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = "Base: ₹${summary.grossBasePay.toInt()}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickPresetChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            maxLines = 1,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
