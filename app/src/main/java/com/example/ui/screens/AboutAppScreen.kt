package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.HaazriViewModel
import com.example.viewmodel.ScreenState

@Composable
fun AboutAppScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5EE))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // App Hero Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Attendance App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Attendance App",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Haazri & Payroll Pro",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF93C5FD)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Version 1.2.0 • Play Store Release",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // App Description
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About the Application",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attendance App (Haazri Pro) is an all-in-one daily workforce attendance, overtime, salary, and cashbook management tool tailored for small businesses, contractors, factories, and site supervisors.\n\nFeaturing local offline database security, Geofence-verified check-ins, multi-language support (English, Hindi, Gujarati, Marathi), automated PDF/Excel salary slips, and instant WhatsApp payslip sharing.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legal & Policies Section
        Text(
            text = "Legal & Policies",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF475569),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AboutLinkRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How we protect and handle your information",
                    onClick = { viewModel.activeScreen.value = ScreenState.PRIVACY_POLICY }
                )
                Divider(color = Color(0xFFF1F5F9))
                AboutLinkRow(
                    icon = Icons.Outlined.Gavel,
                    title = "Terms & Conditions",
                    subtitle = "User agreement & service terms",
                    onClick = { viewModel.activeScreen.value = ScreenState.TERMS_OF_SERVICE }
                )
                Divider(color = Color(0xFFF1F5F9))
                AboutLinkRow(
                    icon = Icons.Outlined.Security,
                    title = "Data Safety & Permissions",
                    subtitle = "Google Play compliance & permissions guide",
                    onClick = { viewModel.activeScreen.value = ScreenState.DATA_SAFETY }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Developer & Publisher Card
        Text(
            text = "Developer & Publisher",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF475569),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Azaz Madkiya",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "azazmadkiya@gmail.com",
                            fontSize = 13.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:azazmadkiya@gmail.com?subject=Attendance%20App%20Inquiry")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Developer Support", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEEF2FF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
    }
}
