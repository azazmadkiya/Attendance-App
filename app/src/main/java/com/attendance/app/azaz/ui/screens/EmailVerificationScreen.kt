package com.attendance.app.azaz.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.attendance.app.azaz.ui.theme.HaazriBgCream
import com.attendance.app.azaz.ui.theme.HaazriHeaderBlue
import com.attendance.app.azaz.ui.theme.HaazriPrimary
import com.attendance.app.azaz.viewmodel.HaazriViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    viewModel: HaazriViewModel,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentEmail by viewModel.loggedInEmail.collectAsState()
    val authUserEmail = remember { viewModel.authManager.getCurrentUserInfo()?.email }
    val displayEmail = if (!authUserEmail.isNullOrBlank()) authUserEmail else currentEmail

    var isChecking by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }

    // Allow user to navigate to dashboard on back press
    BackHandler {
        onVerified()
    }

    // Auto-cooldown timer for resend
    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    // Auto-check verification when user returns to app from their email client
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val verified = viewModel.checkEmailVerificationStatus()
                    if (verified) {
                        Toast.makeText(context, "Email verified successfully! Welcome to Haazri.", Toast.LENGTH_LONG).show()
                        onVerified()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HaazriBgCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(HaazriPrimary, HaazriHeaderBlue)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = "Email Verification",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Verify Your Email",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Confirm your email address to access your dashboard",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Body Card
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "We sent a verification link to:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = HaazriPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayEmail.ifBlank { "your email address" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Open the email on this phone or another device, click the confirmation link, and return here.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Tip: Spam folder
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Can't find the email? Please check your Spam, Junk, or Updates folder.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Status message banner
                        AnimatedVisibility(
                            visible = statusMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            statusMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isStatusError) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isStatusError) Color(0xFFFECACA) else Color(0xFFFDE68A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isStatusError) Color(0xFFDC2626) else Color(0xFFB45309),
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick 1-Tap Solution: Instant Verify & Open Dashboard
                        Button(
                            onClick = {
                                viewModel.verifyEmailInstantly()
                                Toast.makeText(context, "Account activated successfully! Welcome to Haazri.", Toast.LENGTH_LONG).show()
                                onVerified()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_instant_verify"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instant Verify & Open Dashboard",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Primary Action: I've Verified My Email
                        OutlinedButton(
                            onClick = {
                                if (isChecking) return@OutlinedButton
                                coroutineScope.launch {
                                    isChecking = true
                                    statusMessage = null
                                    isStatusError = false
                                    val verified = viewModel.checkEmailVerificationStatus()
                                    isChecking = false
                                    if (verified) {
                                        Toast.makeText(context, "Email confirmed! Welcome to Haazri.", Toast.LENGTH_LONG).show()
                                        onVerified()
                                    } else {
                                        isStatusError = false
                                        statusMessage = "Email link not detected yet. If you did not receive the email, tap 'Instant Verify & Open Dashboard' above to enter directly."
                                    }
                                }
                            },
                            enabled = !isChecking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_check_email_verification"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Checking Status...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("I've Clicked The Email Link", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary Action: Resend Link
                        OutlinedButton(
                            onClick = {
                                if (isSending || resendCooldownSeconds > 0) return@OutlinedButton
                                coroutineScope.launch {
                                    isSending = true
                                    statusMessage = null
                                    val result = viewModel.sendVerificationEmail()
                                    isSending = false
                                    if (result.isSuccess) {
                                        resendCooldownSeconds = 60
                                        isStatusError = false
                                        statusMessage = "A fresh verification link has been sent to your email!"
                                        Toast.makeText(context, "Verification email resent successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isStatusError = true
                                        statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to resend email. Try again later."
                                    }
                                }
                            },
                            enabled = !isSending && resendCooldownSeconds == 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_resend_verification_email"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sending link...")
                            } else if (resendCooldownSeconds > 0) {
                                Text("Resend link in ${resendCooldownSeconds}s", color = Color.Gray, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resend Verification Email", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Skip / Continue to Dashboard Option
                        OutlinedButton(
                            onClick = {
                                onVerified()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_skip_verification"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HaazriPrimary)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HaazriPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Continue to Dashboard (Verify Later)",
                                fontWeight = FontWeight.Bold,
                                color = HaazriPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tertiary Action: Logout / Switch Account
                        TextButton(
                            onClick = {
                                viewModel.logout()
                            },
                            modifier = Modifier.testTag("btn_verification_logout")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Use Different Account / Log Out",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
