package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
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
import com.example.util.NotificationHelper
import com.example.viewmodel.HaazriViewModel

@Composable
fun NotificationsSetupScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val settings by viewModel.notificationSettings.collectAsState()

    var hasPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            NotificationHelper.createNotificationChannels(context)
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    var dailyReminder by remember(settings) { mutableStateOf(settings?.dailyReminderEnabled ?: true) }
    var reminderTime by remember(settings) { mutableStateOf(settings?.reminderTime ?: "09:00 AM") }
    var missedCheckout by remember(settings) { mutableStateOf(settings?.missedCheckoutNudge ?: true) }
    var weeklyReport by remember(settings) { mutableStateOf(settings?.weeklyReportEnabled ?: true) }

    var showTestNotificationBanner by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Automated Notification System", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Configure scheduled alerts and reminders for site managers and workers", fontSize = 13.sp, color = Color(0xFF64748B))

        Spacer(modifier = Modifier.height(16.dp))

        // System Notification Permission Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (hasPermission) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_permission_status_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasPermission) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasPermission) "Notification Permission Active" else "Notification Permission Needed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (hasPermission) Color(0xFF065F46) else Color(0xFF92400E)
                        )
                        Text(
                            text = if (hasPermission)
                                "System alerts & scheduled morning reminders are active"
                            else
                                "Grant permission so the app can send daily attendance reminders",
                            fontSize = 12.sp,
                            color = if (hasPermission) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }

                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("grant_notification_permission_btn")
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Push Notification Banner Preview
        AnimatedVisibility(visible = showTestNotificationBanner) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF253B80)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("haazriPro · Just now", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("⏰ Morning Attendance Reminder", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("Time to mark today's attendance for your workers!", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }

                    Button(
                        onClick = { showTestNotificationBanner = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Notification Options Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Daily Attendance Reminder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Attendance Reminder", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Text("Send automated alert to open roll-call every morning", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = dailyReminder,
                        onCheckedChange = {
                            dailyReminder = it
                            viewModel.saveNotificationSetting(it, reminderTime, missedCheckout, weeklyReport, false)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF253B80))
                    )
                }

                if (dailyReminder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = {
                            reminderTime = it
                            viewModel.saveNotificationSetting(dailyReminder, it, missedCheckout, weeklyReport, false)
                        },
                        label = { Text("Reminder Time") },
                        placeholder = { Text("e.g. 09:00 AM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Quick Presets:", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("08:00 AM", "09:00 AM", "10:00 AM").forEach { preset ->
                            val isSelected = reminderTime.trim().equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    reminderTime = preset
                                    viewModel.saveNotificationSetting(dailyReminder, preset, missedCheckout, weeklyReport, false)
                                    Toast.makeText(context, "Daily reminder scheduled for $preset", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(preset, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF253B80),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))

                // Missed Check-out Nudge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Missed Check-out Nudge", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Text("Evening alert at 07:00 PM if check-out status is pending", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = missedCheckout,
                        onCheckedChange = {
                            missedCheckout = it
                            viewModel.saveNotificationSetting(dailyReminder, reminderTime, it, weeklyReport, false)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF253B80))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))

                // Weekly Summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Weekly Automated Summary Report", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Text("Receive weekly wage calculation digest every Saturday", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = weeklyReport,
                        onCheckedChange = {
                            weeklyReport = it
                            viewModel.saveNotificationSetting(dailyReminder, reminderTime, missedCheckout, it, false)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF253B80))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Automation Schedules Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF253B80),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Schedule Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("• Daily Morning Attendance:", fontSize = 13.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (dailyReminder) "$reminderTime (Active)" else "Disabled",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyReminder) Color(0xFF15803D) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("• Evening Check-out Nudge:", fontSize = 13.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (missedCheckout) "07:00 PM (Active)" else "Disabled",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (missedCheckout) Color(0xFF15803D) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("• Weekly Payout Digest:", fontSize = 13.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (weeklyReport) "Saturdays 06:00 PM (Active)" else "Disabled",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (weeklyReport) Color(0xFF15803D) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.saveNotificationSetting(dailyReminder, reminderTime, missedCheckout, weeklyReport, false)
                    Toast.makeText(context, "Automated alarms synchronized with Android AlarmManager!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sync_alarms_btn")
            ) {
                Text("Sync Schedules", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF253B80))
            }

            Button(
                onClick = {
                    if (!hasPermission) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    // Send Android System Notification
                    NotificationHelper.showNotification(
                        context = context,
                        title = "⏰ Daily Attendance Reminder",
                        message = "Time to mark today's attendance for your workers in haazriPro!"
                    )
                    showTestNotificationBanner = true
                    Toast.makeText(context, "Test notification triggered!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B80)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("trigger_test_notification_btn")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

