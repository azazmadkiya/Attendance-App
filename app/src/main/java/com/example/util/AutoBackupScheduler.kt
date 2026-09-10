package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.AutoBackupWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    const val UNIQUE_WORK_NAME = "haazri_daily_auto_backup"
    const val IMMEDIATE_WORK_NAME = "haazri_immediate_auto_backup"
    const val TAG_AUTO_BACKUP = "haazri_auto_backup_tag"

    private const val PREFS_NAME = "haazri_backup_prefs"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
    private const val KEY_LAST_BACKUP_SUMMARY = "last_backup_summary"
    private const val KEY_LAST_BACKUP_FILE = "last_backup_file"

    /**
     * Schedules periodic WorkManager task that runs once every 24 hours.
     * Uses ExistingPeriodicWorkPolicy.KEEP so an existing schedule is preserved
     * without resetting the 24-hour cycle on each app start.
     */
    fun scheduleDailyBackup(context: Context, forceReplace: Boolean = false) {
        if (!isAutoBackupEnabled(context)) {
            cancelDailyBackup(context)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(TAG_AUTO_BACKUP)
            .build()

        val policy = if (forceReplace) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            policy,
            dailyWorkRequest
        )
    }

    /**
     * Immediately triggers an on-demand WorkManager backup task in background.
     */
    fun triggerImmediateBackup(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .addTag(TAG_AUTO_BACKUP)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels the scheduled daily periodic WorkManager task.
     */
    fun cancelDailyBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyBackup(context, forceReplace = true)
        } else {
            cancelDailyBackup(context)
        }
    }

    fun recordBackupSuccess(
        context: Context,
        workersCount: Int,
        attendanceCount: Int,
        cashbookCount: Int,
        fileName: String
    ) {
        val now = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now))
        val summary = "$workersCount staff • $attendanceCount attendance logs • $cashbookCount ledger entries"

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putString(KEY_LAST_BACKUP_STATUS, "SUCCESS")
            .putString(KEY_LAST_BACKUP_SUMMARY, summary)
            .putString(KEY_LAST_BACKUP_FILE, fileName)
            .apply()
    }

    fun recordBackupFailure(context: Context, errorMessage: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_BACKUP_STATUS, "FAILED")
            .putString(KEY_LAST_BACKUP_SUMMARY, "Error: $errorMessage")
            .apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun getLastBackupStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_BACKUP_STATUS, "IDLE") ?: "IDLE"
    }

    fun getLastBackupSummary(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_BACKUP_SUMMARY, "") ?: ""
    }

    fun getLastBackupFileName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_BACKUP_FILE, "") ?: ""
    }
}
