package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.HaazriViewModel

@Composable
fun AddWorkerScreen(
    viewModel: HaazriViewModel,
    initialName: String = "",
    initialPhone: String = "",
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var wageType by remember { mutableStateOf("Monthly") }
    var wageRate by remember { mutableStateOf("") }
    var overtimeRate by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }

    // Wage Rules State
    var isWageRulesExpanded by remember { mutableStateOf(true) }
    var hajariMultiplier by remember { mutableStateOf("Off") }
    var overtimeMultiplier by remember { mutableStateOf("1.5x") }
    var lateFine by remember { mutableStateOf("") }
    var lateGracePeriod by remember { mutableStateOf("") }
    var halfDayPayFactor by remember { mutableStateOf("0.5") }
    var notes by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE)) // Warm background as in screenshot
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Name field
        Text("Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Ramesh Kumar", color = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("worker_name_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Phone field
        Text("Phone (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = { Text("10-digit mobile", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("worker_phone_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Wage type selector (Daily, Weekly, Monthly)
        Text("Wage type", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Daily", "Weekly", "Monthly").forEach { type ->
                val isSelected = wageType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { wageType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Wage rate input
        Text("Wage rate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = wageRate,
            onValueChange = { wageRate = it },
            placeholder = { Text(if (wageType == "Daily") "₹ per day" else if (wageType == "Weekly") "₹ per week" else "₹ per month", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("worker_wage_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Overtime rate
        Text("Overtime rate (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = overtimeRate,
            onValueChange = { overtimeRate = it },
            placeholder = { Text("₹ per hour", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // UPI ID
        Text("UPI ID (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            placeholder = { Text("name@bank", color = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Expandable Wage rules Card (Exact Screenshot 3 design)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isWageRulesExpanded = !isWageRulesExpanded }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE0E7FF), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wage rules", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                        Text("Hajari, overtime, late fine & half-day", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Icon(
                        imageVector = if (isWageRulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.Gray
                    )
                }

                AnimatedVisibility(visible = isWageRulesExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Divider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Hajari multiplier
                        Text("Hajari multiplier", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Off", "2x", "3.5x", "4.75x", "Custom").forEach { mult ->
                                val isSelected = hajariMultiplier == mult
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFF253B80) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { hajariMultiplier = mult }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mult,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Late fine
                        Text("Late fine (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = lateFine,
                            onValueChange = { lateFine = it },
                            placeholder = { Text("₹ per late day", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color.DarkGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Late grace period
                        Text("Late grace period (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = lateGracePeriod,
                            onValueChange = { lateGracePeriod = it },
                            placeholder = { Text("minutes", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Half-day pay factor
                        Text("Half-day pay factor (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = halfDayPayFactor,
                            onValueChange = { halfDayPayFactor = it },
                            placeholder = { Text("0.5 = half wage (default)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Notes
                        Text("Notes (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Anything to remember", color = Color.Gray) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Action Buttons (Exact Screenshot 3 design)
        OutlinedButton(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.saveWorker(
                        name = name,
                        phone = phone,
                        wageType = wageType,
                        wageRate = wageRate.toDoubleOrNull() ?: 0.0,
                        overtimeRate = overtimeRate.toDoubleOrNull() ?: 0.0,
                        upiId = upiId,
                        hajariMultiplier = hajariMultiplier,
                        overtimeMultiplier = overtimeMultiplier,
                        lateFine = lateFine.toDoubleOrNull() ?: 0.0,
                        lateGracePeriodMinutes = lateGracePeriod.toIntOrNull() ?: 0,
                        halfDayPayFactor = halfDayPayFactor.toDoubleOrNull() ?: 0.5,
                        notes = notes
                    )
                    // Reset fields for next
                    name = ""
                    phone = ""
                    wageRate = ""
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF253B80))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save & add next", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF253B80))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.saveWorker(
                        name = name,
                        phone = phone,
                        wageType = wageType,
                        wageRate = wageRate.toDoubleOrNull() ?: 0.0,
                        overtimeRate = overtimeRate.toDoubleOrNull() ?: 0.0,
                        upiId = upiId,
                        hajariMultiplier = hajariMultiplier,
                        overtimeMultiplier = overtimeMultiplier,
                        lateFine = lateFine.toDoubleOrNull() ?: 0.0,
                        lateGracePeriodMinutes = lateGracePeriod.toIntOrNull() ?: 0,
                        halfDayPayFactor = halfDayPayFactor.toDoubleOrNull() ?: 0.5,
                        notes = notes
                    )
                    onDone()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B80)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_worker_btn")
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save worker", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
