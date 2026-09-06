package com.example.util

import com.example.data.AttendanceRecord
import com.example.data.CashbookEntry
import com.example.data.GeofenceConfig
import com.example.data.NotificationSetting
import com.example.data.Worker
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val workers: List<Worker>,
    val attendanceRecords: List<AttendanceRecord>,
    val cashbookEntries: List<CashbookEntry>,
    val geofenceConfig: GeofenceConfig?,
    val notificationSetting: NotificationSetting?
)

object BackupManager {

    fun exportToJson(
        workers: List<Worker>,
        attendanceRecords: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        geofenceConfig: GeofenceConfig?,
        notificationSetting: NotificationSetting?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())

        // Workers
        val workersArray = JSONArray()
        workers.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("phone", w.phone)
            obj.put("wageType", w.wageType)
            obj.put("wageRate", w.wageRate)
            obj.put("overtimeRate", w.overtimeRate)
            obj.put("upiId", w.upiId)
            obj.put("hajariMultiplier", w.hajariMultiplier)
            obj.put("overtimeMultiplier", w.overtimeMultiplier)
            obj.put("lateFine", w.lateFine)
            obj.put("lateGracePeriodMinutes", w.lateGracePeriodMinutes)
            obj.put("halfDayPayFactor", w.halfDayPayFactor)
            obj.put("notes", w.notes)
            obj.put("createdAt", w.createdAt)
            workersArray.put(obj)
        }
        root.put("workers", workersArray)

        // Attendance Records
        val attendanceArray = JSONArray()
        attendanceRecords.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("workerId", a.workerId)
            obj.put("date", a.date)
            obj.put("status", a.status)
            obj.put("checkInTime", a.checkInTime)
            obj.put("checkOutTime", a.checkOutTime)
            obj.put("overtimeHours", a.overtimeHours)
            obj.put("customAmount", a.customAmount)
            obj.put("isGeofenceVerified", a.isGeofenceVerified)
            obj.put("latitude", a.latitude)
            obj.put("longitude", a.longitude)
            obj.put("notes", a.notes)
            attendanceArray.put(obj)
        }
        root.put("attendanceRecords", attendanceArray)

        // Cashbook Entries
        val cashbookArray = JSONArray()
        cashbookEntries.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("workerId", c.workerId ?: -1L)
            obj.put("type", c.type)
            obj.put("amount", c.amount)
            obj.put("category", c.category)
            obj.put("date", c.date)
            obj.put("time", c.time)
            obj.put("notes", c.notes)
            cashbookArray.put(obj)
        }
        root.put("cashbookEntries", cashbookArray)

        // Geofence Config
        if (geofenceConfig != null) {
            val geoObj = JSONObject()
            geoObj.put("id", geofenceConfig.id)
            geoObj.put("officeName", geofenceConfig.officeName)
            geoObj.put("latitude", geofenceConfig.latitude)
            geoObj.put("longitude", geofenceConfig.longitude)
            geoObj.put("radiusMeters", geofenceConfig.radiusMeters.toDouble())
            geoObj.put("isEnabled", geofenceConfig.isEnabled)
            geoObj.put("autoMarkPresent", geofenceConfig.autoMarkPresent)
            root.put("geofenceConfig", geoObj)
        }

        // Notification Setting
        if (notificationSetting != null) {
            val notifObj = JSONObject()
            notifObj.put("id", notificationSetting.id)
            notifObj.put("dailyReminderEnabled", notificationSetting.dailyReminderEnabled)
            notifObj.put("reminderTime", notificationSetting.reminderTime)
            notifObj.put("missedCheckoutNudge", notificationSetting.missedCheckoutNudge)
            notifObj.put("weeklyReportEnabled", notificationSetting.weeklyReportEnabled)
            notifObj.put("hideAmounts", notificationSetting.hideAmounts)
            root.put("notificationSetting", notifObj)
        }

        return root.toString(2)
    }

    fun importFromJson(jsonString: String): BackupData {
        val cleanJson = jsonString.trim().trim('\uFEFF')
        val root = JSONObject(cleanJson)

        val workers = mutableListOf<Worker>()
        if (root.has("workers")) {
            val workersArray = root.getJSONArray("workers")
            for (i in 0 until workersArray.length()) {
                val obj = workersArray.getJSONObject(i)
                workers.add(
                    Worker(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "Worker"),
                        phone = obj.optString("phone", ""),
                        wageType = obj.optString("wageType", "Monthly"),
                        wageRate = obj.optDouble("wageRate", 0.0),
                        overtimeRate = obj.optDouble("overtimeRate", 0.0),
                        upiId = obj.optString("upiId", ""),
                        hajariMultiplier = obj.optString("hajariMultiplier", "Off"),
                        overtimeMultiplier = obj.optString("overtimeMultiplier", "1.5x"),
                        lateFine = obj.optDouble("lateFine", 0.0),
                        lateGracePeriodMinutes = obj.optInt("lateGracePeriodMinutes", 0),
                        halfDayPayFactor = obj.optDouble("halfDayPayFactor", 0.5),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val attendanceRecords = mutableListOf<AttendanceRecord>()
        if (root.has("attendanceRecords")) {
            val attendanceArray = root.getJSONArray("attendanceRecords")
            for (i in 0 until attendanceArray.length()) {
                val obj = attendanceArray.getJSONObject(i)
                attendanceRecords.add(
                    AttendanceRecord(
                        id = obj.optLong("id", 0L),
                        workerId = obj.optLong("workerId", 0L),
                        date = obj.optString("date", ""),
                        status = obj.optString("status", "P"),
                        checkInTime = obj.optString("checkInTime", ""),
                        checkOutTime = obj.optString("checkOutTime", ""),
                        overtimeHours = obj.optDouble("overtimeHours", 0.0),
                        customAmount = obj.optDouble("customAmount", 0.0),
                        isGeofenceVerified = obj.optBoolean("isGeofenceVerified", false),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val cashbookEntries = mutableListOf<CashbookEntry>()
        if (root.has("cashbookEntries")) {
            val cashbookArray = root.getJSONArray("cashbookEntries")
            for (i in 0 until cashbookArray.length()) {
                val obj = cashbookArray.getJSONObject(i)
                val wId = obj.optLong("workerId", -1L)
                cashbookEntries.add(
                    CashbookEntry(
                        id = obj.optLong("id", 0L),
                        workerId = if (wId == -1L) null else wId,
                        type = obj.optString("type", "EXPENSE"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "General"),
                        date = obj.optString("date", ""),
                        time = obj.optString("time", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        var geofenceConfig: GeofenceConfig? = null
        if (root.has("geofenceConfig")) {
            val obj = root.getJSONObject("geofenceConfig")
            geofenceConfig = GeofenceConfig(
                id = obj.optInt("id", 1),
                officeName = obj.optString("officeName", "Main HQ Office"),
                latitude = obj.optDouble("latitude", 28.6139),
                longitude = obj.optDouble("longitude", 77.2090),
                radiusMeters = obj.optDouble("radiusMeters", 200.0).toFloat(),
                isEnabled = obj.optBoolean("isEnabled", true),
                autoMarkPresent = obj.optBoolean("autoMarkPresent", true)
            )
        }

        var notificationSetting: NotificationSetting? = null
        if (root.has("notificationSetting")) {
            val obj = root.getJSONObject("notificationSetting")
            notificationSetting = NotificationSetting(
                id = obj.optInt("id", 1),
                dailyReminderEnabled = obj.optBoolean("dailyReminderEnabled", true),
                reminderTime = obj.optString("reminderTime", "09:00 AM"),
                missedCheckoutNudge = obj.optBoolean("missedCheckoutNudge", true),
                weeklyReportEnabled = obj.optBoolean("weeklyReportEnabled", true),
                hideAmounts = obj.optBoolean("hideAmounts", false)
            )
        }

        return BackupData(
            workers = workers,
            attendanceRecords = attendanceRecords,
            cashbookEntries = cashbookEntries,
            geofenceConfig = geofenceConfig,
            notificationSetting = notificationSetting
        )
    }
}
