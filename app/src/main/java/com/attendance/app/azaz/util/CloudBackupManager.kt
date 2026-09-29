package com.attendance.app.azaz.util

import android.content.Context
import android.util.Log
import com.attendance.app.azaz.data.HaazriRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

object CloudBackupManager {

    private const val TAG = "CloudBackupManager"
    private const val PREFS_NAME = "cloud_backup_prefs"
    private const val KEY_LAST_SYNC_TIME = "last_cloud_sync_time"
    private const val KEY_LAST_SYNC_STATUS = "last_cloud_sync_status"
    private const val KEY_AUTO_SYNC_ENABLED = "auto_cloud_sync_enabled"

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun getLastSyncStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SYNC_STATUS, "Cloud Protection Active") ?: "Cloud Protection Active"
    }

    /**
     * Backs up the entire local database (workers, attendance, cashbook, settings) to Cloud Firestore.
     */
    suspend fun saveBackupToCloud(
        context: Context,
        repository: HaazriRepository,
        authManager: AuthenticationManager
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.getCurrentUser()
            val userInfo = authManager.getCurrentUserInfo()
            val authPrefs = context.getSharedPreferences("haazri_auth_prefs", Context.MODE_PRIVATE)

            val emailPref = authPrefs.getString("user_email", "") ?: ""
            val phonePref = authPrefs.getString("user_phone", "") ?: ""

            val uid = user?.uid?.takeIf { it.isNotBlank() }
            val email = userInfo?.email?.takeIf { it.isNotBlank() } ?: emailPref.takeIf { it.isNotBlank() }
            val phone = phonePref.takeIf { it.isNotBlank() }

            val userIdentifier = uid ?: email ?: phone

            if (userIdentifier.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Please log in or register to enable Cloud Backup & Auto Recovery."))
            }

            val backupData = repository.getAllDataForBackup()
            val jsonString = BackupManager.exportToJson(backupData)

            // Save local file copy
            val localBackupDir = File(context.filesDir, "cloud_auto_recovery")
            if (!localBackupDir.exists()) localBackupDir.mkdirs()
            val localFile = File(localBackupDir, "auto_cloud_backup.json")
            localFile.writeText(jsonString)

            var isCloudSaved = false
            try {
                val firestore = FirebaseFirestore.getInstance()
                val backupDoc = hashMapOf(
                    "json_data" to jsonString,
                    "updatedAt" to System.currentTimeMillis(),
                    "workerCount" to backupData.workers.size,
                    "attendanceCount" to backupData.attendanceRecords.size,
                    "cashbookCount" to backupData.cashbookEntries.size,
                    "userEmail" to (email ?: ""),
                    "userPhone" to (phone ?: "")
                )

                firestore.collection("cloud_backups")
                    .document(userIdentifier)
                    .set(backupDoc, SetOptions.merge())
                    .await()
                isCloudSaved = true
            } catch (e: Exception) {
                Log.w(TAG, "Firestore cloud save exception: ${e.message}")
            }

            val syncTime = System.currentTimeMillis()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, syncTime)
                .putString(KEY_LAST_SYNC_STATUS, if (isCloudSaved) "Success (Cloud & Device Saved)" else "Saved Locally (Pending Network)")
                .apply()

            val msg = "Backup complete: ${backupData.workers.size} workers, ${backupData.attendanceRecords.size} attendance records & ${backupData.cashbookEntries.size} cashbook entries secured!"
            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup to cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Recovers data from Cloud Firestore or local cloud auto-recovery backup file.
     */
    suspend fun recoverDataFromCloud(
        context: Context,
        repository: HaazriRepository,
        authManager: AuthenticationManager,
        isOverwrite: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.getCurrentUser()
            val userInfo = authManager.getCurrentUserInfo()
            val authPrefs = context.getSharedPreferences("haazri_auth_prefs", Context.MODE_PRIVATE)

            val emailPref = authPrefs.getString("user_email", "") ?: ""
            val phonePref = authPrefs.getString("user_phone", "") ?: ""

            val uid = user?.uid?.takeIf { it.isNotBlank() }
            val email = userInfo?.email?.takeIf { it.isNotBlank() } ?: emailPref.takeIf { it.isNotBlank() }
            val phone = phonePref.takeIf { it.isNotBlank() }

            val userIdentifier = uid ?: email ?: phone

            var jsonString: String? = null

            // 1. First try fetching from Cloud Firestore
            if (!userIdentifier.isNullOrBlank()) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val doc = withTimeoutOrNull(6000) {
                        firestore.collection("cloud_backups")
                            .document(userIdentifier)
                            .get()
                            .await()
                    }
                    if (doc != null && doc.exists()) {
                        jsonString = doc.getString("json_data")
                        Log.i(TAG, "Retrieved cloud backup from Firestore for $userIdentifier")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch cloud backup from Firestore: ${e.message}")
                }
            }

            // 2. Fallback to local auto-recovery backup file if offline/uninstalled
            if (jsonString.isNullOrBlank()) {
                val localFile = File(File(context.filesDir, "cloud_auto_recovery"), "auto_cloud_backup.json")
                if (localFile.exists()) {
                    jsonString = localFile.readText()
                    Log.i(TAG, "Retrieved cloud backup from local file fallback")
                }
            }

            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No cloud backup found for this account. Make sure you log in with the same Email or Mobile number."))
            }

            val backupData = BackupManager.importFromJson(jsonString)
            repository.restoreBackupData(
                workers = backupData.workers,
                attendanceRecords = backupData.attendanceRecords,
                cashbookEntries = backupData.cashbookEntries,
                notificationSetting = backupData.notificationSetting,
                clearExisting = isOverwrite
            )

            val syncTime = System.currentTimeMillis()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, syncTime)
                .putString(KEY_LAST_SYNC_STATUS, "Data Recovered Successfully")
                .apply()

            val msg = "Data Recovery Successful! Recovered ${backupData.workers.size} workers, ${backupData.attendanceRecords.size} attendance records, and ${backupData.cashbookEntries.size} cashbook entries."
            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recover data from cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically attempts to recover cloud data if local workers list is empty upon login / reinstall.
     */
    suspend fun autoRecoverIfEmpty(
        context: Context,
        repository: HaazriRepository,
        authManager: AuthenticationManager
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentBackup = repository.getAllDataForBackup()
                if (currentBackup.workers.isEmpty()) {
                    Log.i(TAG, "Local database is empty. Attempting auto cloud recovery on login/reinstall...")
                    recoverDataFromCloud(context, repository, authManager, isOverwrite = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto recovery on empty database skipped/warning: ${e.message}")
            }
        }
    }
}
