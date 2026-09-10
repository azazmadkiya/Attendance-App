package com.example.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.HaazriDatabase
import com.example.data.HaazriRepository
import com.example.util.AutoBackupScheduler
import com.example.util.BackupManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val MAX_SAVED_AUTO_BACKUPS = 10
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting automatic 24-hour backup...")

            val db = HaazriDatabase.getDatabase(applicationContext)
            val repository = HaazriRepository(db)

            // Gather all data
            val backupData = repository.getAllDataForBackup()

            // Generate portable JSON string
            val jsonString = BackupManager.exportToJson(
                workers = backupData.workers,
                attendanceRecords = backupData.attendanceRecords,
                cashbookEntries = backupData.cashbookEntries,
                notificationSetting = backupData.notificationSetting
            )

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Haazri_AutoBackup_$timeStamp.json"

            // 1. Save to app internal backups directory
            val backupsDir = File(applicationContext.filesDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            val localFile = File(backupsDir, fileName)
            localFile.writeText(jsonString, Charsets.UTF_8)

            // 2. Also write to cacheDir for immediate quick-access
            val cacheFile = File(applicationContext.cacheDir, fileName)
            cacheFile.writeText(jsonString, Charsets.UTF_8)

            // 3. Save to public Downloads directory via MediaStore (zero extra permissions needed)
            saveToDownloadsMediaStore(applicationContext, fileName, jsonString)

            // 4. Prune old auto backups (retain latest MAX_SAVED_AUTO_BACKUPS to protect device storage)
            pruneOldAutoBackups(backupsDir)

            // 5. Update local record of last successful auto-backup
            AutoBackupScheduler.recordBackupSuccess(
                context = applicationContext,
                workersCount = backupData.workers.size,
                attendanceCount = backupData.attendanceRecords.size,
                cashbookCount = backupData.cashbookEntries.size,
                fileName = fileName
            )

            // 6. Notify user of safety assurance
            NotificationHelper.showBackupNotification(
                context = applicationContext,
                title = "Automatic Backup Complete",
                message = "Safe daily backup created: ${backupData.workers.size} staff & ${backupData.attendanceRecords.size} attendance records saved."
            )

            Log.d(TAG, "Automatic backup completed successfully: $fileName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Automatic backup failed", e)
            AutoBackupScheduler.recordBackupFailure(
                context = applicationContext,
                errorMessage = e.localizedMessage ?: "Unknown error"
            )

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun pruneOldAutoBackups(backupsDir: File) {
        try {
            val autoBackupFiles = backupsDir.listFiles { file ->
                file.isFile && file.name.startsWith("Haazri_AutoBackup_") && file.name.endsWith(".json")
            } ?: return

            if (autoBackupFiles.size > MAX_SAVED_AUTO_BACKUPS) {
                // Sort oldest first
                val sorted = autoBackupFiles.sortedBy { it.lastModified() }
                val filesToDeleteCount = sorted.size - MAX_SAVED_AUTO_BACKUPS
                for (i in 0 until filesToDeleteCount) {
                    sorted[i].delete()
                    Log.d(TAG, "Pruned old auto-backup: ${sorted[i].name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pruning old auto backups", e)
        }
    }

    private fun saveToDownloadsMediaStore(context: Context, fileName: String, jsonContent: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                    true
                } else false
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
            Log.e(TAG, "Failed saving backup to MediaStore downloads", e)
            false
        }
    }
}
