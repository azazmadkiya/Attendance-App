package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchWorkers(query: String): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: Long): Flow<Worker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    @Query("DELETE FROM workers")
    suspend fun deleteAllWorkers()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getRecordForWorkerAndDate(workerId: Long, date: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("DELETE FROM attendance_records WHERE workerId = :workerId")
    suspend fun deleteAttendanceForWorker(workerId: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()
}

@Dao
interface CashbookDao {
    @Query("SELECT * FROM cashbook_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<CashbookEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashbookEntry)

    @Delete
    suspend fun deleteEntry(entry: CashbookEntry)

    @Query("DELETE FROM cashbook_entries")
    suspend fun deleteAllCashbookEntries()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM geofence_config WHERE id = 1")
    fun getGeofenceConfig(): Flow<GeofenceConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGeofenceConfig(config: GeofenceConfig)

    @Query("SELECT * FROM notification_settings WHERE id = 1")
    fun getNotificationSettings(): Flow<NotificationSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotificationSettings(settings: NotificationSetting)
}
