package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
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
import com.example.viewmodel.HaazriViewModel

@Composable
fun GeofenceAdminScreen(viewModel: HaazriViewModel) {
    val geofenceConfig by viewModel.geofenceConfig.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val records by viewModel.currentAttendanceRecords.collectAsState()

    var officeName by remember(geofenceConfig) { mutableStateOf(geofenceConfig?.officeName ?: "Central Office HQ") }
    var latStr by remember(geofenceConfig) { mutableStateOf(geofenceConfig?.latitude?.toString() ?: "28.6139") }
    var lngStr by remember(geofenceConfig) { mutableStateOf(geofenceConfig?.longitude?.toString() ?: "77.2090") }
    var radiusStr by remember(geofenceConfig) { mutableStateOf(geofenceConfig?.radiusMeters?.toInt()?.toString() ?: "200") }
    var isEnabled by remember(geofenceConfig) { mutableStateOf(geofenceConfig?.isEnabled ?: true) }

    // Simulation state for live verification test
    var simLat by remember { mutableStateOf("28.6141") }
    var simLng by remember { mutableStateOf("77.2092") }

    val officeLat = latStr.toDoubleOrNull() ?: 28.6139
    val officeLng = lngStr.toDoubleOrNull() ?: 77.2090
    val radius = radiusStr.toFloatOrNull() ?: 200f

    val simLatVal = simLat.toDoubleOrNull() ?: 28.6141
    val simLngVal = simLng.toDoubleOrNull() ?: 77.2092

    val isVerifiedInside = viewModel.isWithinGeofence(simLatVal, simLngVal, officeLat, officeLng, radius)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Geofence Location Rules", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Restrict attendance check-ins to physical office site boundary", fontSize = 13.sp, color = Color(0xFF64748B))

        Spacer(modifier = Modifier.height(16.dp))

        // Geofence Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1E3A8A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Office Geofence Boundary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF253B80))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = officeName,
                    onValueChange = { officeName = it },
                    label = { Text("Office Site Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lngStr,
                        onValueChange = { lngStr = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = radiusStr,
                    onValueChange = { radiusStr = it },
                    label = { Text("Allowed Radius (meters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.saveGeofence(officeName, officeLat, officeLng, radius, isEnabled)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B80)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_geofence_btn")
                ) {
                    Text("Save Geofence Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Geofence Tester Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF1E3A8A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Live Check-in Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E3A8A))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            simLat = "28.6141"
                            simLng = "77.2092"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Inside (40m)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            simLat = "28.6500"
                            simLng = "77.2500"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Outside (5km)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isVerifiedInside) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (isVerifiedInside) Icons.Default.CheckCircle else Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (isVerifiedInside) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isVerifiedInside) "Verification Passed: Inside Office Radius" else "Verification Failed: Outside Office Radius",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isVerifiedInside) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = if (isVerifiedInside) "Worker attendance will automatically mark as Geofence Verified" else "Check-in blocked or flagged for admin review",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real-time Employee Tracking Status List
        Text("Admin Real-Time Employee Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(8.dp))

        workers.forEach { worker ->
            val record = records.find { it.workerId == worker.id }
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(worker.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Status: ${record?.status ?: "Unmarked"}", fontSize = 12.sp, color = Color.Gray)
                    }

                    if (record?.isGeofenceVerified == true) {
                        Text("📍 Verified On-Site", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                    } else {
                        Text("⚪ Remote/Offsite", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
