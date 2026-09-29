package com.attendance.app.azaz.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.attendance.app.azaz.ui.components.EmptyStateView
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
import com.attendance.app.azaz.data.AttendanceRecord
import com.attendance.app.azaz.data.Worker
import com.attendance.app.azaz.ui.theme.*
import com.attendance.app.azaz.viewmodel.HaazriViewModel
import com.attendance.app.azaz.viewmodel.ScreenState
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: HaazriViewModel) {
    val workers by viewModel.workers.collectAsState()
    val attendanceRecords by viewModel.currentAttendanceRecords.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val mode by viewModel.attendanceMode.collectAsState()
    val rollCallIndex by viewModel.rollCallIndex.collectAsState()
    val isAmountsHidden by viewModel.isAmountsHidden.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    val filteredWorkers = remember(workers, attendanceRecords, searchQuery, statusFilter) {
        workers.filter { worker ->
            val matchesSearch = worker.name.contains(searchQuery, ignoreCase = true)
            val record = attendanceRecords.find { it.workerId == worker.id }
            val status = record?.status
            val matchesFilter = when (statusFilter) {
                "Present" -> status == "P"
                "Absent" -> status == "A"
                "Half" -> status == "1/2"
                "Off" -> status == "O"
                "Not Marked" -> status == null
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    var workerForManualAmount by remember { mutableStateOf<Worker?>(null) }

    val context = LocalContext.current

    // Dialog for Manual Custom Amount for Absent, Half, Off, Present and Adding Attendance Notes
    val targetWorker = workerForManualAmount
    if (targetWorker != null) {
        val record = attendanceRecords.find { it.workerId == targetWorker.id }
        ManualAmountDialog(
            worker = targetWorker,
            currentRecord = record,
            selectedDate = selectedDate,
            onDismiss = { workerForManualAmount = null },
            onSave = { status, amount, note ->
                viewModel.setAttendance(targetWorker.id, status, customAmount = amount, notes = note)
                workerForManualAmount = null
            },
            onReset = {
                viewModel.setAttendance(targetWorker.id, record?.status ?: "A", customAmount = 0.0, notes = "")
                workerForManualAmount = null
            }
        )
    }

    // Format date for display (e.g. "Tue, 11 Aug 2026")
    val displayDateStr = remember(selectedDate) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = parser.parse(selectedDate) ?: Date()
            SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(dateObj)
        } catch (e: Exception) {
            selectedDate
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2)),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            // Date Selector Bar matching reference image 6.jpg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.changeDateByDays(-1) },
                    modifier = Modifier.testTag("prev_date_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", tint = Color(0xFF1E293B))
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.clickable {
                        // Open Android DatePicker
                        val cal = Calendar.getInstance()
                        val dpd = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance()
                                newCal.set(year, month, dayOfMonth)
                                val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newCal.time)
                                viewModel.selectedDate.value = formatted
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.show()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayDateStr, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }

                IconButton(
                    onClick = { viewModel.changeDateByDays(1) },
                    modifier = Modifier.testTag("next_date_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = Color(0xFF1E293B))
                }
            }
        }

        item {
            // Search & Filter Row matching reference image 6.jpg
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name...", color = Color(0xFF64748B), fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF334155)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF334155),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Rectangular Filter Pills matching reference image 6.jpg
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("All", "Present", "Absent", "Half", "Off").forEach { filter ->
                        val isSelected = statusFilter == filter
                        val isAll = filter == "All"
                        val bgColor = when {
                            isSelected && isAll -> Color(0xFFEDE9FE)
                            isSelected -> Color(0xFFEFF6FF)
                            else -> Color.White
                        }
                        val textColor = when {
                            isSelected && isAll -> Color(0xFF6D28D9)
                            isSelected -> Color(0xFF1D4ED8)
                            else -> Color(0xFF1E293B)
                        }
                        val borderColor = when {
                            isSelected && isAll -> Color(0xFFDDD6FE)
                            isSelected -> Color(0xFF1D4ED8)
                            else -> Color(0xFF475569)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            modifier = Modifier.clickable { statusFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            // Actions Row matching reference image 6.jpg: "Mark all present" + "List" / "Roll-call"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.markAllPresent() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDBEAFE)),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
                    modifier = Modifier.testTag("mark_all_present_btn")
                ) {
                    Text("Mark all present", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1D4ED8))
                }

                // Mode Selector Switch matching reference image 6.jpg
                Row(
                    modifier = Modifier
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("List", "Roll-call").forEach { m ->
                        val isSelected = mode == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { viewModel.attendanceMode.value = m }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = m,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status Legend Row matching reference image 6.jpg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Present", Color(0xFF16A34A))
                LegendItem("Absent", Color(0xFFDC2626))
                LegendItem("Half", Color(0xFFEA580C))
                LegendItem("Off", Color(0xFF334155))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (workers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.EventNote,
                        title = "No Attendance Records",
                        description = "No workers are available to mark attendance for this date. Add workers to your roster to start recording daily attendance.",
                        actionLabel = "+ Add Workers",
                        onActionClick = { viewModel.activeScreen.value = com.attendance.app.azaz.viewmodel.ScreenState.ADD_WORKER },
                        testTag = "empty_attendance_state"
                    )
                }
            }
        } else if (filteredWorkers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No workers found matching your search.", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else if (mode == "List") {
            // Mode 1: List View
            items(filteredWorkers, key = { it.id }) { worker ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    val record = attendanceRecords.find { it.workerId == worker.id }
                    AttendanceWorkerRowCard(
                        worker = worker,
                        currentRecord = record,
                        isAmountsHidden = isAmountsHidden,
                        onStatusChange = { newStatus ->
                            viewModel.setAttendance(worker.id, newStatus)
                        },
                        onOpenManualAmount = {
                            workerForManualAmount = worker
                        }
                    )
                }
            }
        } else {
            // Mode 2: Roll-call View (Screenshot 7)
            item {
                val total = filteredWorkers.size
                val safeIndex = rollCallIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
                val currentWorker = filteredWorkers.getOrNull(safeIndex)
                val record = currentWorker?.let { w -> attendanceRecords.find { it.workerId == w.id } }

                if (currentWorker != null) {
                    RollCallCard(
                        worker = currentWorker,
                        currentRecord = record,
                        isAmountsHidden = isAmountsHidden,
                        index = safeIndex + 1,
                        total = total,
                        onStatusSelected = { status ->
                            viewModel.setAttendance(currentWorker.id, status)
                            if (safeIndex < total - 1) {
                                viewModel.rollCallIndex.value = safeIndex + 1
                            }
                        },
                        onOpenManualAmount = {
                            workerForManualAmount = currentWorker
                        },
                        onPrevious = {
                            if (safeIndex > 0) viewModel.rollCallIndex.value = safeIndex - 1
                        },
                        onSkip = {
                            if (safeIndex < total - 1) viewModel.rollCallIndex.value = safeIndex + 1
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        ) { }
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AttendanceWorkerRowCard(
    worker: Worker,
    currentRecord: AttendanceRecord?,
    isAmountsHidden: Boolean = false,
    onStatusChange: (String) -> Unit,
    onOpenManualAmount: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_row_${worker.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Header: Avatar + Worker Name & Wage + Edit Amount Button matching 6.jpg
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.name.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worker.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val wageAmount = if (isAmountsHidden) "••••" else worker.wageRate.toInt().toString()
                    val unitStr = if (worker.wageType.equals("Daily", ignoreCase = true)) "/day" else "/mo"
                    Text(
                        text = "₹$wageAmount$unitStr",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Manual Amount / Notes Action Button Icon matching 6.jpg
                IconButton(
                    onClick = onOpenManualAmount,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Set Manual Amount",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Row: Status Buttons matching reference image 6.jpg ([ P ] [ A ] [ ½ ] [ O ])
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusPill(
                    label = "P",
                    isSelected = currentRecord?.status == "P",
                    bgActiveColor = Color(0xFFDCFCE7),
                    borderActiveColor = Color(0xFF16A34A),
                    textActiveColor = Color(0xFF15803D),
                    modifier = Modifier.weight(1f)
                ) { onStatusChange("P") }

                StatusPill(
                    label = "A",
                    isSelected = currentRecord?.status == "A",
                    bgActiveColor = Color(0xFFFEE2E2),
                    borderActiveColor = Color(0xFFDC2626),
                    textActiveColor = Color(0xFFB91C1C),
                    modifier = Modifier.weight(1f)
                ) { onStatusChange("A") }

                StatusPill(
                    label = "½",
                    isSelected = currentRecord?.status == "1/2",
                    bgActiveColor = Color(0xFFFFEDD5),
                    borderActiveColor = Color(0xFFEA580C),
                    textActiveColor = Color(0xFFC2410C),
                    modifier = Modifier.weight(1f)
                ) { onStatusChange("1/2") }

                StatusPill(
                    label = "O",
                    isSelected = currentRecord?.status == "O",
                    bgActiveColor = Color(0xFFE2E8F0),
                    borderActiveColor = Color(0xFF334155),
                    textActiveColor = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) { onStatusChange("O") }
            }

            // Display badge if custom manual amount is set for this day
            if (currentRecord?.customAmount != null && currentRecord.customAmount > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenManualAmount)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🏷️ Custom Pay: ₹${currentRecord.customAmount.toInt()} (${
                                when (currentRecord.status) {
                                    "P" -> "Present"
                                    "A" -> "Absent"
                                    "1/2" -> "Half-day"
                                    "O" -> "Off"
                                    else -> currentRecord.status
                                }
                            })",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Manual Amount",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Display Note badge if note exists
            if (!currentRecord?.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenManualAmount)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("📝 Note: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                            Text(
                                text = currentRecord?.notes ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isSelected: Boolean,
    bgActiveColor: Color,
    borderActiveColor: Color,
    textActiveColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) bgActiveColor else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderActiveColor else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (isSelected) textActiveColor else Color(0xFF334155)
        )
    }
}

@Composable
fun RollCallCard(
    worker: Worker,
    currentRecord: AttendanceRecord?,
    isAmountsHidden: Boolean = false,
    index: Int,
    total: Int,
    onStatusSelected: (String) -> Unit,
    onOpenManualAmount: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar (e.g. 1/1)
        Text("$index/$total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { index.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF1E3A8A),
            trackColor = Color(0xFFCBD5E1)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Big Avatar Circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFC8E6C9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = worker.name.take(1).uppercase(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(worker.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(2.dp))
        val statusText = when (currentRecord?.status) {
            "P" -> "Present"
            "A" -> "Absent"
            "1/2" -> "Half-day"
            "O" -> "Off"
            else -> "Not Marked"
        }
        val wageRateStr = if (isAmountsHidden) "₹••••" else "₹${worker.wageRate.toInt()}"
        val customAmtText = if (currentRecord?.customAmount != null && currentRecord.customAmount > 0.0) {
            val customStr = if (isAmountsHidden) "₹••••" else "₹${currentRecord.customAmount.toInt()}"
            " · Manual: $customStr"
        } else ""
        Text("$wageRateStr/mo · $statusText$customAmtText", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))

        Spacer(modifier = Modifier.height(20.dp))

        // 2x2 Big Buttons Grid (Screenshot 7)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RollCallBigButton(
                label = "✓ Present",
                isSelected = currentRecord?.status == "P",
                bgActiveColor = PresentGreenBg,
                textColor = PresentGreenText,
                borderColor = PresentGreenText,
                modifier = Modifier.weight(1f),
                onClick = { onStatusSelected("P") }
            )
            RollCallBigButton(
                label = "✕ Absent",
                isSelected = currentRecord?.status == "A",
                bgActiveColor = AbsentRedBg,
                textColor = AbsentRedText,
                borderColor = AbsentRedText,
                modifier = Modifier.weight(1f),
                onClick = { onStatusSelected("A") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RollCallBigButton(
                label = "Half",
                isSelected = currentRecord?.status == "1/2",
                bgActiveColor = HalfOrangeBg,
                textColor = HalfOrangeText,
                borderColor = HalfOrangeText,
                modifier = Modifier.weight(1f),
                onClick = { onStatusSelected("1/2") }
            )
            RollCallBigButton(
                label = "Off",
                isSelected = currentRecord?.status == "O",
                bgActiveColor = OffGrayBg,
                textColor = OffGrayText,
                borderColor = OffGrayText,
                modifier = Modifier.weight(1f),
                onClick = { onStatusSelected("O") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual Amount Action Button
        OutlinedButton(
            onClick = onOpenManualAmount,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF1E3A8A))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    currentRecord?.customAmount != null && currentRecord.customAmount > 0.0 && !currentRecord.notes.isNullOrBlank() ->
                        "₹${currentRecord.customAmount.toInt()} • 📝 ${currentRecord.notes}"
                    currentRecord?.customAmount != null && currentRecord.customAmount > 0.0 ->
                        "Manual Amount: ₹${currentRecord.customAmount.toInt()} (Tap to Edit)"
                    !currentRecord?.notes.isNullOrBlank() ->
                        "📝 Note: ${currentRecord.notes} (Tap to Edit)"
                    else -> "➕ Set Custom Amount (₹) / Add Note"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Previous / Skip navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            TextButton(onClick = onSkip) {
                Text("Skip", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun RollCallBigButton(
    label: String,
    isSelected: Boolean,
    bgActiveColor: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) bgActiveColor else Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isSelected) borderColor else Color(0xFFCBD5E1)),
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) textColor else Color(0xFF334155)
            )
        }
    }
}

@Composable
fun ManualAmountDialog(
    worker: Worker,
    currentRecord: AttendanceRecord?,
    selectedDate: String,
    onDismiss: () -> Unit,
    onSave: (status: String, amount: Double, note: String) -> Unit,
    onReset: () -> Unit
) {
    val fullDailyWage = remember(worker) { com.attendance.app.azaz.util.WageCalculator.calculateDailyBaseRate(worker).toInt() }
    val halfDailyWage = remember(worker) { (com.attendance.app.azaz.util.WageCalculator.calculateDailyBaseRate(worker) * worker.halfDayPayFactor).toInt() }

    var selectedStatus by remember { mutableStateOf(currentRecord?.status ?: "P") }
    var notesInput by remember { mutableStateOf(currentRecord?.notes ?: "") }
    var amountInput by remember {
        mutableStateOf(
            if (currentRecord?.customAmount != null && currentRecord.customAmount > 0.0) {
                currentRecord.customAmount.toInt().toString()
            } else {
                when (currentRecord?.status) {
                    "P" -> fullDailyWage.toString()
                    "1/2" -> halfDailyWage.toString()
                    "A", "O" -> "0"
                    else -> fullDailyWage.toString()
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Set Attendance & Note", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${worker.name} • $selectedDate", fontSize = 13.sp, color = Color(0xFF64748B))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("1. Select Attendance Status:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val statuses = listOf(
                        "P" to "Present",
                        "A" to "Absent",
                        "1/2" to "Half-day",
                        "O" to "Off"
                    )
                    statuses.forEach { (code, label) ->
                        val isSel = selectedStatus == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1E3A8A) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedStatus = code
                                    amountInput = when (code) {
                                        "P" -> fullDailyWage.toString()
                                        "1/2" -> halfDailyWage.toString()
                                        else -> "0"
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSel) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("2. Enter Manual Wage / Pay Amount (₹):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("e.g. 100, 250, 500") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Quick Preset Chips:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = {
                            selectedStatus = "P"
                            amountInput = fullDailyWage.toString()
                        },
                        label = { Text("Full Rate ₹$fullDailyWage", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = {
                            selectedStatus = "1/2"
                            amountInput = halfDailyWage.toString()
                        },
                        label = { Text("Half Rate ₹$halfDailyWage", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    listOf("0", "100", "200", "250", "500").forEach { preset ->
                        AssistChip(
                            onClick = { amountInput = preset },
                            label = { Text("₹$preset", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("3. Add Note / Remarks (नोट / रिमार्क जोड़ें):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("e.g. Late by 30 mins, Advance paid, Site duty...") },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Quick Note Suggestions:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("Late 30 min", "Advance Paid", "Site Duty", "Medical Leave", "Personal Work", "Overtime 2 Hrs").forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                notesInput = if (notesInput.isBlank()) suggestion else "$notesInput, $suggestion"
                            },
                            label = { Text(suggestion, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    onSave(selectedStatus, amt, notesInput.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Save Attendance & Note", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if ((currentRecord?.customAmount != null && currentRecord.customAmount > 0.0) || !currentRecord?.notes.isNullOrBlank()) {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
