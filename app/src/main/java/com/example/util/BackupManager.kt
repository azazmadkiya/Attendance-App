package com.example.util

import com.example.data.AttendanceRecord
import com.example.data.CashbookEntry
import com.example.data.NotificationSetting
import com.example.data.Worker
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val workers: List<Worker>,
    val attendanceRecords: List<AttendanceRecord>,
    val cashbookEntries: List<CashbookEntry>,
    val notificationSetting: NotificationSetting?
)

object BackupManager {

    fun exportToJson(
        workers: List<Worker>,
        attendanceRecords: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        notificationSetting: NotificationSetting?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Attendance & Payroll")
        root.put("exportTimestamp", System.currentTimeMillis())

        // Summary metadata for easy verification
        val meta = JSONObject()
        meta.put("totalWorkers", workers.size)
        meta.put("totalAttendanceRecords", attendanceRecords.size)
        meta.put("totalCashbookEntries", cashbookEntries.size)
        root.put("meta", meta)

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

    fun importFromJson(rawJson: String): BackupData {
        // Strip BOM and excess whitespace
        val cleanInput = rawJson.trim().removePrefix("\uFEFF")

        val workers = mutableListOf<Worker>()
        val attendanceRecords = mutableListOf<AttendanceRecord>()
        val cashbookEntries = mutableListOf<CashbookEntry>()
        var notificationSetting: NotificationSetting? = null

        // Handle if user pasted directly a JSON Array of workers: [ {...}, {...} ]
        if (cleanInput.startsWith("[")) {
            val array = JSONArray(cleanInput)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseWorker(obj)?.let { workers.add(it) }
            }
            return BackupData(workers, attendanceRecords, cashbookEntries, notificationSetting)
        }

        // Find outer JSON object boundaries
        val startIndex = cleanInput.indexOf('{')
        val endIndex = cleanInput.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw IllegalArgumentException("Invalid backup format: No valid JSON object found.")
        }

        val jsonString = cleanInput.substring(startIndex, endIndex + 1)
        val root = JSONObject(jsonString)

        // Workers parsing (supports "workers", "staff", "employees")
        val workersArray = root.optJSONArray("workers")
            ?: root.optJSONArray("staff")
            ?: root.optJSONArray("employees")

        if (workersArray != null) {
            for (i in 0 until workersArray.length()) {
                val obj = workersArray.optJSONObject(i) ?: continue
                parseWorker(obj)?.let { workers.add(it) }
            }
        }

        // Attendance parsing (supports "attendanceRecords", "attendance", "records")
        val attendanceArray = root.optJSONArray("attendanceRecords")
            ?: root.optJSONArray("attendance")
            ?: root.optJSONArray("records")

        if (attendanceArray != null) {
            for (i in 0 until attendanceArray.length()) {
                val obj = attendanceArray.optJSONObject(i) ?: continue
                val id = optFlexibleLong(obj, "id", 0L)
                val workerId = optFlexibleLong(obj, "workerId", 0L)
                val date = obj.optString("date", "")
                if (date.isNotBlank()) {
                    attendanceRecords.add(
                        AttendanceRecord(
                            id = id,
                            workerId = workerId,
                            date = date,
                            status = obj.optString("status", "P"),
                            checkInTime = obj.optString("checkInTime", ""),
                            checkOutTime = obj.optString("checkOutTime", ""),
                            overtimeHours = optFlexibleDouble(obj, "overtimeHours", 0.0),
                            customAmount = optFlexibleDouble(obj, "customAmount", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }
        }

        // Cashbook parsing (supports "cashbookEntries", "cashbook", "ledger", "expenses")
        val cashbookArray = root.optJSONArray("cashbookEntries")
            ?: root.optJSONArray("cashbook")
            ?: root.optJSONArray("ledger")
            ?: root.optJSONArray("expenses")

        if (cashbookArray != null) {
            for (i in 0 until cashbookArray.length()) {
                val obj = cashbookArray.optJSONObject(i) ?: continue
                val rawWId = optFlexibleLong(obj, "workerId", -1L)
                val date = obj.optString("date", "")
                cashbookEntries.add(
                    CashbookEntry(
                        id = optFlexibleLong(obj, "id", 0L),
                        workerId = if (rawWId <= 0L) null else rawWId,
                        type = obj.optString("type", "EXPENSE"),
                        amount = optFlexibleDouble(obj, "amount", 0.0),
                        category = obj.optString("category", "General"),
                        date = if (date.isNotBlank()) date else "2026-01-01",
                        time = obj.optString("time", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        // Notification Setting
        if (root.has("notificationSetting")) {
            val obj = root.optJSONObject("notificationSetting")
            if (obj != null) {
                notificationSetting = NotificationSetting(
                    id = optFlexibleInt(obj, "id", 1),
                    dailyReminderEnabled = optFlexibleBoolean(obj, "dailyReminderEnabled", true),
                    reminderTime = obj.optString("reminderTime", "09:00 AM"),
                    missedCheckoutNudge = optFlexibleBoolean(obj, "missedCheckoutNudge", true),
                    weeklyReportEnabled = optFlexibleBoolean(obj, "weeklyReportEnabled", true),
                    hideAmounts = optFlexibleBoolean(obj, "hideAmounts", false)
                )
            }
        }

        if (workers.isEmpty() && attendanceRecords.isEmpty() && cashbookEntries.isEmpty()) {
            throw IllegalArgumentException("The backup file contains no valid workers, attendance, or cashbook records.")
        }

        return BackupData(
            workers = workers,
            attendanceRecords = attendanceRecords,
            cashbookEntries = cashbookEntries,
            notificationSetting = notificationSetting
        )
    }

    private fun parseWorker(obj: JSONObject): Worker? {
        val name = obj.optString("name", "").trim()
        if (name.isBlank()) return null
        return Worker(
            id = optFlexibleLong(obj, "id", 0L),
            name = name,
            phone = obj.optString("phone", "").trim(),
            wageType = obj.optString("wageType", "Monthly"),
            wageRate = optFlexibleDouble(obj, "wageRate", 0.0),
            overtimeRate = optFlexibleDouble(obj, "overtimeRate", 0.0),
            upiId = obj.optString("upiId", ""),
            hajariMultiplier = obj.optString("hajariMultiplier", "Off"),
            overtimeMultiplier = obj.optString("overtimeMultiplier", "1.5x"),
            lateFine = optFlexibleDouble(obj, "lateFine", 0.0),
            lateGracePeriodMinutes = optFlexibleInt(obj, "lateGracePeriodMinutes", 0),
            halfDayPayFactor = optFlexibleDouble(obj, "halfDayPayFactor", 0.5),
            notes = obj.optString("notes", ""),
            createdAt = optFlexibleLong(obj, "createdAt", System.currentTimeMillis())
        )
    }

    private fun optFlexibleDouble(obj: JSONObject, key: String, fallback: Double): Double {
        if (!obj.has(key)) return fallback
        val value = obj.opt(key) ?: return fallback
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun optFlexibleLong(obj: JSONObject, key: String, fallback: Long): Long {
        if (!obj.has(key)) return fallback
        val value = obj.opt(key) ?: return fallback
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun optFlexibleInt(obj: JSONObject, key: String, fallback: Int): Int {
        if (!obj.has(key)) return fallback
        val value = obj.opt(key) ?: return fallback
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun optFlexibleBoolean(obj: JSONObject, key: String, fallback: Boolean): Boolean {
        if (!obj.has(key)) return fallback
        val value = obj.opt(key) ?: return fallback
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            is Number -> value.toInt() != 0
            else -> fallback
        }
    }
}
