package com.example.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.HaazriPrimary
import com.example.util.AppLockManager
import com.example.util.findFragmentActivity

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    
    val savedPin by appLockManager.pinFlow.collectAsState(initial = null)
    val isBiometricEnabled by appLockManager.biometricFlow.collectAsState(initial = false)
    
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPrompted by remember { mutableStateOf(false) }
    var showPinFallback by remember { mutableStateOf(!isBiometricEnabled) }
    
    fun showBiometricPrompt() {
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Show PIN fallback if biometric fails or is cancelled
                        showPinFallback = true
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        appLockManager.unlockApp()
                        onUnlocked()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        error = "Fingerprint not recognized. Please try again."
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Attendance Data")
                .setSubtitle("Confirm your identity to access sensitive records")
                .setNegativeButtonText("Use PIN")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !hasPrompted) {
            hasPrompted = true
            showBiometricPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)), // Light clean background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Secure Icon Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HaazriPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Lock",
                    tint = HaazriPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "App Locked",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please authenticate to access your secure attendance records and cashbook.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = showPinFallback,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            if (it.length <= 4) pinInput = it
                            if (pinInput.length == 4) {
                                if (pinInput == savedPin) {
                                    error = null
                                    appLockManager.unlockApp()
                                    onUnlocked()
                                } else {
                                    error = "Incorrect PIN"
                                    pinInput = "" // reset on error
                                }
                            }
                        },
                        label = { Text("Enter 4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = error != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!, 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Surface(
                    onClick = { showBiometricPrompt() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Unlock with Fingerprint",
                            tint = HaazriPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap to unlock with Biometrics",
                    fontSize = 13.sp,
                    color = HaazriPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showBiometricPrompt() }
                )
            }
        }
    }
}
