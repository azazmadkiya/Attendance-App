package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HaazriPrimary
import kotlinx.coroutines.launch
import com.example.util.AppLockManager

@Composable
fun SetPinScreen(
    appLockManager: AppLockManager,
    onPinSet: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(1) } // 1 = Set PIN, 2 = Confirm PIN, 3 = Biometrics? (Optional)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)), // Clean modern background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Icon Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HaazriPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (step == 3) Icons.Default.Fingerprint else Icons.Default.Security,
                    contentDescription = "Security",
                    tint = HaazriPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (step == 1) {
                Text(
                    text = "Secure Your App",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set a 4-digit PIN to protect your attendance records.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 4) pin = it
                        if (pin.length == 4) {
                            error = null
                            step = 2
                        }
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else if (step == 2) {
                Text(
                    text = "Confirm Your PIN",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please re-enter the 4-digit PIN.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        if (it.length <= 4) confirmPin = it
                        if (confirmPin.length == 4) {
                            if (confirmPin == pin) {
                                error = null
                                if (appLockManager.canAuthenticateWithBiometrics()) {
                                    step = 3
                                } else {
                                    coroutineScope.launch {
                                        appLockManager.setPin(pin)
                                        appLockManager.setBiometricEnabled(false)
                                        appLockManager.unlockApp()
                                        onPinSet()
                                    }
                                }
                            } else {
                                error = "PINs do not match"
                                confirmPin = "" // reset
                            }
                        }
                    },
                    label = { Text("Re-enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else if (step == 3) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInHorizontally()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Enable Fingerprint",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use your biometric data for faster secure access to the dashboard.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    appLockManager.setPin(pin)
                                    appLockManager.setBiometricEnabled(true)
                                    appLockManager.unlockApp()
                                    onPinSet()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Biometric Unlock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    appLockManager.setPin(pin)
                                    appLockManager.setBiometricEnabled(false)
                                    appLockManager.unlockApp()
                                    onPinSet()
                                }
                            }
                        ) {
                            Text("Skip for now", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
