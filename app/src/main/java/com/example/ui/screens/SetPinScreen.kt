package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (step == 1) {
            Text(text = "Set a 4-Digit PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("Enter PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null
            )
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (pin.length == 4) {
                    error = null
                    step = 2
                } else {
                    error = "PIN must be 4 digits"
                }
            }) {
                Text("Next")
            }
        } else if (step == 2) {
            Text(text = "Confirm 4-Digit PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = { Text("Re-enter PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null
            )
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (confirmPin == pin) {
                    error = null
                    step = 3
                } else {
                    error = "PINs do not match"
                }
            }) {
                Text("Confirm")
            }
        } else if (step == 3) {
            Text(text = "Enable Fingerprint Unlock?", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "You can also use your fingerprint to unlock the app.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = {
                    coroutineScope.launch {
                        appLockManager.setPin(pin)
                        appLockManager.setBiometricEnabled(false)
                        appLockManager.unlockApp()
                        onPinSet()
                    }
                }) {
                    Text("Skip")
                }
                Button(onClick = {
                    coroutineScope.launch {
                        appLockManager.setPin(pin)
                        appLockManager.setBiometricEnabled(true)
                        appLockManager.unlockApp()
                        onPinSet()
                    }
                }) {
                    Text("Enable")
                }
            }
        }
    }
}
