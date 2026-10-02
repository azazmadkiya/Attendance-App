package com.attendance.app.azaz.data

import androidx.room.withTransaction
import com.attendance.app.azaz.util.BackupData
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
            val finalWorkers = workers.toMutableList()
            if (finalWorkers.isEmpty()) {
                val referencedWorkerIds = (attendanceRecords.map { it.workerId } + cashbookEntries.mapNotNull { it.workerId }).distinct()
                for (wId in referencedWorkerIds) {
                    if (wId > 0) {
                        finalWorkers.add(Worker(id = wId, name = "Restored Worker #$wId", wageType = "Monthly", wageRate = 0.0))
                    }
                }
            }

            val workerIdMap = mutableMapOf<Long, Long>()
            if (clearExisting) {
                db.cashbookDao().deleteAllCashbookEntries()
                db.attendanceDao().deleteAllAttendanceRecords()
                db.workerDao().deleteAllWorkers()

                finalWorkers.forEach { worker ->
                    val assignedId = db.workerDao().insertWorker(worker.copy(id = 0))
                    if (worker.id != 0L) {
                        workerIdMap[worker.id] = assignedId
                    }
                    workerIdMap[assignedId] = assignedId
                }
            } else {
                val existingWorkers = db.workerDao().getAllWorkersList()
                finalWorkers.forEach { worker ->
                    val existing = existingWorkers.find {
                        (it.phone.isNotBlank() && it.phone == worker.phone) ||
                        (it.name.equals(worker.name, ignoreCase = true) && it.wageType == worker.wageType)
                    }

                    if (existing != null) {
                        if (worker.id != 0L) {
                            workerIdMap[worker.id] = existing.id
                        }
                        workerIdMap[existing.id] = existing.id
                    } else {
                        val newId = db.workerDao().insertWorker(worker.copy(id = 0))
                        if (worker.id != 0L) {
                            workerIdMap[worker.id] = newId
                        }
                        workerIdMap[newId] = newId
                    }
                }
            }

            // Insert attendance records with remapped worker IDs and safety check
            attendanceRecords.forEach { record ->
                var remappedWorkerId = workerIdMap[record.workerId] ?: record.workerId
                if (db.workerDao().getWorkerByIdSync(remappedWorkerId) == null) {
                    remappedWorkerId = db.workerDao().insertWorker(
                        Worker(name = "Worker #${record.workerId}", wageType = "Monthly", wageRate = 0.0)
                    )
                }

                if (clearExisting) {
                    db.attendanceDao().insertOrUpdateAttendance(
                        record.copy(id = 0, workerId = remappedWorkerId)
                    )
                } else {
                    val existingRecord = db.attendanceDao().getRecordForWorkerAndDate(remappedWorkerId, record.date)
                    if (existingRecord == null) {
                        db.attendanceDao().insertOrUpdateAttendance(
                            record.copy(id = 0, workerId = remappedWorkerId)
                        )
                    }
                }
            }

            // Insert cashbook entries with remapped worker IDs and safety check
            cashbookEntries.forEach { entry ->
                val remappedWorkerId = entry.workerId?.let { originalId ->
                    var mapped = workerIdMap[originalId] ?: originalId
                    if (db.workerDao().getWorkerByIdSync(mapped) == null) {
                        mapped = db.workerDao().insertWorker(
                            Worker(name = "Worker #$originalId", wageType = "Monthly", wageRate = 0.0)
                        )
                    }
                    mapped
                }
                db.cashbookDao().insertEntry(
                    entry.copy(id = 0, workerId = remappedWorkerId)
                )
            }

            if (notificationSetting != null) {
                db.settingsDao().saveNotificationSettings(notificationSetting)
            }
        }
    }
}
