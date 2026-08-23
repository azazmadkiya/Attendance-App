package com.example.data

import kotlinx.coroutines.flow.Flow

class HaazriRepository(
    private val db: HaazriDatabase,
    private val cloudSync: FirebaseCloudSyncManager = FirebaseCloudSyncManager()
) {
    val allWorkers: Flow<List<Worker>> = db.workerDao().getAllWorkers()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = db.attendanceDao().getAllAttendanceRecords()
    val allCashbookEntries: Flow<List<CashbookEntry>> = db.cashbookDao().getAllEntries()
    val geofenceConfig: Flow<GeofenceConfig?> = db.settingsDao().getGeofenceConfig()
    val notificationSettings: Flow<NotificationSetting?> = db.settingsDao().getNotificationSettings()

    fun searchWorkers(query: String): Flow<List<Worker>> = db.workerDao().searchWorkers(query)

    fun getWorkerById(id: Long): Flow<Worker?> = db.workerDao().getWorkerById(id)

    suspend fun insertWorker(worker: Worker): Long {
        val id = db.workerDao().insertWorker(worker)
        val savedWorker = worker.copy(id = id)
        cloudSync.syncWorkerToCloud(savedWorker)
        return id
    }

    suspend fun updateWorker(worker: Worker) {
        db.workerDao().updateWorker(worker)
        cloudSync.syncWorkerToCloud(worker)
    }

    suspend fun deleteWorker(worker: Worker) {
        db.attendanceDao().deleteAttendanceForWorker(worker.id)
        db.workerDao().deleteWorker(worker)
        cloudSync.deleteWorkerFromCloud(worker.id)
    }

    suspend fun deleteAllWorkers() {
        db.attendanceDao().deleteAllAttendanceRecords()
        db.workerDao().deleteAllWorkers()
    }

    suspend fun clearAllLocalData() {
        db.cashbookDao().deleteAllCashbookEntries()
        db.attendanceDao().deleteAllAttendanceRecords()
        db.workerDao().deleteAllWorkers()
    }

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> = db.attendanceDao().getAttendanceForDate(date)

    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceRecord>> = db.attendanceDao().getAttendanceForWorker(workerId)

    suspend fun setWorkerAttendance(
        workerId: Long,
        date: String,
        status: String,
        checkInTime: String = "",
        checkOutTime: String = "",
        customAmount: Double? = null,
        isGeofenceVerified: Boolean = false,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        notes: String? = null
    ) {
        val existing = db.attendanceDao().getRecordForWorkerAndDate(workerId, date)
        val newCustomAmount = customAmount ?: existing?.customAmount ?: 0.0
        val newNotes = notes ?: existing?.notes ?: ""
        val record = existing?.copy(
            status = status,
            checkInTime = if (checkInTime.isNotEmpty()) checkInTime else existing.checkInTime,
            checkOutTime = if (checkOutTime.isNotEmpty()) checkOutTime else existing.checkOutTime,
            customAmount = newCustomAmount,
            isGeofenceVerified = isGeofenceVerified || existing.isGeofenceVerified,
            latitude = if (latitude != 0.0) latitude else existing.latitude,
            longitude = if (longitude != 0.0) longitude else existing.longitude,
            notes = newNotes
        ) ?: AttendanceRecord(
            workerId = workerId,
            date = date,
            status = status,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            customAmount = newCustomAmount,
            isGeofenceVerified = isGeofenceVerified,
            latitude = latitude,
            longitude = longitude,
            notes = newNotes
        )
        db.attendanceDao().insertOrUpdateAttendance(record)
        cloudSync.syncAttendanceToCloud(record)
    }

    suspend fun markAllWorkersPresent(date: String, workers: List<Worker>) {
        workers.forEach { worker ->
            setWorkerAttendance(worker.id, date, "P", checkInTime = "09:00 AM")
        }
    }

    suspend fun insertCashbookEntry(entry: CashbookEntry) {
        db.cashbookDao().insertEntry(entry)
        cloudSync.syncCashbookToCloud(entry)
    }

    suspend fun deleteCashbookEntry(entry: CashbookEntry) = db.cashbookDao().deleteEntry(entry)

    suspend fun saveGeofenceConfig(config: GeofenceConfig) = db.settingsDao().saveGeofenceConfig(config)

    suspend fun saveNotificationSettings(settings: NotificationSetting) = db.settingsDao().saveNotificationSettings(settings)

    suspend fun restoreBackupData(
        workers: List<Worker>,
        attendanceRecords: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        geofenceConfig: GeofenceConfig?,
        notificationSetting: NotificationSetting?,
        clearExisting: Boolean
    ) {
        if (clearExisting) {
            db.cashbookDao().deleteAllCashbookEntries()
            db.attendanceDao().deleteAllAttendanceRecords()
            db.workerDao().deleteAllWorkers()
        }

        workers.forEach { db.workerDao().insertWorker(it) }
        attendanceRecords.forEach { db.attendanceDao().insertOrUpdateAttendance(it) }
        cashbookEntries.forEach { db.cashbookDao().insertEntry(it) }
        if (geofenceConfig != null) db.settingsDao().saveGeofenceConfig(geofenceConfig)
        if (notificationSetting != null) db.settingsDao().saveNotificationSettings(notificationSetting)

        cloudSync.syncFullDatabaseToCloud(workers, attendanceRecords, cashbookEntries)
    }

    suspend fun syncAllToCloud(accountPhone: String? = null): Boolean {
        val currentWorkers = db.workerDao().getAllWorkers()
        // Read current state
        return true
    }
}
