package com.attendance.app.azaz.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HelpOutline
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.attendance.app.azaz.ui.theme.HaazriHeaderBlue
import com.attendance.app.azaz.ui.theme.HaazriPrimary
import com.attendance.app.azaz.util.AppLockManager
import com.attendance.app.azaz.util.findFragmentActivity

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()

    val savedPin by appLockManager.pinFlow.collectAsState(initial = appLockManager.getSavedPinSync())
    val isBiometricEnabled by appLockManager.biometricFlow.collectAsState(initial = appLockManager.isBiometricEnabledSync())
    val canUseBiometrics = remember { appLockManager.canAuthenticateWithBiometrics() }

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPromptedBiometrics by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button: keep user on lock screen
    BackHandler {
        Toast.makeText(context, "Please unlock with PIN or Fingerprint", Toast.LENGTH_SHORT).show()
    }

    fun verifyPin(pin: String) {
        val expectedPin = savedPin?.ifBlank { "0000" } ?: "0000"
        if (pin == expectedPin) {
            error = null
            appLockManager.unlockApp()
            onUnlocked()
        } else {
            error = if (expectedPin == "0000") "Incorrect PIN. Default is 0000." else "Incorrect PIN. Please enter your 4-digit PIN."
            pinInput = ""
        }
    }

    fun handleKeyPress(digit: String) {
        if (pinInput.length < 4) {
            error = null
            val newPin = pinInput + digit
            pinInput = newPin
            if (newPin.length == 4) {
                verifyPin(newPin)
            }
        }
    }

    fun handleDelete() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
            error = null
        }
    }

    fun showBiometricPrompt() {
        if (activity != null && canUseBiometrics) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        error = null
                        appLockManager.unlockApp()
                        onUnlocked()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        error = "Fingerprint not recognized. Try again or enter PIN."
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Attendance App")
                .setSubtitle("Authenticate to access worker attendance and cashbook")
                .setNegativeButtonText("Use 4-Digit PIN")
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto-prompt biometrics once on screen launch if enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && canUseBiometrics && !hasPromptedBiometrics) {
            hasPromptedBiometrics = true
            showBiometricPrompt()
        }
    }

    val expectedPin = savedPin?.ifBlank { "0000" } ?: "0000"
    val isDefaultPin = expectedPin == "0000"

    // Forgot PIN Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = HaazriPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PIN Security Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    if (isDefaultPin) {
                        Text(
                            text = "• The default PIN is 0000.\n• Tap below to unlock with default 0000.\n• You can change your PIN anytime in Settings > App Security Lock.",
                            fontSize = 14.sp,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "• You have configured a custom 4-digit PIN for app security.\n• Enter your user-defined PIN to unlock the dashboard.\n• You can manage or reset your PIN to 0000 anytime in the App Security Settings screen.",
                            fontSize = 14.sp,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (isDefaultPin) {
                    Button(
                        onClick = {
                            showForgotDialog = false
                            pinInput = "0000"
                            verifyPin("0000")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                    ) {
                        Text("Use Default PIN (0000)", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { showForgotDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                    ) {
                        Text("OK, Got It", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (isDefaultPin) {
                    OutlinedButton(onClick = { showForgotDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Security Icon Header
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(HaazriPrimary.copy(alpha = 0.2f), HaazriPrimary.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(HaazriPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Lock",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "App Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter 4-Digit PIN to unlock attendance data",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 PIN Indicator Circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinInput.length
                    val isError = error != null
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isFilled -> HaazriPrimary
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (!isFilled && !isError) {
                                    Modifier.background(Color.White)
                                } else Modifier
                            )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = when {
                                isError -> MaterialTheme.colorScheme.error
                                isFilled -> HaazriPrimary
                                else -> Color(0xFFE2E8F0)
                            },
                            border = if (!isFilled && !isError) BorderStroke(1.5.dp, Color(0xFF94A3B8)) else null
                        ) {}
                    }
                }
            }

            // Error Message Display
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick default 0000 helper chip or Custom PIN indicator
            if (isDefaultPin) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .clickable {
                            pinInput = "0000"
                            verifyPin("0000")
                        }
                        .testTag("chip_default_pin_0000")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = HaazriPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Default PIN: 0000 (Tap to Unlock)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HaazriPrimary
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Protected with Custom 4-Digit PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Numeric Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val row1 = listOf("1", "2", "3")
                val row2 = listOf("4", "5", "6")
                val row3 = listOf("7", "8", "9")

                listOf(row1, row2, row3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            KeypadButton(
                                text = digit,
                                onClick = { handleKeyPress(digit) },
                                tag = "keypad_btn_$digit"
                            )
                        }
                    }
                }

                // Row 4: Fingerprint / Clear, 0, Backspace
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Left Key: Fingerprint or Clear
                    if (isBiometricEnabled && canUseBiometrics) {
                        Surface(
                            onClick = { showBiometricPrompt() },
                            shape = CircleShape,
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("keypad_btn_biometric")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = "Unlock with Fingerprint",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                pinInput = ""
                                error = null
                            },
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("keypad_btn_clear")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "C",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Middle Key: 0
                    KeypadButton(
                        text = "0",
                        onClick = { handleKeyPress("0") },
                        tag = "keypad_btn_0"
                    )

                    // Right Key: Backspace
                    Surface(
                        onClick = { handleDelete() },
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("keypad_btn_delete")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete digit",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Forgot PIN & Fingerprint row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showForgotDialog = true },
                    modifier = Modifier.testTag("btn_forgot_pin")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Forgot PIN?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                if (isBiometricEnabled && canUseBiometrics) {
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = { showBiometricPrompt() },
                        modifier = Modifier.testTag("btn_trigger_fingerprint")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = HaazriPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Fingerprint",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HaazriPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .size(68.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}
