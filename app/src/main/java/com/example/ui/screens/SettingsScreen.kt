

package com.example.ui.screens

import kotlinx.coroutines.launch
import com.example.util.AppLockManager


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.HaazriViewModel
import com.example.viewmodel.ScreenState

@Composable
fun SettingsScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val userName by viewModel.loggedInUserName.collectAsState()
    val companyName by viewModel.loggedInCompanyName.collectAsState()
    val userPhone by viewModel.loggedInPhone.collectAsState()
    val userEmail by viewModel.loggedInEmail.collectAsState()
    val firebaseUserInfo by viewModel.firebaseUserInfo.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val appLockManager = remember { AppLockManager(context) }
    val savedPin by appLockManager.getPin().collectAsState(initial = null)
    val isBiometricEnabled by appLockManager.isBiometricEnabled().collectAsState(initial = false)
    val isAppLockEnabled = savedPin != null

    val isAmountsHidden by viewModel.isAmountsHidden.collectAsState()

    val workers by viewModel.workers.collectAsState()
    val attendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val cashbookEntries by viewModel.cashbookEntries.collectAsState()

    var hideAmounts by remember { mutableStateOf(notificationSettings?.hideAmounts ?: false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDataInfoDialog by remember { mutableStateOf(false) }
    var showAppLockDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // 1. Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(userName) }
        var editCompany by remember { mutableStateOf(companyName) }
        var editPhone by remember { mutableStateOf(userPhone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCompany,
                        onValueChange = { editCompany = it },
                        label = { Text("Company / Site Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_company_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_phone_field"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editCompany.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateUserProfile(editName, editCompany, editPhone)
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Language Dialog
    if (showLanguageDialog) {
        val languages = listOf(
            "English",
            "Hindi (हिंदी)",
            "Gujarati (ગુજરાતી)",
            "Marathi (मराठी)",
            "Hinglish"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select App Language", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(lang)
                                    showLanguageDialog = false
                                    Toast.makeText(context, "Language changed to $lang", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedLanguage == lang),
                                onClick = {
                                    viewModel.updateLanguage(lang)
                                    showLanguageDialog = false
                                    Toast.makeText(context, "Language changed to $lang", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Your Data Info Dialog
    if (showDataInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDataInfoDialog = false },
            title = { Text("Your App Data & Storage", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "All your worker profiles, attendance logs, and financial cashbook records are securely stored on your local device (Room SQLite DB).",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("• Workers saved: ${workers.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Attendance logs: ${attendanceRecords.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Cashbook transactions: ${cashbookEntries.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDataInfoDialog = false
                        viewModel.activeScreen.value = ScreenState.BACKUP_RESTORE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Backup & Export Data", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDataInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 4. App Lock Dialog
    if (showAppLockDialog) {
        val coroutineScope = rememberCoroutineScope()
        var tempEnabled by remember { mutableStateOf(isAppLockEnabled) }
        var tempBiometric by remember { mutableStateOf(isBiometricEnabled) }
        var tempPin by remember { mutableStateOf(savedPin ?: "") }

        AlertDialog(
            onDismissRequest = { showAppLockDialog = false },
            title = { Text("App Security Lock", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable App Security", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = { tempEnabled = it }
                        )
                    }

                    if (tempEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = tempPin,
                            onValueChange = { if (it.length <= 4) tempPin = it },
                            label = { Text("Set 4-Digit Security PIN") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_pin_field"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Fingerprint Unlock", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Switch(
                                checked = tempBiometric,
                                onCheckedChange = { tempBiometric = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (tempEnabled) {
                                if (tempPin.length == 4) {
                                    appLockManager.setPin(tempPin)
                                    appLockManager.setBiometricEnabled(tempBiometric)
                                    showAppLockDialog = false
                                    Toast.makeText(context, "App lock enabled with PIN", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                appLockManager.clearLock()
                                showAppLockDialog = false
                                Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Save Settings", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAppLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out of $companyName?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // User Profile Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEditProfileDialog = true }
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (userEmail.isNotBlank()) "$companyName • $userEmail" else "$companyName • +91 $userPhone",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = Color.White)
            }
        }

        // Section: Account
        SettingsSectionHeader("Account")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    title = "Edit Profile",
                    subtitle = "Name, company name & mobile number",
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier.testTag("edit_profile_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.testTag("language_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Logout,
                    title = "Logout / Switch Account",
                    subtitle = "Log out from $companyName",
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.testTag("logout_row")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Geofence & Location Tracking
        SettingsSectionHeader("Geofence & Tracking")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Geofence Settings",
                    subtitle = "Set office location & radius for automatic location check-in",
                    onClick = { viewModel.activeScreen.value = ScreenState.GEOFENCE_ADMIN },
                    modifier = Modifier.testTag("geofence_settings_row")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Privacy & Security
        SettingsSectionHeader("Privacy & Security")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    title = "Your data",
                    subtitle = "Database status, storage & backup info",
                    onClick = { showDataInfoDialog = true },
                    modifier = Modifier.testTag("your_data_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "App Lock",
                    subtitle = if (isAppLockEnabled) "Enabled (PIN: $savedPin)" else "Disabled (PIN / Security lock)",
                    onClick = { showAppLockDialog = true },
                    modifier = Modifier.testTag("app_lock_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Reminders & Notifications",
                    subtitle = "Attendance, settlement & backup nudges",
                    onClick = { viewModel.activeScreen.value = ScreenState.NOTIFICATIONS_SETUP },
                    modifier = Modifier.testTag("reminders_settings_row")
                )
                Divider(color = Color(0xFFF1F5F9))

                // Toggle Switch Row: Hide amounts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide amounts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Text("Mask money here & in the app switcher", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isAmountsHidden,
                        onCheckedChange = {
                            viewModel.toggleHideAmounts()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF253B80)),
                        modifier = Modifier.testTag("settings_hide_amounts_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Reports & Payroll (मासिक रिपोर्ट व स्टेटमेंट)
        SettingsSectionHeader("Reports & Payroll")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Outlined.Assessment,
                    title = "All Staff Monthly Report",
                    subtitle = "सभी कर्मचारियों की मासिक रिपोर्ट • PDF & Excel Export",
                    onClick = { viewModel.activeScreen.value = ScreenState.MONTHLY_REPORT },
                    modifier = Modifier.testTag("all_staff_monthly_report_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Backup & Restore",
                    subtitle = "Export, share & restore database files",
                    onClick = { viewModel.activeScreen.value = ScreenState.BACKUP_RESTORE },
                    modifier = Modifier.testTag("backup_restore_row")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Legal & Google Play Policies (Play Store Compliance)
        SettingsSectionHeader("Legal & App Policies")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Data protection, Geofencing & privacy policy",
                    onClick = { viewModel.activeScreen.value = ScreenState.PRIVACY_POLICY },
                    modifier = Modifier.testTag("privacy_policy_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Gavel,
                    title = "Terms & Conditions",
                    subtitle = "Terms of service & app usage agreement",
                    onClick = { viewModel.activeScreen.value = ScreenState.TERMS_OF_SERVICE },
                    modifier = Modifier.testTag("terms_of_service_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Data Safety & Permissions",
                    subtitle = "Play Store compliance, permissions & wipe data",
                    onClick = { viewModel.activeScreen.value = ScreenState.DATA_SAFETY },
                    modifier = Modifier.testTag("data_safety_row")
                )
                Divider(color = Color(0xFFF1F5F9))
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "About App & Developer",
                    subtitle = "Attendance App v1.2.0 • Developer: Azaz Madkiya",
                    onClick = { viewModel.activeScreen.value = ScreenState.ABOUT_APP },
                    modifier = Modifier.testTag("about_app_row")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Account Session
        SettingsSectionHeader("Account Session")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button_last")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout ($companyName)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Developer Info Footer Card (At the very bottom of Settings tab)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.activeScreen.value = ScreenState.ABOUT_APP }
                .testTag("developer_info_footer")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = "Verified Developer",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Developer By Azazmadkiya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E3A8A),
                        letterSpacing = 0.3.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Attendance App • Pro Edition v1.2.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3B82F6)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color(0xFF475569),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFFEEF2FF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
    }
}
