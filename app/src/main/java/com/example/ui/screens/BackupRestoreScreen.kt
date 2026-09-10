package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HaazriPrimary
import com.example.viewmodel.HaazriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupItem(
    val file: File,
    val name: String,
    val lastModified: Long,
    val sizeBytes: Long
)

@Composable
fun BackupRestoreScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val workers by viewModel.workers.collectAsState()
    val attendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val cashbookEntries by viewModel.cashbookEntries.collectAsState()

    val loggedInCompany by viewModel.loggedInCompanyName.collectAsState()
    val loggedInPhone by viewModel.loggedInPhone.collectAsState()

    var isOverwriteMode by remember { mutableStateOf(false) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var jsonPasteInput by remember { mutableStateOf("") }
    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }
    var isOperating by remember { mutableStateOf(false) }

    // Confirm restore dialog state for saved local backups
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }

    // Local backups stored on device
    var localBackups by remember { mutableStateOf<List<LocalBackupItem>>(emptyList()) }

    var autoBackupEnabled by remember { mutableStateOf(com.example.util.AutoBackupScheduler.isAutoBackupEnabled(context)) }
    var lastBackupTime by remember { mutableStateOf(com.example.util.AutoBackupScheduler.getLastBackupTime(context)) }
    var lastBackupSummary by remember { mutableStateOf(com.example.util.AutoBackupScheduler.getLastBackupSummary(context)) }
    var lastBackupStatus by remember { mutableStateOf(com.example.util.AutoBackupScheduler.getLastBackupStatus(context)) }
    var lastBackupFileName by remember { mutableStateOf(com.example.util.AutoBackupScheduler.getLastBackupFileName(context)) }

    fun refreshAutoBackupInfo() {
        lastBackupTime = com.example.util.AutoBackupScheduler.getLastBackupTime(context)
        lastBackupSummary = com.example.util.AutoBackupScheduler.getLastBackupSummary(context)
        lastBackupStatus = com.example.util.AutoBackupScheduler.getLastBackupStatus(context)
        lastBackupFileName = com.example.util.AutoBackupScheduler.getLastBackupFileName(context)
    }

    fun refreshLocalBackups() {
        val backupsDir = File(context.filesDir, "backups")
        if (backupsDir.exists()) {
            val list = backupsDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }
                ?.map { LocalBackupItem(it, it.name, it.lastModified(), it.length()) }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()
            localBackups = list
        } else {
            localBackups = emptyList()
        }
        refreshAutoBackupInfo()
    }

    LaunchedEffect(Unit) {
        refreshLocalBackups()
    }

    // System File Manager Launcher to save backup into user-chosen folder
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isOperating = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = viewModel.getFullBackupJson()
                    
                    // 1. Save locally first so it appears in "Saved Backups on Device"
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "Haazri_Backup_$timeStamp.json"
                    val backupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
                    val localFile = File(backupsDir, fileName)
                    localFile.writeText(jsonString, Charsets.UTF_8)
                    
                    val cacheFile = File(context.cacheDir, fileName)
                    cacheFile.writeText(jsonString, Charsets.UTF_8)
                    
                    // 2. Write to the user-chosen location safely using a stream copy
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        localFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        outputStream.flush()
                    }
                    
                    withContext(Dispatchers.Main) {
                        isOperating = false
                        isSuccessStatus = true
                        restoreStatusMessage = "Backup file saved successfully to selected location!"
                        Toast.makeText(context, "Backup file saved successfully!", Toast.LENGTH_LONG).show()
                        refreshLocalBackups()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isOperating = false
                        isSuccessStatus = false
                        restoreStatusMessage = "Failed to save file: ${e.localizedMessage}"
                        Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // SAF Document Picker launcher for JSON import
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isOperating = true
            restoreStatusMessage = "Reading and verifying backup file..."
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: ""

                    if (jsonString.isNotBlank()) {
                        viewModel.restoreBackupData(
                            jsonString = jsonString,
                            clearExisting = isOverwriteMode
                        ) { success, msg ->
                            isOperating = false
                            isSuccessStatus = success
                            restoreStatusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            refreshLocalBackups()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isOperating = false
                            isSuccessStatus = false
                            restoreStatusMessage = "Selected file is empty."
                            Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isOperating = false
                        isSuccessStatus = false
                        restoreStatusMessage = "Failed to read backup file: ${e.localizedMessage}"
                        Toast.makeText(context, "Error opening file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Confirmation dialog when restoring a saved local backup
    if (pendingRestoreFile != null) {
        val targetFile = pendingRestoreFile!!
        AlertDialog(
            onDismissRequest = { pendingRestoreFile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = HaazriPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Backup?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "File: ${targetFile.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isOverwriteMode)
                            "Mode: OVERWRITE (Current database will be replaced with data from this backup)."
                        else
                            "Mode: MERGE (New workers and records will be added without overwriting existing entries).",
                        fontSize = 12.sp,
                        color = if (isOverwriteMode) Color(0xFFDC2626) else Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileToRead = targetFile
                        pendingRestoreFile = null
                        isOperating = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val json = fileToRead.readText(Charsets.UTF_8)
                                viewModel.restoreBackupData(
                                    jsonString = json,
                                    clearExisting = isOverwriteMode
                                ) { success, msg ->
                                    isOperating = false
                                    isSuccessStatus = success
                                    restoreStatusMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isOperating = false
                                    isSuccessStatus = false
                                    restoreStatusMessage = "Restore failed: ${e.localizedMessage}"
                                    Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                ) {
                    Text("Confirm Restore", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingRestoreFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for pasting raw JSON text
    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        tint = HaazriPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paste Backup JSON Code", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Paste your exported JSON backup text below to restore your data.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = jsonPasteInput,
                        onValueChange = { jsonPasteInput = it },
                        placeholder = { Text("{\n  \"version\": 1,\n  \"workers\": [...]\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("paste_json_textfield"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonPasteInput.isBlank()) {
                            Toast.makeText(context, "Please paste JSON backup code first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isOperating = true
                        viewModel.restoreBackupData(
                            jsonString = jsonPasteInput,
                            clearExisting = isOverwriteMode
                        ) { success, msg ->
                            isOperating = false
                            isSuccessStatus = success
                            restoreStatusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                showPasteJsonDialog = false
                                jsonPasteInput = ""
                                refreshLocalBackups()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary)
                ) {
                    Text("Restore Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasteJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Database Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = loggedInCompany.ifBlank { "Offline Local Database" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        if (loggedInPhone.isNotBlank()) {
                            Text(
                                text = "Admin: $loggedInPhone",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = "Database",
                            tint = HaazriPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BackupStatChip(
                        title = "Staff",
                        count = "${workers.size}",
                        bgColor = Color(0xFFEFF6FF),
                        textColor = Color(0xFF1D4ED8)
                    )
                    BackupStatChip(
                        title = "Attendance",
                        count = "${attendanceRecords.size}",
                        bgColor = Color(0xFFF0FDF4),
                        textColor = Color(0xFF15803D)
                    )
                    BackupStatChip(
                        title = "Cashbook",
                        count = "${cashbookEntries.size}",
                        bgColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFFB45309)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 100% Private On-Device Data Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "100% Private On-Device Data",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        text = "All staff, attendance logs, and ledger entries are stored securely on this phone. Use the offline export tools below to create and restore your backups anytime.",
                        fontSize = 11.sp,
                        color = Color(0xFF15803D),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Operation in progress spinner
        if (isOperating) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HaazriPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Processing backup data... Please wait.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }

        // Status Message Banner
        if (restoreStatusMessage != null && !isOperating) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccessStatus) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccessStatus) Color(0xFF15803D) else Color(0xFFB91C1C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = restoreStatusMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuccessStatus) Color(0xFF15803D) else Color(0xFFB91C1C),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { restoreStatusMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION: AUTOMATIC 24-HOUR WORKMANAGER BACKUP
        Text(
            text = "Automatic 24-Hour Backup (WorkManager)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Auto-Backup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Runs silently every 24 hours in background",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { enabled ->
                            autoBackupEnabled = enabled
                            com.example.util.AutoBackupScheduler.setAutoBackupEnabled(context, enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Automatic 24-hour backup enabled" else "Automatic backup disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF253B80)
                        ),
                        modifier = Modifier.testTag("auto_backup_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Last backup details & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (lastBackupTime > 0) Color(0xFF16A34A) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lastBackupTime > 0) {
                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastBackupTime))
                                "Last Run: $dateStr"
                            } else {
                                "Last Run: Pending first 24h cycle"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        if (lastBackupSummary.isNotBlank()) {
                            Text(
                                text = lastBackupSummary,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (autoBackupEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = if (autoBackupEnabled) "Active (24h)" else "Disabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (autoBackupEnabled) Color(0xFF15803D) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Immediate trigger button
                OutlinedButton(
                    onClick = {
                        com.example.util.AutoBackupScheduler.triggerImmediateBackup(context)
                        Toast.makeText(context, "WorkManager backup job enqueued! Saving in background...", Toast.LENGTH_LONG).show()
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1200)
                            refreshAutoBackupInfo()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_trigger_workmanager_backup"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run WorkManager Backup Now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E3A8A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: CREATE OFFLINE BACKUP
        Text(
            text = "Create Offline Backup",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Export your entire database directly from SQLite into a portable JSON backup file.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action 1: Save Backup (Choose Path)
                Button(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        createDocumentLauncher.launch("Haazri_Backup_$timeStamp.json")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_backup_manual")
                ) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Backup to Device (Choose Folder)", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Share Backup File via WhatsApp / Email / Drive
                Button(
                    onClick = {
                        isOperating = true
                        restoreStatusMessage = "Preparing backup to share..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonString = viewModel.getFullBackupJson()
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileName = "Haazri_Backup_$timeStamp.json"

                                // Save file
                                val backupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
                                val localFile = File(backupsDir, fileName)
                                localFile.writeText(jsonString, Charsets.UTF_8)

                                val cacheFile = File(context.cacheDir, fileName)
                                cacheFile.writeText(jsonString, Charsets.UTF_8)

                                withContext(Dispatchers.Main) {
                                    isOperating = false
                                    refreshLocalBackups()
                                    try {
                                        val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            cacheFile
                                        )

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            putExtra(Intent.EXTRA_SUBJECT, "Attendance App Backup ($timeStamp)")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
                                    } catch (e: Exception) {
                                        val textShareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, jsonString)
                                            putExtra(Intent.EXTRA_SUBJECT, "Attendance App Backup ($timeStamp)")
                                        }
                                        context.startActivity(Intent.createChooser(textShareIntent, "Share Backup Data"))
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isOperating = false
                                    isSuccessStatus = false
                                    restoreStatusMessage = "Failed to export: ${e.localizedMessage}"
                                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaazriPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_share_backup")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Backup File (WhatsApp / Drive)", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy Code
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val jsonString = viewModel.getFullBackupJson()
                            withContext(Dispatchers.Main) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Attendance Backup JSON", jsonString)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Backup JSON code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_copy_backup")
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Raw Backup Code", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                }
            }
        }

        // SECTION 2: SAVED LOCAL BACKUPS ON THIS DEVICE
        if (localBackups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Backups on Device (${localBackups.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp)
                )
                TextButton(onClick = { refreshLocalBackups() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp), tint = HaazriPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", fontSize = 12.sp, color = HaazriPrimary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                localBackups.take(5).forEach { backupItem ->
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(backupItem.lastModified))
                    val sizeKb = String.format(Locale.getDefault(), "%.1f KB", backupItem.sizeBytes / 1024.0)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, tint = HaazriPrimary, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backupItem.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1
                                )
                                Text(
                                    text = "$dateFormatted • $sizeKb",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // 1-Tap Restore Button
                            Button(
                                onClick = {
                                    pendingRestoreFile = backupItem.file
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Share single backup
                            IconButton(
                                onClick = {
                                    try {
                                        val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            backupItem.file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            putExtra(Intent.EXTRA_SUBJECT, backupItem.name)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error sharing: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: RESTORE BACKUP FROM FILE OR CODE
        Text(
            text = "Restore Offline Backup",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select any JSON backup file from your phone storage, Google Drive, or Downloads to restore your staff and records.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Restore Mode Switch (Merge vs Overwrite)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOverwriteMode) "Overwrite / Replace Existing Data" else "Merge with Existing Data",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverwriteMode) Color(0xFFDC2626) else Color(0xFF15803D)
                            )
                            Text(
                                text = if (isOverwriteMode) "Clears current database before restoring backup" else "Appends backup data to existing database safely",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = isOverwriteMode,
                            onCheckedChange = { isOverwriteMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFDC2626),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.testTag("switch_restore_mode")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Button: Select & Import JSON File
                Button(
                    onClick = {
                        openDocumentLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_select_restore_file")
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select & Import JSON File", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button: Paste JSON Text Code
                OutlinedButton(
                    onClick = { showPasteJsonDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_paste_json_code")
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paste Backup JSON Code", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BackupStatChip(title: String, count: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

private fun saveBackupToDownloadsMediaStore(context: Context, fileName: String, jsonContent: String): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                true
            } else false
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                val file = File(downloadsDir, fileName)
                file.writeText(jsonContent, Charsets.UTF_8)
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/json"),
                    null
                )
                true
            } else false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
