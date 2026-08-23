package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

@Composable
fun DataSafetyScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permanent Data Deletion", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete all local attendance data, workers, cashbook records, and preferences?\n\nThis action cannot be undone. Please ensure you have exported a backup if needed.",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        viewModel.clearAllData()
                        Toast.makeText(context, "All data has been deleted successfully", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Delete Everything", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmationDialog = false }) {
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
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Data Safety & Security",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Google Play Data Safety Standards",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Transparency on how Attendance App collects, handles, protects, and allows you to delete your data.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        // Summary Badges Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Play Store Data Safety Highlights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                SafetyBadgeRow(
                    icon = Icons.Outlined.Lock,
                    title = "Data is Encrypted in Transit & at Rest",
                    desc = "Device sandbox and TLS encryption protocols safeguard all transmissions."
                )
                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                SafetyBadgeRow(
                    icon = Icons.Outlined.DoNotDisturbOn,
                    title = "No Data Shared with Third-Party Advertisers",
                    desc = "We do not sell personal or worker records to ad networks or data aggregators."
                )
                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                SafetyBadgeRow(
                    icon = Icons.Outlined.Storage,
                    title = "Local-First Storage (Room SQLite)",
                    desc = "Your staff database lives on your local Android device."
                )
                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                SafetyBadgeRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "You Can Request Data Deletion Anytime",
                    desc = "Provides one-tap local wipe and server data deletion requests."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Permissions Breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Detailed Android Permissions Usage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                PermissionExplainer(
                    name = "Location (ACCESS_FINE_LOCATION)",
                    status = "Optional • When In Use Only",
                    reason = "Used exclusively to check whether the supervisor is inside the designated site Geofence boundary during attendance check-in."
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionExplainer(
                    name = "Notifications (POST_NOTIFICATIONS)",
                    status = "User Configurable",
                    reason = "Used for daily shift start/end attendance reminders, backup reminders, and wage payout alerts."
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionExplainer(
                    name = "Contacts (READ_CONTACTS)",
                    status = "Optional",
                    reason = "Used when tapping 'Select from Contacts' to quickly import a worker's phone number without manual typing."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Data Deletion Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Account & Data Deletion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF991B1B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "As required by Google Play Developer policies, you have complete ownership to wipe or request deletion of all associated data.",
                    fontSize = 13.sp,
                    color = Color(0xFF7F1D1D),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showDeleteConfirmationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_data_btn")
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe All Local App Data", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:azazmadkiya@gmail.com?subject=Account%20and%20Data%20Deletion%20Request")
                        }
                        context.startActivity(Intent.createChooser(intent, "Request Cloud Data Deletion"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Cloud Account Deletion", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SafetyBadgeRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
            Text(desc, fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
        }
    }
}

@Composable
fun PermissionExplainer(
    name: String,
    status: String,
    reason: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
            Surface(
                color = Color(0xFFEEF2FF),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(reason, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)
    }
}
