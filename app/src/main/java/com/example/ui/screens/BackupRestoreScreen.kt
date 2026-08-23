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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(viewModel: HaazriViewModel) {
    val context = LocalContext.current
    val workers by viewModel.workers.collectAsState()
    val attendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val cashbookEntries by viewModel.cashbookEntries.collectAsState()

    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val lastCloudSyncTime by viewModel.lastCloudSyncTime.collectAsState()
    val loggedInCompany by viewModel.loggedInCompanyName.collectAsState()
    val loggedInPhone by viewModel.loggedInPhone.collectAsState()

    var isOverwriteMode by remember { mutableStateOf(false) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var jsonPasteInput by remember { mutableStateOf("") }
    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    // System File Manager Launcher to save backup file into user-chosen folder
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = viewModel.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                isSuccessStatus = true
                restoreStatusMessage = "Backup file saved successfully to File Manager!"
                Toast.makeText(context, "Backup file saved in File Manager!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isSuccessStatus = false
                restoreStatusMessage = "Failed to save file: ${e.localizedMessage}"
                Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // File picker launcher for JSON import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isNotBlank()) {
                    viewModel.restoreBackupData(
                        jsonString = jsonString,
                        clearExisting = isOverwriteMode
                    ) { success, msg ->
                        isSuccessStatus = success
                        restoreStatusMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isSuccessStatus = false
                restoreStatusMessage = "Failed to read backup file: ${e.localizedMessage}"
                Toast.makeText(context, "Error opening file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                        "Paste your exported JSON backup code below to restore your data.",
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
                        viewModel.restoreBackupData(
                            jsonString = jsonPasteInput,
                            clearExisting = isOverwriteMode
                        ) { success, msg ->
                            isSuccessStatus = success
                            restoreStatusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                showPasteJsonDialog = false
                                jsonPasteInput = ""
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
            .background(Color(0xFFF7F5EE))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status Banner Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase Cloud & Local Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Connected Account: $loggedInCompany ($loggedInPhone)",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Stats summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BackupStatChip("Workers", "${workers.size}", Color(0xFFE0E7FF), Color(0xFF1E3A8A))
                    BackupStatChip("Attendance", "${attendanceRecords.size}", Color(0xFFDCFCE7), Color(0xFF15803D))
                    BackupStatChip("Ledger", "${cashbookEntries.size}", Color(0xFFFEF3C7), Color(0xFFB45309))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // FIREBASE CLOUD REAL-TIME BACKUP CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase Cloud Live Sync",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val syncTimeFormatted = if (lastCloudSyncTime > 0) {
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastCloudSyncTime))
                } else {
                    "Auto-Sync Active"
                }

                Text(
                    text = "• Real-Time Cloud Firestore: All workers, attendance, & money entries sync automatically.\n" +
                           "• Account Profile: Stored on Firebase under $loggedInPhone.\n" +
                           "• Last Full Cloud Backup: $syncTimeFormatted",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A8A),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sync Now to Cloud
                    Button(
                        onClick = {
                            viewModel.syncAllToCloudNow { success, msg ->
                                isSuccessStatus = success
                                restoreStatusMessage = msg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_sync_cloud_now"),
                        enabled = !isCloudSyncing
                    ) {
                        if (isCloudSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync to Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Restore from Cloud
                    OutlinedButton(
                        onClick = {
                            viewModel.restoreFromCloudNow(clearExisting = isOverwriteMode) { success, msg ->
                                isSuccessStatus = success
                                restoreStatusMessage = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_restore_cloud_now"),
                        enabled = !isCloudSyncing
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                    }
                }
            }
        }

        if (restoreStatusMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: EXPORT LOCAL FILE BACKUP
        Text(
            text = "Offline File Backup & Export",
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
                    text = "Export your complete app database into a portable JSON file for offline storage or transferring between devices.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action: Share Backup File
                Button(
                    onClick = {
                        val jsonString = viewModel.exportBackupJson()
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = "Attendance_Backup_$timeStamp.json"

                        try {
                            val cacheFile = File(context.cacheDir, fileName)
                            cacheFile.writeText(jsonString)

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
                    Text("Share Backup File / Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy to Clipboard
                    OutlinedButton(
                        onClick = {
                            val jsonString = viewModel.exportBackupJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Attendance Backup JSON", jsonString)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Backup JSON code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_backup")
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }

                    // Save Local Snapshot File via File Manager & Downloads
                    OutlinedButton(
                        onClick = {
                            val jsonString = viewModel.exportBackupJson()
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileName = "Haazri_Backup_$timeStamp.json"
                            
                            try {
                                val savedToDownloads = saveBackupToDownloadsMediaStore(context, fileName, jsonString)
                                val internalFile = File(context.filesDir, fileName)
                                internalFile.writeText(jsonString)

                                if (savedToDownloads) {
                                    Toast.makeText(
                                        context,
                                        "Backup saved in Downloads!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                createDocumentLauncher.launch(fileName)
                            } catch (e: Exception) {
                                createDocumentLauncher.launch(fileName)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_local_backup")
                    ) {
                        Icon(Icons.Outlined.SaveAlt, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to File Manager", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A34A))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: RESTORE OFFLINE BACKUP
        Text(
            text = "Restore Local File Backup",
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
                    text = "Select a JSON backup file or paste your backup code to restore all workers, attendance logs, and financial records.",
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
                                text = if (isOverwriteMode) "Clears current database before restoring backup" else "Appends backup data to existing database",
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

                // Button: Select JSON File
                Button(
                    onClick = {
                        filePickerLauncher.launch("*/*")
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
                file.writeText(jsonContent)
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
