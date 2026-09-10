package com.example.data

import androidx.room.withTransaction
import com.example.util.BackupData
import kotlinx.coroutines.flow.Flow

class HaazriRepository(
    private val db: HaazriDatabase
) {
    val allWorkers: Flow<List<Worker>> = db.workerDao().getAllWorkers()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = db.attendanceDao().getAllAttendanceRecords()
    val allCashbookEntries: Flow<List<CashbookEntry>> = db.cashbookDao().getAllEntries()
    val notificationSettings: Flow<NotificationSetting?> = db.settingsDao().getNotificationSettings()

    fun searchWorkers(query: String): Flow<List<Worker>> = db.workerDao().searchWorkers(query)

    fun getWorkerById(id: Long): Flow<Worker?> = db.workerDao().getWorkerById(id)

    suspend fun insertWorker(worker: Worker): Long {
        val id = db.workerDao().insertWorker(worker)
        return id
    }

    suspend fun updateWorker(worker: Worker) {
        db.workerDao().updateWorker(worker)
    }

    suspend fun deleteWorker(worker: Worker) {
        db.attendanceDao().deleteAttendanceForWorker(worker.id)
        db.workerDao().deleteWorker(worker)
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
            notes = newNotes
        ) ?: AttendanceRecord(
            workerId = workerId,
            date = date,
            status = status,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            customAmount = newCustomAmount,
            notes = newNotes
        )
        db.attendanceDao().insertOrUpdateAttendance(record)
    }

    suspend fun markAllWorkersPresent(date: String, workers: List<Worker>) {
        db.withTransaction {
            workers.forEach { worker ->
                setWorkerAttendance(worker.id, date, "P", checkInTime = "09:00 AM")
            }
        }
    }

    suspend fun insertCashbookEntry(entry: CashbookEntry) {
        db.cashbookDao().insertEntry(entry)
    }

    suspend fun deleteCashbookEntry(entry: CashbookEntry) = db.cashbookDao().deleteEntry(entry)

    suspend fun saveNotificationSettings(settings: NotificationSetting) = db.settingsDao().saveNotificationSettings(settings)

    suspend fun getAllDataForBackup(): BackupData {
        val workersList = db.workerDao().getAllWorkersList()
        val attendanceList = db.attendanceDao().getAllAttendanceList()
        val cashbookList = db.cashbookDao().getAllEntriesList()
        val notif = db.settingsDao().getNotificationSettingsSync()
        return BackupData(
            workers = workersList,
            attendanceRecords = attendanceList,
            cashbookEntries = cashbookList,
            notificationSetting = notif
        )
    }

    suspend fun restoreBackupData(
        workers: List<Worker>,
        attendanceRecords: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        notificationSetting: NotificationSetting?,
        clearExisting: Boolean
    ) {
        db.withTransaction {
            val workerIdMap = mutableMapOf<Long, Long>()
            if (clearExisting) {
                db.cashbookDao().deleteAllCashbookEntries()
                db.attendanceDao().deleteAllAttendanceRecords()
                db.workerDao().deleteAllWorkers()

                // Overwrite mode: insert each worker freshly to obtain verified clean IDs
                workers.forEach { worker ->
                    val assignedId = db.workerDao().insertWorker(worker.copy(id = 0))
                    if (worker.id != 0L) {
                        workerIdMap[worker.id] = assignedId
                    }
                    workerIdMap[assignedId] = assignedId
                }

                // Insert attendance records with remapped worker IDs
                attendanceRecords.forEach { record ->
                    val remappedWorkerId = workerIdMap[record.workerId] ?: record.workerId
                    db.attendanceDao().insertOrUpdateAttendance(
                        record.copy(id = 0, workerId = remappedWorkerId)
                    )
                }

                // Insert cashbook entries with remapped worker IDs
                cashbookEntries.forEach { entry ->
                    val remappedWorkerId = entry.workerId?.let { workerIdMap[it] ?: it }
                    db.cashbookDao().insertEntry(
                        entry.copy(id = 0, workerId = remappedWorkerId)
                    )
                }
            } else {
                // Merge mode: check existing workers to prevent duplicate creation
                val existingWorkers = db.workerDao().getAllWorkersList()

                workers.forEach { worker ->
                    val existing = existingWorkers.find {
                        (it.phone.isNotBlank() && it.phone == worker.phone) ||
                        (it.name.equals(worker.name, ignoreCase = true) && it.wageType == worker.wageType)
                    }

                    if (existing != null) {
                        // Re-use existing worker id
                        if (worker.id != 0L) {
                            workerIdMap[worker.id] = existing.id
                        }
                        workerIdMap[existing.id] = existing.id
                    } else {
                        // New worker: insert with new auto-generated ID
                        val newId = db.workerDao().insertWorker(worker.copy(id = 0))
                        if (worker.id != 0L) {
                            workerIdMap[worker.id] = newId
                        }
                        workerIdMap[newId] = newId
                    }
                }

                // Merge attendance records without duplicating same date for same worker
                attendanceRecords.forEach { record ->
                    val remappedWorkerId = workerIdMap[record.workerId] ?: record.workerId
                    val existingRecord = db.attendanceDao().getRecordForWorkerAndDate(remappedWorkerId, record.date)
                    if (existingRecord == null) {
                        db.attendanceDao().insertOrUpdateAttendance(
                            record.copy(id = 0, workerId = remappedWorkerId)
                        )
                    }
                }

                // Merge cashbook entries
                cashbookEntries.forEach { entry ->
                    val remappedWorkerId = entry.workerId?.let { workerIdMap[it] ?: it }
                    db.cashbookDao().insertEntry(
                        entry.copy(id = 0, workerId = remappedWorkerId)
                    )
                }
            }

            if (notificationSetting != null) {
                db.settingsDao().saveNotificationSettings(notificationSetting)
            }
        }
    }
}
