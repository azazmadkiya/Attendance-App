package com.attendance.app.azaz.util

import com.attendance.app.azaz.data.AttendanceRecord
import com.attendance.app.azaz.data.Worker
import com.attendance.app.azaz.viewmodel.MonthlySummary

val Double.netDailyWage: Double get() = this

object WageCalculator {
    fun calculateDailyWage(worker: Worker, status: String): Double {
        val baseWage = worker.wageRate
        return when (status.uppercase()) {
            "PRESENT", "P" -> baseWage
            "HALF", "HALF-DAY", "H", "1/2" -> baseWage * worker.halfDayPayFactor
            "OVERTIME", "OT" -> baseWage + worker.overtimeRate
            else -> 0.0
        }
    }

    fun calculateDailyBaseRate(worker: Worker): Double {
        return worker.wageRate
    }

    fun calculateMonthlyTotal(worker: Worker, records: List<AttendanceRecord>): Double {
        return records.sumOf { calculateDailyWage(worker, it.status) }
    }

    fun calculateWageForRecords(worker: Worker, period: String, records: List<AttendanceRecord>): MonthlySummary {
        val present = records.count { it.status.uppercase() in listOf("P", "PRESENT") }
        val half = records.count { it.status.uppercase() in listOf("H", "HALF", "HALF-DAY", "1/2") }
        val absent = records.count { it.status.uppercase() in listOf("A", "ABSENT") }
        val ot = records.sumOf { it.overtimeHours }
        val basePay = present * worker.wageRate + half * (worker.wageRate * worker.halfDayPayFactor)
        val otPay = ot * worker.overtimeRate
        val total = basePay + otPay
        return MonthlySummary(
            workerId = worker.id,
            workerName = worker.name,
            wageType = worker.wageType,
            baseWageRate = worker.wageRate,
            totalPresentDays = present,
            totalHalfDays = half,
            totalAbsentDays = absent,
            totalOvertimeHours = ot,
            grossBasePay = basePay,
            netMonthlyWage = total
        )
    }
}
