package com.attendance.app.azaz.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.attendance.app.azaz.ui.theme.HaazriBgCream
import com.attendance.app.azaz.ui.theme.HaazriHeaderBlue
import com.attendance.app.azaz.ui.theme.HaazriPrimary
import com.attendance.app.azaz.viewmodel.HaazriViewModel
import com.attendance.app.azaz.viewmodel.ScreenState
import java.io.File

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("avatar_${System.currentTimeMillis()}", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: HaazriViewModel,
    isInitialSetup: Boolean = false,
    onComplete: () -> Unit = { viewModel.activeScreen.value = ScreenState.MAIN_TABS }
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentUserName by viewModel.loggedInUserName.collectAsState()
    val currentCompany by viewModel.loggedInCompanyName.collectAsState()
    val currentPhone by viewModel.loggedInPhone.collectAsState()
    val currentEmail by viewModel.loggedInEmail.collectAsState()
    val currentPhotoUri by viewModel.loggedInUserPhoto.collectAsState()

    var displayNameInput by remember { mutableStateOf(currentUserName) }
    var companyNameInput by remember { mutableStateOf(currentCompany) }
    var selectedPhotoUri by remember { mutableStateOf(currentPhotoUri) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    BackHandler {
        onComplete()
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            val uriString = pendingCameraUri.toString()
            selectedPhotoUri = uriString
            viewModel.updateUserPhoto(uriString)
            Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = createTempImageUri(context)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take a profile photo.", Toast.LENGTH_LONG).show()
        }
    }

    // Photo picker launcher (Android Photo Picker - Zero Permission)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uriString = uri.toString()
            selectedPhotoUri = uriString
            viewModel.updateUserPhoto(uriString)
            Toast.makeText(context, "Photo selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerCamera = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                val uri = createTempImageUri(context)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isInitialSetup) "Setup Your Profile" else "User Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (!isInitialSetup) {
                        IconButton(onClick = onComplete) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (isInitialSetup) {
                        TextButton(onClick = onComplete) {
                            Text(
                                text = "Skip",
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HaazriHeaderBlue
                )
            )
        },
        containerColor = HaazriBgCream
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo with Camera Badge
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .size(130.dp)
                            .testTag("profile_photo_container")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(HaazriPrimary, Color(0xFF3B82F6))
                                    )
                                )
                                .border(3.dp, Color.White, CircleShape)
                                .clickable { showPhotoOptionsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!selectedPhotoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = displayNameInput.trim().firstOrNull()?.uppercase() ?: "A",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Camera overlay badge button
                        FilledIconButton(
                            onClick = { showPhotoOptionsDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .offset(x = 4.dp, y = 4.dp)
                                .testTag("btn_camera_badge"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = HaazriPrimary,
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isInitialSetup) "Add Your Photo & Details" else "Profile Photo & Name",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "Tap the camera to take a photo or select one from gallery",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Buttons Row (Camera & Gallery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = triggerCamera,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_take_photo"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_pick_photo"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gallery",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    if (!selectedPhotoUri.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                selectedPhotoUri = null
                                viewModel.updateUserPhoto(null)
                                Toast.makeText(context, "Photo removed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("btn_remove_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove Photo",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Information Form
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Profile Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display Name Field
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Display Name *") },
                        placeholder = { Text("Enter your full name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = HaazriPrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Business / Company Name Field
                    OutlinedTextField(
                        value = companyNameInput,
                        onValueChange = { companyNameInput = it },
                        label = { Text("Company / Business Name") },
                        placeholder = { Text("Enter business name") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Business, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone Number (Information)
                    OutlinedTextField(
                        value = currentPhone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Registered Mobile") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFFF8FAFC)
                        ),
                        enabled = false
                    )

                    if (currentEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = currentEmail,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Registered Email") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save / Continue Button
            Button(
                onClick = {
                    val trimmedName = displayNameInput.trim()
                    if (trimmedName.isBlank()) {
                        Toast.makeText(context, "Please enter your display name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.updateUserDisplayName(trimmedName)
                    viewModel.updateUserProfile(
                        name = trimmedName,
                        company = companyNameInput.trim().ifBlank { "My Business" },
                        phone = currentPhone
                    )
                    if (selectedPhotoUri != null) {
                        viewModel.updateUserPhoto(selectedPhotoUri)
                    }
                    isSaving = false
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    onComplete()
                },
                enabled = !isSaving && displayNameInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_profile"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Profile...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isInitialSetup) "Save & Continue to Dashboard" else "Save Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isInitialSetup) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("btn_skip_setup")
                ) {
                    Text(
                        text = "I'll do this later",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Photo Source Selection Dialog
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = "Profile Photo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose how you want to upload your photo:",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ListItem(
                        headlineContent = { Text("Take Photo with Camera", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = HaazriPrimary)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showPhotoOptionsDialog = false
                                triggerCamera()
                            }
                    )

                    ListItem(
                        headlineContent = { Text("Choose from Gallery", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = HaazriPrimary)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showPhotoOptionsDialog = false
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    )

                    if (!selectedPhotoUri.isNullOrBlank()) {
                        ListItem(
                            headlineContent = {
                                Text("Remove Current Photo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            },
                            leadingContent = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showPhotoOptionsDialog = false
                                    selectedPhotoUri = null
                                    viewModel.updateUserPhoto(null)
                                    Toast.makeText(context, "Photo removed", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
