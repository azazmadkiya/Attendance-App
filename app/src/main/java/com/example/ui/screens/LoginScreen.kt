package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.example.viewmodel.HaazriViewModel

@Composable
fun LoginScreen(
    viewModel: HaazriViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

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
                            Row(
                                modifier = Modifier.padding(10.dp),
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
                                    lineHeight = 16.sp
                                )
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

                    // Mobile Number
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it.filter { char -> char.isDigit() }.take(10) },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("Enter 10 digit mobile number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        placeholder = { Text(if (isRegisterMode) "Enter password (min 4 characters)" else "Enter your password") },
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button (Sign Up / Login)
                    Button(
                        onClick = {
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
                                if (phoneInput.length < 10) {
                                    errorMessage = "Please enter a valid 10-digit mobile number"
                                    return@Button
                                }
                                if (passwordInput.length < 4) {
                                    errorMessage = "Please set a password (at least 4 characters)"
                                    return@Button
                                }
                                if (emailInput.isNotBlank() && !emailInput.contains("@")) {
                                    errorMessage = "Please enter a valid email address"
                                    return@Button
                                }
                                viewModel.registerUser(
                                    company = companyNameInput,
                                    name = managerNameInput,
                                    phone = phoneInput,
                                    passwordOrPin = passwordInput,
                                    email = emailInput.trim()
                                )
                                Toast.makeText(context, "Welcome to Attendance App, $managerNameInput!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                if (phoneInput.length < 10) {
                                    errorMessage = "Please enter 10-digit mobile number"
                                    return@Button
                                }
                                if (passwordInput.isBlank()) {
                                    errorMessage = "Please enter your password"
                                    return@Button
                                }
                                val success = viewModel.loginUser(phone = phoneInput, passwordOrPin = passwordInput)
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "Incorrect password or unregistered mobile number"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_auth_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                    ) {
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
