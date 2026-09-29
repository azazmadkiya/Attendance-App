package com.attendance.app.azaz.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.azaz.ui.theme.HaazriBgCream
import com.attendance.app.azaz.ui.theme.HaazriHeaderBlue
import com.attendance.app.azaz.ui.theme.HaazriPrimary
import com.attendance.app.azaz.util.AppLockManager
import kotlinx.coroutines.launch

@Composable
fun PinSecuritySettingsScreen(
    appLockManager: AppLockManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val canUseBiometrics = remember { appLockManager.canAuthenticateWithBiometrics() }

    var isLockEnabled by remember {
        mutableStateOf(appLockManager.getSavedPinSync() != null)
    }
    var pinInput by remember {
        mutableStateOf(appLockManager.getSavedPinSync() ?: "0000") // Default PIN 0000
    }
    var isBiometricEnabled by remember {
        mutableStateOf(appLockManager.isBiometricEnabledSync())
    }
    var showPinText by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBack()
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
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Security",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "App Security PIN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Manage 4-digit PIN (Default is 0000) & Fingerprint",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Main Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Toggle Enable Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable App Lock Security",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Require PIN or Fingerprint when opening app",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { isLockEnabled = it },
                            modifier = Modifier.testTag("switch_enable_app_lock")
                        )
                    }

                    AnimatedVisibility(
                        visible = isLockEnabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(16.dp))

                            // 4-Digit PIN Section
                            Text(
                                text = "4-Digit Numeric PIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Default PIN is 0000. You can keep it or enter your own 4 digits.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    if (filtered.length <= 4) {
                                        pinInput = filtered
                                        errorMessage = null
                                    }
                                },
                                label = { Text("4-Digit PIN (Default: 0000)") },
                                placeholder = { Text("0000") },
                                singleLine = true,
                                visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                trailingIcon = {
                                    IconButton(onClick = { showPinText = !showPinText }) {
                                        Icon(
                                            imageVector = if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPinText) "Hide PIN" else "Show PIN",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                isError = errorMessage != null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_app_lock_pin")
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Quick Chip to Reset / Set to 0000
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        pinInput = "0000"
                                        errorMessage = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = HaazriPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset to Default (0000)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HaazriPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Optional Fingerprint Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = "Fingerprint",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fingerprint Unlock",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (canUseBiometrics) "Unlock quickly with biometric sensor" else "Biometric sensor not available on this device",
                                        fontSize = 12.sp,
                                        color = if (canUseBiometrics) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }

                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { isBiometricEnabled = it },
                                    enabled = canUseBiometrics,
                                    modifier = Modifier.testTag("switch_biometric_unlock")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Button
                    Button(
                        onClick = {
                            if (isLockEnabled) {
                                val finalPin = pinInput.ifBlank { "0000" }
                                if (finalPin.length != 4) {
                                    errorMessage = "PIN must be exactly 4 digits (e.g. 0000)"
                                    return@Button
                                }
                                coroutineScope.launch {
                                    appLockManager.setPin(finalPin)
                                    appLockManager.setBiometricEnabled(isBiometricEnabled && canUseBiometrics)
                                    appLockManager.setPromptedAppLock(true)
                                    Toast.makeText(context, "App security PIN saved successfully ($finalPin)", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            } else {
                                coroutineScope.launch {
                                    appLockManager.clearLock()
                                    appLockManager.setPromptedAppLock(true)
                                    Toast.makeText(context, "App security lock disabled", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_save_pin_security"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLockEnabled) "Save Security PIN" else "Disable App Lock",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = HaazriPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "When App Lock is active, you will be prompted for your 4-digit PIN (default 0000) or fingerprint every time you open or resume the app.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
