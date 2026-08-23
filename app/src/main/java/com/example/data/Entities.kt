package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val wageType: String = "Monthly", // Daily, Weekly, Monthly
    val wageRate: Double = 0.0,
    val overtimeRate: Double = 0.0,
    val upiId: String = "",
    val hajariMultiplier: String = "Off", // Off, 2x, 3.5x, 4.75x, Custom
    val overtimeMultiplier: String = "1.5x",
    val lateFine: Double = 0.0,
    val lateGracePeriodMinutes: Int = 0,
    val halfDayPayFactor: Double = 0.5,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val date: String, // YYYY-MM-DD
    val status: String, // P, A, 1/2, O
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val overtimeHours: Double = 0.0,
    val customAmount: Double = 0.0,
    val isGeofenceVerified: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "cashbook_entries")
data class CashbookEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long? = null,
    val type: String, // INCOME, EXPENSE
    val amount: Double,
    val category: String = "General",
    val date: String, // YYYY-MM-DD
    val time: String = "",
    val notes: String = ""
)

@Entity(tableName = "geofence_config")
data class GeofenceConfig(
    @PrimaryKey val id: Int = 1,
    val officeName: String = "Main HQ Office",
    val latitude: Double = 28.6139, // Default New Delhi / Admin HQ
    val longitude: Double = 77.2090,
    val radiusMeters: Float = 200f,
    val isEnabled: Boolean = true,
    val autoMarkPresent: Boolean = true
)

@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey val id: Int = 1,
    val dailyReminderEnabled: Boolean = true,
    val reminderTime: String = "09:00 AM",
    val missedCheckoutNudge: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val hideAmounts: Boolean = false
)
