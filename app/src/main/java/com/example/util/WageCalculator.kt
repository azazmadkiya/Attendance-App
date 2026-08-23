package com.example.util

import com.example.data.AttendanceRecord
import com.example.data.Worker
import java.text.SimpleDateFormat
import java.util.Locale

data class DailyWageBreakdown(
    val date: String,
    val status: String,
    val basePay: Double,
    val hajariMultiplierPay: Double,
    val overtimePay: Double,
    val lateDeduction: Double,
    val netDailyWage: Double
)

data class MonthlyWageSummary(
    val workerId: Long,
    val workerName: String,
    val monthYear: String, // YYYY-MM
    val wageType: String,
    val baseWageRate: Double,
    val totalPresentDays: Int,
    val totalAbsentDays: Int,
    val totalHalfDays: Int,
    val totalOffDays: Int,
    val totalOvertimeHours: Double,
    val grossBasePay: Double,
    val grossOvertimePay: Double,
    val totalLateDeductions: Double,
    val netMonthlyWage: Double,
    val dailyBreakdowns: List<DailyWageBreakdown>
)

object WageCalculator {

    /**
     * Calculates the daily wage base rate based on worker wage configuration.
     */
    fun calculateDailyBaseRate(worker: Worker): Double {
        return when (worker.wageType) {
            "Daily" -> worker.wageRate
            "Weekly" -> worker.wageRate / 7.0
            "Monthly" -> worker.wageRate / 30.0
            else -> worker.wageRate
        }
    }

    /**
     * Parses Hajari multiplier into a numerical factor.
     */
    fun getHajariFactor(worker: Worker): Double {
        return when (worker.hajariMultiplier) {
            "2x" -> 2.0
            "3.5x" -> 3.5
            "4.75x" -> 4.75
            "Custom" -> 1.5
            else -> 1.0 // "Off"
        }
    }

    /**
     * Parses Overtime multiplier into a numerical factor.
     */
    fun getOvertimeFactor(worker: Worker): Double {
        val cleaned = worker.overtimeMultiplier.replace("x", "").trim()
        return cleaned.toDoubleOrNull() ?: 1.5
    }

    /**
     * Calculates single day wage breakdown for a worker and attendance record.
     */
    fun calculateDailyWage(worker: Worker, record: AttendanceRecord?): DailyWageBreakdown {
        if (record == null) {
            return DailyWageBreakdown(
                date = "",
                status = "Unmarked",
                basePay = 0.0,
                hajariMultiplierPay = 0.0,
                overtimePay = 0.0,
                lateDeduction = 0.0,
                netDailyWage = 0.0
            )
        }

        val dailyBaseRate = calculateDailyBaseRate(worker)
        val hajariFactor = getHajariFactor(worker)
        val overtimeFactor = getOvertimeFactor(worker)

        // Base Pay by Attendance Status or Manual Custom Amount
        val rawBasePay = if (record.customAmount > 0.0) {
            record.customAmount
        } else {
            val statusPayFactor = when (record.status) {
                "P" -> 1.0
                "1/2" -> worker.halfDayPayFactor
                "O" -> 0.0 // Paid or unpaid off based on policy, default 0
                else -> 0.0 // "A"
            }
            dailyBaseRate * statusPayFactor
        }
        val multiplierBonus = if (record.status == "P" && hajariFactor > 1.0) {
            rawBasePay * (hajariFactor - 1.0)
        } else 0.0

        // Overtime Pay calculation
        val hourlyRate = if (worker.overtimeRate > 0.0) {
            worker.overtimeRate
        } else {
            (dailyBaseRate / 8.0) * overtimeFactor
        }
        val totalOvertimePay = record.overtimeHours * hourlyRate

        // Late Fine Deduction calculation
        var lateDeduction = 0.0
        if (worker.lateFine > 0.0 && record.checkInTime.isNotEmpty()) {
            val isLate = isCheckInLate(record.checkInTime, worker.lateGracePeriodMinutes)
            if (isLate) {
                lateDeduction = worker.lateFine
            }
        }

        val netWage = (rawBasePay + multiplierBonus + totalOvertimePay - lateDeduction).coerceAtLeast(0.0)

        return DailyWageBreakdown(
            date = record.date,
            status = record.status,
            basePay = rawBasePay,
            hajariMultiplierPay = multiplierBonus,
            overtimePay = totalOvertimePay,
            lateDeduction = lateDeduction,
            netDailyWage = netWage
        )
    }

    /**
     * Calculates payroll summary for a worker given any list of attendance records and a custom period label.
     */
    fun calculateWageForRecords(
        worker: Worker,
        periodLabel: String,
        records: List<AttendanceRecord>
    ): MonthlyWageSummary {
        var presentCount = 0
        var absentCount = 0
        var halfCount = 0
        var offCount = 0
        var totalOTHours = 0.0

        val breakdowns = mutableListOf<DailyWageBreakdown>()

        records.forEach { rec ->
            when (rec.status) {
                "P" -> presentCount++
                "A" -> absentCount++
                "1/2" -> halfCount++
                "O" -> offCount++
            }
            totalOTHours += rec.overtimeHours
            breakdowns.add(calculateDailyWage(worker, rec))
        }

        val grossBasePay = breakdowns.sumOf { it.basePay + it.hajariMultiplierPay }
        val grossOTPay = breakdowns.sumOf { it.overtimePay }
        val totalLateDeductions = breakdowns.sumOf { it.lateDeduction }
        val netMonthlyWage = breakdowns.sumOf { it.netDailyWage }

        return MonthlyWageSummary(
            workerId = worker.id,
            workerName = worker.name,
            monthYear = periodLabel,
            wageType = worker.wageType,
            baseWageRate = worker.wageRate,
            totalPresentDays = presentCount,
            totalAbsentDays = absentCount,
            totalHalfDays = halfCount,
            totalOffDays = offCount,
            totalOvertimeHours = totalOTHours,
            grossBasePay = grossBasePay,
            grossOvertimePay = grossOTPay,
            totalLateDeductions = totalLateDeductions,
            netMonthlyWage = netMonthlyWage,
            dailyBreakdowns = breakdowns
        )
    }

    /**
     * Calculates monthly payroll summary for a worker given attendance records for that month.
     */
    fun calculateMonthlyWage(
        worker: Worker,
        monthYear: String, // YYYY-MM
        monthRecords: List<AttendanceRecord>
    ): MonthlyWageSummary {
        val filtered = monthRecords.filter { it.date.startsWith(monthYear) }
        return calculateWageForRecords(worker, monthYear, filtered)
    }

    private fun isCheckInLate(checkInTimeStr: String, gracePeriodMinutes: Int): Boolean {
        return try {
            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val checkInDate = format.parse(checkInTimeStr) ?: return false

            // Standard Shift Start at 09:00 AM
            val standardStart = format.parse("09:00 AM") ?: return false
            val diffMs = checkInDate.time - standardStart.time
            val diffMinutes = diffMs / (1000 * 60)

            diffMinutes > gracePeriodMinutes
        } catch (e: Exception) {
            false
        }
    }
}
