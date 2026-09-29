package com.attendance.app.azaz.util

import android.content.Context
import androidx.work.*
import com.attendance.app.azaz.worker.AutoBackupWorker
import java.io.File
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val PREFS_NAME = "auto_backup_prefs"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_TIME = "last_backup_time"
    private const val KEY_LAST_SUMMARY = "last_backup_summary"
    private const val KEY_LAST_STATUS = "last_backup_status"
    private const val KEY_LAST_FILE = "last_backup_file"

    fun scheduleDailyBackup(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AutoOfflineBackup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyBackup(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("AutoOfflineBackup")
        }
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_TIME, 0L)
    }

    fun getLastBackupSummary(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SUMMARY, "Daily offline backup active") ?: "Daily offline backup active"
    }

    fun getLastBackupStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_STATUS, "Success") ?: "Success"
    }

    fun getLastBackupFileName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_FILE, "Haazri_Daily_Backup.json") ?: "Haazri_Daily_Backup.json"
    }

    fun updateBackupDetails(context: Context, summary: String, fileName: String, status: String = "Success") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_SUMMARY, summary)
            .putString(KEY_LAST_FILE, fileName)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    fun triggerImmediateBackup(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

