package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import com.example.ui.components.EmptyStateView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Worker
import com.example.ui.theme.*
import com.example.viewmodel.HaazriViewModel
import com.example.viewmodel.ScreenState

@Composable
fun WorkersScreen(
    viewModel: HaazriViewModel,
    onWorkerClick: (Long) -> Unit
) {
    val workers by viewModel.workers.collectAsState()
    val searchQuery by viewModel.workerSearchQuery.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentAttendanceRecords by viewModel.currentAttendanceRecords.collectAsState()
    val isAmountsHidden by viewModel.isAmountsHidden.collectAsState()

    // Calculate Current Day Summary Stats
    val todayPresent = currentAttendanceRecords.count { it.status == "P" }
    val todayAbsent = currentAttendanceRecords.count { it.status == "A" }
    val todayHalf = currentAttendanceRecords.count { it.status == "1/2" }
    val todayUnmarked = (workers.size - currentAttendanceRecords.size).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Attendance Summary Card (Current Day)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workers_attendance_summary_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Summary Count Metrics Grid for Current Day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Total Staff
                        SummaryMetricBadge(
                            label = "Total",
                            count = "${workers.size}",
                            bgColor = Color(0xFFEFF6FF),
                            textColor = Color(0xFF1E3A8A),
                            modifier = Modifier.weight(1f)
                        )

                        // Present
                        SummaryMetricBadge(
                            label = "Present",
                            count = "$todayPresent",
                            bgColor = PresentGreenBg,
                            textColor = PresentGreenText,
                            modifier = Modifier.weight(1f)
                        )

                        // Absent
                        SummaryMetricBadge(
                            label = "Absent",
                            count = "$todayAbsent",
                            bgColor = AbsentRedBg,
                            textColor = AbsentRedText,
                            modifier = Modifier.weight(1f)
                        )

                        // Half Day
                        SummaryMetricBadge(
                            label = "Half Day",
                            count = "$todayHalf",
                            bgColor = HalfOrangeBg,
                            textColor = HalfOrangeText,
                            modifier = Modifier.weight(1f)
                        )

                        // Pending
                        SummaryMetricBadge(
                            label = "Pending",
                            count = "$todayUnmarked",
                            bgColor = OffGrayBg,
                            textColor = OffGrayText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search by name or phone bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.workerSearchQuery.value = it },
                placeholder = { Text("Search by name or phone", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFCBD5E1),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_worker_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Worker count subtitle
            Text(
                text = "${workers.size} ${if (workers.size == 1) "worker" else "workers"}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Workers List
            if (workers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.GroupAdd,
                        title = "No Workers Found",
                        description = "Start building your team roster! Add site workers to mark attendance, track daily wages, and calculate payroll.",
                        actionLabel = "+ Add First Worker",
                        onActionClick = { viewModel.activeScreen.value = ScreenState.ADD_WORKER },
                        testTag = "empty_workers_state"
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(workers, key = { it.id }) { worker ->
                        val record = currentAttendanceRecords.find { it.workerId == worker.id }
                        val isPending = record == null
                        WorkerCard(
                            worker = worker,
                            isPending = isPending,
                            isAmountsHidden = isAmountsHidden,
                            onClick = { onWorkerClick(worker.id) }
                        )
                    }
                }
            }
        }

        // Main Primary FAB: "+ Add worker"
        ExtendedFloatingActionButton(
            onClick = { viewModel.activeScreen.value = ScreenState.ADD_WORKER },
            containerColor = Color(0xFF253B80),
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = "Add worker") },
            text = { Text("Add worker", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .testTag("add_worker_main_fab")
        )
    }
}

@Composable
fun WorkerCard(
    worker: Worker,
    isPending: Boolean = false,
    isAmountsHidden: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("worker_item_${worker.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Circular initial avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isPending) Color(0xFFFEE2E2) else Color(0xFFC8E6C9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = worker.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPending) Color(0xFFDC2626) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Worker Name & Wage info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = worker.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPending) Color(0xFFDC2626) else Color(0xFF1E293B)
                    )
                    if (isPending) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Pending",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val wageRateStr = if (isAmountsHidden) "₹••••" else "₹${worker.wageRate.toInt()}"
                val wageText = when (worker.wageType) {
                    "Monthly" -> "Monthly · $wageRateStr/mo"
                    "Daily" -> "Daily · $wageRateStr/day"
                    else -> "Weekly · $wageRateStr/wk"
                }
                Text(
                    text = wageText,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Details",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun SummaryMetricBadge(
    label: String,
    count: String,
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
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
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
