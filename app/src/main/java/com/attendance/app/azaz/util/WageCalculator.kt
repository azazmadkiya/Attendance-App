package com.attendance.app.azaz.util

import com.attendance.app.azaz.data.AttendanceRecord
import com.attendance.app.azaz.data.Worker
import com.attendance.app.azaz.viewmodel.MonthlySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val Double.netDailyWage: Double get() = this

object WageCalculator {

    /**
     * Calculates the daily base rate for a worker according to their wage type:
     * - Daily: wageRate directly
     * - Weekly: wageRate / 7.0
     * - Monthly: wageRate / daysInMonth (e.g. 31 in Oct, 30 in Sep, 28/29 in Feb, default 30.0)
     */
    fun calculateDailyBaseRate(worker: Worker, dateOrMonth: String = ""): Double {
        return when (worker.wageType.trim()) {
            "Daily" -> worker.wageRate
            "Weekly" -> worker.wageRate / 7.0
            "Monthly" -> {
                val daysInMonth = getDaysInMonth(dateOrMonth)
                worker.wageRate / daysInMonth
            }
            else -> worker.wageRate
        }
    }

    fun calculateDailyBaseRate(worker: Worker): Double {
        return calculateDailyBaseRate(worker, "")
    }

    /**
     * Returns the actual number of days in the specified month (or date) or 30.0 as fallback.
     */
    fun getDaysInMonth(dateOrMonth: String): Double {
        if (dateOrMonth.isBlank()) return 30.0
        return try {
            val parts = dateOrMonth.split("-")
            if (parts.size >= 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month - 1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
            } else {
                30.0
            }
        } catch (_: Exception) {
            30.0
        }
    }

    fun getHajariFactor(worker: Worker): Double {
        return when (worker.hajariMultiplier) {
            "2x" -> 2.0
            "3.5x" -> 3.5
            "4.75x" -> 4.75
            "Custom" -> 1.5
            else -> 1.0 // "Off"
        }
    }

    fun getOvertimeFactor(worker: Worker): Double {
        val cleaned = worker.overtimeMultiplier.replace("x", "").trim()
        return cleaned.toDoubleOrNull() ?: 1.5
    }

    private fun isCheckInLate(checkInTimeStr: String, gracePeriodMinutes: Int): Boolean {
        return try {
            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val checkInDate = format.parse(checkInTimeStr) ?: return false
            val standardStart = format.parse("09:00 AM") ?: return false
            val diffMs = checkInDate.time - standardStart.time
            val diffMinutes = diffMs / (1000 * 60)
            diffMinutes > gracePeriodMinutes
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Calculates the net daily wage for a worker on a single day.
     */
    fun calculateDailyWage(
        worker: Worker,
        status: String,
        customAmount: Double = 0.0,
        date: String = "",
        overtimeHours: Double = 0.0,
        checkInTime: String = ""
    ): Double {
        if (customAmount > 0.0) return customAmount

        val dailyBaseRate = calculateDailyBaseRate(worker, date)
        val statusPayFactor = when (status.uppercase()) {
            "PRESENT", "P" -> 1.0
            "HALF", "HALF-DAY", "H", "1/2" -> worker.halfDayPayFactor
            "OVERTIME", "OT" -> 1.0
            else -> 0.0
        }
        val rawBasePay = dailyBaseRate * statusPayFactor

        val hajariFactor = getHajariFactor(worker)
        val multiplierBonus = if ((status.uppercase() == "P" || status.uppercase() == "PRESENT") && hajariFactor > 1.0) {
            rawBasePay * (hajariFactor - 1.0)
        } else 0.0

        val hourlyRate = if (worker.overtimeRate > 0.0) {
            worker.overtimeRate
        } else {
            (dailyBaseRate / 8.0) * getOvertimeFactor(worker)
        }
        val otPay = overtimeHours * hourlyRate

        var lateDeduction = 0.0
        if (worker.lateFine > 0.0 && checkInTime.isNotEmpty()) {
            if (isCheckInLate(checkInTime, worker.lateGracePeriodMinutes)) {
                lateDeduction = worker.lateFine
            }
        }

        val net = (rawBasePay + multiplierBonus + otPay - lateDeduction).coerceAtLeast(0.0)
        return Math.round(net * 100.0) / 100.0
    }

    fun calculateDailyWage(worker: Worker, record: AttendanceRecord): Double {
        return calculateDailyWage(
            worker = worker,
            status = record.status,
            customAmount = record.customAmount,
            date = record.date,
            overtimeHours = record.overtimeHours,
            checkInTime = record.checkInTime
        )
    }

    fun calculateDailyWage(worker: Worker, status: String): Double {
        return calculateDailyWage(worker, status, 0.0, "", 0.0, "")
    }

    fun calculateMonthlyTotal(worker: Worker, records: List<AttendanceRecord>): Double {
        return records.sumOf { calculateDailyWage(worker, it) }
    }

    /**
     * Calculates payroll summary for a worker given attendance records and period label.
     */
    fun calculateWageForRecords(worker: Worker, period: String, records: List<AttendanceRecord>): MonthlySummary {
        var present = 0
        var half = 0
        var absent = 0
        var ot = 0.0
        var netTotal = 0.0
        var grossBasePay = 0.0

        val dailyBaseRate = calculateDailyBaseRate(worker, period)

        records.forEach { rec ->
            when (rec.status.uppercase()) {
                "P", "PRESENT" -> present++
                "1/2", "HALF", "HALF-DAY", "H" -> half++
                "A", "ABSENT" -> absent++
            }
            ot += rec.overtimeHours
            val daily = calculateDailyWage(worker, rec)
            netTotal += daily

            val dayBase = if (rec.customAmount > 0.0) rec.customAmount else when (rec.status.uppercase()) {
                "P", "PRESENT" -> dailyBaseRate
                "1/2", "HALF", "HALF-DAY", "H" -> dailyBaseRate * worker.halfDayPayFactor
                else -> 0.0
            }
            grossBasePay += dayBase
        }

        return MonthlySummary(
            workerId = worker.id,
            workerName = worker.name,
            wageType = worker.wageType,
            baseWageRate = worker.wageRate,
            totalPresentDays = present,
            totalHalfDays = half,
            totalAbsentDays = absent,
            totalOvertimeHours = ot,
            grossBasePay = Math.round(grossBasePay * 100.0) / 100.0,
            netMonthlyWage = Math.round(netTotal * 100.0) / 100.0
        )
    }
}
