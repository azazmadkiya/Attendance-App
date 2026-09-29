package com.attendance.app.azaz.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoBackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return Result.success()
    }
}
