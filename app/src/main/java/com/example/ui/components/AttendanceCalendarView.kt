package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceCalendarView(
    year: Int,
    month: Int, // 0-11
    attendanceRecords: List<AttendanceRecord>,
    onDayClick: (Int, AttendanceRecord?) -> Unit = { _, _ -> }
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday, 1 for Monday etc.

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    val currentMonthLabel = monthYearFormat.format(cal.time)

    // Helper to get record for a specific day
    fun getRecordForDay(day: Int): AttendanceRecord? {
        val dateCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dateStr = sdf.format(dateCal.time)
        return attendanceRecords.find { it.date == dateStr }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonthLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of week header
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val totalCells = daysInMonth + firstDayOfWeek
            val rows = Math.ceil(totalCells / 7.0).toInt()

            var currentDay = 1

            for (i in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (j in 0..6) {
                        if (i == 0 && j < firstDayOfWeek) {
                            // Empty cells before start of month
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else if (currentDay <= daysInMonth) {
                            val day = currentDay
                            val record = getRecordForDay(day)
                            
                            val bgColor = when (record?.status) {
                                "P" -> Color(0xFFDCFCE7) // Green light
                                "A" -> Color(0xFFFEE2E2) // Red light
                                "1/2" -> Color(0xFFFEF9C3) // Yellow light
                                "O" -> Color(0xFFF1F5F9) // Gray light
                                else -> Color.Transparent
                            }
                            
                            val textColor = when (record?.status) {
                                "P" -> Color(0xFF16A34A) // Green dark
                                "A" -> Color(0xFFDC2626) // Red dark
                                "1/2" -> Color(0xFFCA8A04) // Yellow dark
                                "O" -> Color(0xFF475569) // Gray dark
                                else -> Color(0xFF1E293B)
                            }
                            
                            val isToday = day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && 
                                          month == Calendar.getInstance().get(Calendar.MONTH) &&
                                          year == Calendar.getInstance().get(Calendar.YEAR)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) Color(0xFF3B82F6) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onDayClick(day, record) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                    if (record?.overtimeHours != null && record.overtimeHours > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(textColor)
                                        )
                                    }
                                }
                            }
                            currentDay++
                        } else {
                            // Empty cells after end of month
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            
            // Legend
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("P", "Present", Color(0xFFDCFCE7), Color(0xFF16A34A))
                LegendItem("A", "Absent", Color(0xFFFEE2E2), Color(0xFFDC2626))
                LegendItem("1/2", "Half", Color(0xFFFEF9C3), Color(0xFFCA8A04))
                LegendItem("O", "Off", Color(0xFFF1F5F9), Color(0xFF475569))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, desc: String, bgColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(desc, fontSize = 12.sp, color = Color(0xFF64748B))
    }
}
