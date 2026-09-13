package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.theme.HaazriBgCream
import com.example.ui.theme.HaazriHeaderBlue
import com.example.ui.theme.HaazriPrimary
import com.example.util.AuthResult
import com.example.viewmodel.HaazriViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: HaazriViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) return@remember c
            c = c.baseContext
        }
        null
    }
    val coroutineScope = rememberCoroutineScope()

    // Mode: Login vs Sign-Up
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form fields
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var companyNameInput by remember { mutableStateOf("") }
    var managerNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

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
                            colors = listOf(
                                HaazriPrimary,
                                HaazriHeaderBlue
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Attendance App",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) "Create your Business Account • Sign Up" else "Secure Staff Attendance & Payroll • Login",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Main Card Container for Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .offset(y = (-18).dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Mode Selector (Login vs Sign-Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isRegisterMode) HaazriPrimary else Color.Transparent)
                                .clickable {
                                    isRegisterMode = false
                                    errorMessage = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("tab_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (!isRegisterMode) Color.White else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isRegisterMode) HaazriPrimary else Color.Transparent)
                                .clickable {
                                    isRegisterMode = true
                                    errorMessage = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("tab_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign-Up",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isRegisterMode) Color.White else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error Message Container
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        fontSize = 12.sp,
                                        color = Color(0xFFC62828),
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (errorMessage!!.contains("device Settings", ignoreCase = true) ||
                                    errorMessage!!.contains("Google account", ignoreCase = true)
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    })
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFC62828)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Open Device Settings to Add Account",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sign-Up Specific Fields
                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            // Company / Business Name
                            OutlinedTextField(
                                value = companyNameInput,
                                onValueChange = { companyNameInput = it },
                                label = { Text("Company / Business Name") },
                                placeholder = { Text("e.g. Madkiya Construction") },
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("company_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manager / Supervisor Name
                            OutlinedTextField(
                                value = managerNameInput,
                                onValueChange = { managerNameInput = it },
                                label = { Text("Your Name (Admin/Supervisor)") },
                                placeholder = { Text("e.g. Azaz Madkiya") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manager_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Email Address (Optional)
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address (Optional)") },
                                placeholder = { Text("admin@example.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Mobile Number or Email
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { 
                            // In register mode only allow 10 digits; in login mode allow mobile or email
                            phoneInput = if (isRegisterMode) it.filter { char -> char.isDigit() }.take(10) else it
                        },
                        label = { Text(if (isRegisterMode) "Mobile Number" else "Mobile Number or Email") },
                        placeholder = { Text(if (isRegisterMode) "Enter 10 digit mobile number" else "Enter mobile number or email") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (!isRegisterMode && phoneInput.contains("@")) Icons.Default.Email else Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isRegisterMode) KeyboardType.Number else KeyboardType.Email
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(if (isRegisterMode) "Set Password" else "Password") },
                        placeholder = { Text(if (isRegisterMode) "Enter password (min 6 characters)" else "Enter your password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isRegisterMode && passwordInput.isNotEmpty()) {
                        val passwordStrength = remember(passwordInput) {
                            var score = 0
                            if (passwordInput.length >= 8) score++
                            if (passwordInput.any { it.isDigit() }) score++
                            if (passwordInput.any { it.isUpperCase() }) score++
                            if (passwordInput.any { !it.isLetterOrDigit() }) score++
                            score
                        }

                        val strengthColor = when (passwordStrength) {
                            0, 1 -> Color(0xFFEF4444)
                            2 -> Color(0xFFF59E0B)
                            3 -> Color(0xFFEAB308)
                            4 -> Color(0xFF10B981)
                            else -> Color.Gray
                        }

                        val strengthText = when (passwordStrength) {
                            0, 1 -> "Weak"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Strong"
                            else -> ""
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val color = if (index < passwordStrength || (passwordStrength == 0 && index == 0)) strengthColor else Color(0xFFE2E8F0)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Password strength: $strengthText",
                            fontSize = 12.sp,
                            color = strengthColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isRegisterMode) {
                        var showForgotDialog by remember { mutableStateOf(false) }
                        var forgotEmailInput by remember { mutableStateOf("") }
                        var forgotLoading by remember { mutableStateOf(false) }
                        var forgotMessage by remember { mutableStateOf<String?>(null) }

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = { showForgotDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Forgot Password?", color = HaazriPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (showForgotDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!forgotLoading) showForgotDialog = false
                                },
                                title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text("Enter your registered email address to receive a password reset link.", color = Color.Gray, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = forgotEmailInput,
                                            onValueChange = { forgotEmailInput = it },
                                            label = { Text("Email Address") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (forgotMessage != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(forgotMessage!!, color = if (forgotMessage!!.contains("Check your email", true)) Color(0xFF10B981) else MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (forgotEmailInput.isBlank() || !forgotEmailInput.contains("@")) {
                                                forgotMessage = "Please enter a valid email address."
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                forgotLoading = true
                                                forgotMessage = null
                                                val res = viewModel.sendPasswordResetEmail(forgotEmailInput.trim())
                                                forgotLoading = false
                                                if (res.isSuccess) {
                                                    forgotMessage = "Check your email for the reset link."
                                                } else {
                                                    forgotMessage = res.exceptionOrNull()?.message ?: "Failed to send reset email."
                                                }
                                            }
                                        },
                                        enabled = !forgotLoading
                                    ) {
                                        if (forgotLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("Send Link")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showForgotDialog = false }, enabled = !forgotLoading) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button (Sign Up / Login)
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            errorMessage = null

                            if (isRegisterMode) {
                                if (companyNameInput.isBlank()) {
                                    errorMessage = "Please enter company / business name"
                                    return@Button
                                }
                                if (managerNameInput.isBlank()) {
                                    errorMessage = "Please enter your name"
                                    return@Button
                                }
                                val cleanPhone = phoneInput.filter { it.isDigit() }
                                if (cleanPhone.length < 10) {
                                    errorMessage = "Please enter a valid 10-digit mobile number"
                                    return@Button
                                }
                                if (passwordInput.length < 8 || !passwordInput.any { it.isDigit() } || !passwordInput.any { it.isUpperCase() }) {
                                    errorMessage = "Password must be at least 8 characters, with 1 uppercase letter and 1 number"
                                    return@Button
                                }
                                if (emailInput.isNotBlank() && !emailInput.contains("@")) {
                                    errorMessage = "Please enter a valid email address"
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isLoading = true
                                    val result = viewModel.registerUser(
                                        company = companyNameInput,
                                        name = managerNameInput,
                                        phone = cleanPhone,
                                        passwordOrPin = passwordInput,
                                        email = emailInput.trim()
                                    )
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            Toast.makeText(context, "Account created & synced to Cloud!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        is AuthResult.Cancelled -> {}
                                    }
                                }
                            } else {
                                if (phoneInput.isBlank()) {
                                    errorMessage = "Please enter your 10-digit mobile number or email"
                                    return@Button
                                }
                                if (passwordInput.isBlank()) {
                                    errorMessage = "Please enter your password"
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isLoading = true
                                    val result = viewModel.loginUser(
                                        phoneOrEmail = phoneInput.trim(),
                                        passwordOrPin = passwordInput
                                    )
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            Toast.makeText(context, "Welcome back, ${viewModel.loggedInUserName.value}!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        is AuthResult.Cancelled -> {}
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_auth_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRegisterMode) "Creating Cloud Account..." else "Verifying with Firebase...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Create Account (Sign Up)" else "Login to Dashboard",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider with OR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "OR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            if (isLoading) return@OutlinedButton
                            errorMessage = null
                            coroutineScope.launch {
                                isLoading = true
                                val result = viewModel.signInWithGoogle(activity)
                                isLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        Toast.makeText(context, "Welcome, ${viewModel.loggedInUserName.value}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                    is AuthResult.Cancelled -> {}
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_google_signin"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google Sign In",
                            tint = Color(0xFFEA4335),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Security Badge Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% Safe • Encrypted & Secure Database",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
