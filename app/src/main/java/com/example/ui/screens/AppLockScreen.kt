package com.example.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.util.AppLockManager

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity
    val savedPin by appLockManager.getPin().collectAsState(initial = null)
    val isBiometricEnabled by appLockManager.isBiometricEnabled().collectAsState(initial = false)

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPromptedBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(isBiometricEnabled, hasPromptedBiometric) {
        if (isBiometricEnabled && !hasPromptedBiometric) {
            hasPromptedBiometric = true
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(context, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // If user cancels, they can just use PIN
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        appLockManager.unlockApp()
                        onUnlocked()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        error = "Fingerprint not recognized"
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use PIN")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "App Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
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
                    }
                }
            },
            label = { Text("Enter 4-Digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = error != null
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
