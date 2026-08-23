package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.Composable
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
fun PrivacyPolicyScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val policyUrl = "https://azazmadkiya.github.io/Attendance-App/privacy-policy.html"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header Banner Card
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
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Effective Date: August 2026",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Your privacy and data security are our top priorities. Attendance App (Haazri Pro) is designed with a privacy-first, local-storage architecture.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Privacy Policy URL", policyUrl))
                    Toast.makeText(context, "Privacy Policy URL copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("copy_policy_url_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))
                    context.startActivity(Intent.createChooser(intent, "Open Privacy Policy"))
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_web_policy_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Web Policy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        // Section 1: Introduction
        PolicyCard(
            title = "1. Introduction & Overview",
            icon = Icons.Outlined.Info
        ) {
            Text(
                text = "Attendance App (\"Haazri Pro\", \"we\", \"us\", or \"our\"), developed by Azaz Madkiya, is a workforce attendance and payroll management utility for contractors, site supervisors, and business owners.\n\nThis Privacy Policy governs the collection, use, and disclosure of information when you use our mobile application.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: Data Collection
        PolicyCard(
            title = "2. Information We Collect",
            icon = Icons.Outlined.Storage
        ) {
            PolicyBullet(
                title = "User & Business Profile:",
                desc = "Name, company/firm name, and mobile number provided during optional login/profile setup."
            )
            PolicyBullet(
                title = "Worker & Staff Records:",
                desc = "Worker names, wage rates (per day / per month / per hour / per piece), contact numbers, and daily attendance statuses (Present, Absent, Half-day, Overtime)."
            )
            PolicyBullet(
                title = "Cashbook & Financial Logs:",
                desc = "Salary advance payments, bonuses, deductions, and petty cash transactions."
            )
            PolicyBullet(
                title = "Authentication Credentials:",
                desc = "When signing in with Google, we authenticate securely using Google Sign-In / Firebase Auth token. We do not store your Google password."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 3: Location Data & Geofence Policy
        PolicyCard(
            title = "3. Location Data & Geofencing",
            icon = Icons.Outlined.LocationOn
        ) {
            Text(
                text = "Our app offers an optional Geofenced Attendance feature to verify if employees or supervisors are within the designated work site or office radius.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            PolicyBullet(
                title = "On-Device Location Verification:",
                desc = "GPS coordinates are accessed strictly during active attendance logging to compute proximity to the preset work site."
            )
            PolicyBullet(
                title = "No Background Tracking:",
                desc = "We DO NOT track your continuous location in the background or monitor your movements when the app is closed."
            )
            PolicyBullet(
                title = "No Sale of Location Data:",
                desc = "Location coordinates are never sold, rented, or shared with third-party advertising brokers."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 4: Device Permissions
        PolicyCard(
            title = "4. Device Permissions & Purpose",
            icon = Icons.Outlined.Key
        ) {
            PolicyBullet(
                title = "POST_NOTIFICATIONS:",
                desc = "To deliver daily attendance marking reminders, salary settlement alerts, and local backup notifications."
            )
            PolicyBullet(
                title = "ACCESS_FINE / COARSE_LOCATION:",
                desc = "Required strictly for verifying site boundaries in Geofence Attendance mode."
            )
            PolicyBullet(
                title = "READ_CONTACTS (Optional):",
                desc = "Allows you to quickly pick a phone number from your contact book when adding a new worker."
            )
            PolicyBullet(
                title = "STORAGE / EXPORT:",
                desc = "To generate, save, and share monthly attendance and payroll reports in PDF and Excel formats."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 5: Data Storage & Security
        PolicyCard(
            title = "5. Data Storage, Security & Backup",
            icon = Icons.Outlined.Shield
        ) {
            PolicyBullet(
                title = "Local-First Architecture:",
                desc = "All worker databases and attendance history are stored directly on your device in a sandboxed SQLite database (Room DB)."
            )
            PolicyBullet(
                title = "App Lock Protection:",
                desc = "You can activate a 4-digit PIN security lock inside Settings to prevent unauthorized physical access to your records."
            )
            PolicyBullet(
                title = "User Controlled Backups:",
                desc = "You retain 100% control of your data and can export JSON backups or restore them at any time."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 6: Data Deletion Rights
        PolicyCard(
            title = "6. User Rights & Data Deletion",
            icon = Icons.Outlined.DeleteForever
        ) {
            Text(
                text = "In compliance with Google Play Store User Data policies and international privacy standards (GDPR / CCPA):",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            PolicyBullet(
                title = "Right to Erase / Reset:",
                desc = "You can clear all local app data, workers, and attendance history at any time from the app's Data Safety settings or Android App Info > Clear Storage."
            )
            PolicyBullet(
                title = "Account Deletion Request:",
                desc = "If you have an associated Firebase account and wish to permanently delete all server records, you may trigger in-app deletion or email us at azazmadkiya@gmail.com with the subject 'Data Deletion Request'."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 7: Third-Party SDKs
        PolicyCard(
            title = "7. Third-Party Service Providers",
            icon = Icons.Outlined.Hub
        ) {
            Text(
                text = "We may use trusted third-party SDKs that process limited data in accordance with their privacy policies:",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Google Play Services (Core Android functionality)\n• Firebase Authentication (Secure OAuth login)\n• Google Maps Platform (Geofence coordinates visualization)", fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 8: Children's Privacy
        PolicyCard(
            title = "8. Children's Privacy",
            icon = Icons.Outlined.ChildCare
        ) {
            Text(
                text = "This application is designed for business management and workplace administration. It is not directed to children under the age of 13, and we do not knowingly collect personal information from children.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 9: Contact Information
        PolicyCard(
            title = "9. Contact Us",
            icon = Icons.Outlined.Mail
        ) {
            Text(
                text = "If you have any questions, concerns, or requests regarding this Privacy Policy, please contact our Data Protection Officer:\n\nDeveloper: Azaz Madkiya\nEmail: azazmadkiya@gmail.com\nApplication: Attendance App (Haazri Pro)",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:azazmadkiya@gmail.com?subject=Attendance%20App%20Privacy%20Inquiry")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Support (azazmadkiya@gmail.com)", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun PolicyCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun PolicyBullet(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), fontSize = 14.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 17.sp
            )
        }
    }
}
