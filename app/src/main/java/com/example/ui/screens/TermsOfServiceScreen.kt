package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.HaazriViewModel

@Composable
fun TermsOfServiceScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                            imageVector = Icons.Outlined.Gavel,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Terms & Conditions",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Last Updated: August 2026",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Please read these Terms of Service carefully before using Attendance App (Haazri Pro). By using the application, you agree to be bound by these terms.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        // Section 1: Acceptance of Terms
        PolicyCard(
            title = "1. Acceptance of Terms",
            icon = Icons.Outlined.CheckCircleOutline
        ) {
            Text(
                text = "By downloading, installing, or using the Attendance App (Haazri Pro), you acknowledge that you have read, understood, and agree to be bound by these Terms and our Privacy Policy. If you disagree with any portion of these terms, please discontinue using the application.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: License & App Usage
        PolicyCard(
            title = "2. License & Authorized Use",
            icon = Icons.Outlined.VerifiedUser
        ) {
            PolicyBullet(
                title = "Non-Exclusive License:",
                desc = "You are granted a personal, non-transferable, revocable license to use Attendance App solely for managing legitimate workforce attendance, shifts, and salary calculations."
            )
            PolicyBullet(
                title = "Prohibited Conduct:",
                desc = "You agree not to decompile, reverse engineer, extract source code, modify, or resell the application or its bundled assets without prior written consent from the developer."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 3: User Responsibility & Data Backup
        PolicyCard(
            title = "3. Recordkeeping & Local Backup",
            icon = Icons.Outlined.CloudUpload
        ) {
            PolicyBullet(
                title = "Data Management Responsibility:",
                desc = "Attendance records, worker wages, and cashbook transactions are stored locally on your device. You are solely responsible for creating regular backups (JSON export or PDF/Excel archives) to prevent data loss due to device damage or factory resets."
            )
            PolicyBullet(
                title = "Dispute Resolution with Workers:",
                desc = "The application provides calculation utilities based on rates you enter. The user is responsible for resolving any wage disputes or verifying local labor law compliance."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 4: Disclaimer of Warranties
        PolicyCard(
            title = "4. Disclaimer of Warranties",
            icon = Icons.Outlined.WarningAmber
        ) {
            Text(
                text = "Attendance App is provided on an \"AS IS\" and \"AS AVAILABLE\" basis without warranties of any kind, whether express or implied. While we strive for absolute calculation precision, we do not warrant that the application will be 100% error-free or uninterrupted under all hardware configurations.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 5: Limitation of Liability
        PolicyCard(
            title = "5. Limitation of Liability",
            icon = Icons.Outlined.Shield
        ) {
            Text(
                text = "To the maximum extent permitted by applicable law, the developer (Azaz Madkiya) shall not be liable for any indirect, incidental, special, consequential, or punitive damages, including loss of data, profits, or business interruption arising from the use or inability to use this app.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 6: Modifications to Terms
        PolicyCard(
            title = "6. Changes to Terms",
            icon = Icons.Outlined.Update
        ) {
            Text(
                text = "We reserve the right to revise or update these Terms at any time. Continued use of the app following any modifications constitutes your acceptance of the updated terms.",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 7: Contact & Support
        PolicyCard(
            title = "7. Contact Information",
            icon = Icons.Outlined.ContactSupport
        ) {
            Text(
                text = "For legal inquiries or questions regarding these terms, please contact:\n\nDeveloper: Azaz Madkiya\nEmail: azazmadkiya@gmail.com\nLocation: India",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:azazmadkiya@gmail.com?subject=Attendance%20App%20Terms%20Inquiry")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Developer", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
