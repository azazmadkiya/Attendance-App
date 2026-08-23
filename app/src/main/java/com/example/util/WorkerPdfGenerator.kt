package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecord
import com.example.data.CashbookEntry
import com.example.data.Worker
import com.example.viewmodel.HaazriViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WorkerPdfGenerator {

    fun formatDisplayPeriod(periodTitle: String): String {
        val cleanPeriod = periodTitle.removePrefix("Month ").trim()
        val regexMonth = Regex("""^(\d{4})-(\d{2})$""")
        if (regexMonth.matches(cleanPeriod)) {
            return try {
                val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val d = parser.parse(cleanPeriod)
                if (d != null) formatter.format(d) else periodTitle
            } catch (e: Exception) {
                periodTitle
            }
        }
        if (cleanPeriod.contains(" to ")) {
            val parts = cleanPeriod.split(" to ")
            if (parts.size == 2) {
                try {
                    val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outSdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                    val d1 = inSdf.parse(parts[0].trim())
                    val d2 = inSdf.parse(parts[1].trim())
                    if (d1 != null && d2 != null) {
                        return "${outSdf.format(d1)} to ${outSdf.format(d2)}"
                    }
                } catch (e: Exception) {
                    // fallback
                }
            }
        }
        return periodTitle
    }

    fun buildWorkerHtmlReport(
        worker: Worker,
        attendanceHistory: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        viewModel: HaazriViewModel,
        reportPeriodTitle: String = "Full History"
    ): String {
        val displayPeriod = formatDisplayPeriod(reportPeriodTitle)
        val currentDate = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val totalPresent = attendanceHistory.count { it.status == "P" }
        val totalHalf = attendanceHistory.count { it.status == "1/2" }
        val totalAbsent = attendanceHistory.count { it.status == "A" }

        // Calculate earnings per attendance
        val attendanceEarnings = attendanceHistory.map { rec ->
            val dailyBreakdown = viewModel.getDailyWageBreakdown(worker, rec)
            val earned = dailyBreakdown.netDailyWage
            rec to earned
        }
        val totalEarned = attendanceEarnings.sumOf { it.second }
        val totalPaid = cashbookEntries.sumOf { it.amount }
        val netBalance = totalEarned - totalPaid

        val attendanceRows = attendanceEarnings.joinToString("") { (rec, earned) ->
            val statusColor = when (rec.status) {
                "P" -> "#16a34a"
                "A" -> "#dc2626"
                "1/2" -> "#d97706"
                else -> "#64748b"
            }
            val statusText = when (rec.status) {
                "P" -> "Present"
                "A" -> "Absent"
                "1/2" -> "Half Day"
                else -> "Off"
            }
            val noteBadge = if (rec.notes.isNotBlank()) {
                """<span style="color: #1e3a8a; font-weight: 600; font-size: 11px; background: #eff6ff; padding: 2px 6px; border-radius: 4px; border: 1px solid #bfdbfe; display: inline-block;">📝 ${rec.notes}</span>"""
            } else {
                """<span style="color: #94a3b8; font-size: 11px;">-</span>"""
            }
            val customAmtInfo = if (rec.customAmount > 0) {
                """<div style="font-size: 10px; color: #b45309; font-weight: bold; margin-top: 2px;">(Manual: ₹${rec.customAmount.toInt()})</div>"""
            } else ""
            val otInfo = if (rec.overtimeHours > 0) {
                """<span style="color: #d97706; font-size: 10px; font-weight: bold;"> • OT ${rec.overtimeHours}h</span>"""
            } else ""

            """
            <tr>
                <td><strong>${rec.date}</strong></td>
                <td>
                    <span style="color: $statusColor; font-weight: bold; background: ${statusColor}18; padding: 2px 6px; border-radius: 4px;">$statusText</span>
                    $customAmtInfo
                </td>
                <td>${rec.checkInTime.ifEmpty { "09:00 AM" }}$otInfo</td>
                <td>$noteBadge</td>
                <td>${if (rec.isGeofenceVerified) "📍 GPS Verified" else "Manual Entry"}</td>
                <td style="text-align: right; font-weight: bold; color: #16a34a;">+ ₹${String.format(Locale.US, "%.1f", earned)}</td>
            </tr>
            """.trimIndent()
        }

        val ledgerRows = cashbookEntries.joinToString("") { entry ->
            """
            <tr>
                <td>${entry.date}</td>
                <td><span style="color: #2563eb; font-weight: bold;">${entry.category}</span></td>
                <td>${entry.notes.ifEmpty { "Paid" }}</td>
                <td style="text-align: right; font-weight: bold; color: #dc2626;">- ₹${entry.amount.toInt()}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>${worker.name} - Account Statement</title>
            <style>
                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 20px; color: #1e293b; line-height: 1.4; }
                .header { border-bottom: 2px solid #1e3a8a; padding-bottom: 12px; margin-bottom: 20px; }
                .company-title { font-size: 22px; font-weight: bold; color: #1e3a8a; margin: 0; }
                .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
                .profile-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 20px; }
                .profile-title { font-size: 18px; font-weight: bold; color: #0f172a; margin-bottom: 4px; }
                .summary-grid { display: table; width: 100%; margin-bottom: 20px; border-spacing: 10px; }
                .summary-box { display: table-cell; width: 25%; background: #f1f5f9; padding: 12px; border-radius: 6px; text-align: center; }
                .summary-val { font-size: 16px; font-weight: bold; margin-top: 4px; }
                .section-title { font-size: 15px; font-weight: bold; color: #1e3a8a; border-bottom: 1px solid #cbd5e1; padding-bottom: 6px; margin-top: 24px; margin-bottom: 10px; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 12px; }
                th { background: #1e3a8a; color: white; text-align: left; padding: 8px; font-size: 11px; text-transform: uppercase; }
                td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
                tr:nth-child(even) { background: #f8fafc; }
                .footer { margin-top: 30px; font-size: 11px; text-align: center; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="company-title">ATTENDANCE APP</div>
                <div class="subtitle">Employee Account Statement & Attendance Ledger Report</div>
                <div class="subtitle" style="margin-top:2px; font-weight:bold; color:#1e3a8a;">Report Filter Period: $displayPeriod</div>
                <div class="subtitle" style="margin-top:2px;">Generated on: $currentDate</div>
            </div>

            <div class="profile-card">
                <div class="profile-title">${worker.name}</div>
                <div>Phone: ${worker.phone.ifEmpty { "N/A" }}</div>
                <div>Wage Plan: ${worker.wageType} Wage @ ₹${worker.wageRate.toInt()} / period</div>
            </div>

            <div class="summary-grid">
                <div class="summary-box" style="background:#e0e7ff;">
                    <div style="font-size:11px; color:#3730a3;">TOTAL DAYS WORKED</div>
                    <div class="summary-val" style="color:#1e3a8a;">${totalPresent} Present (${totalHalf} Half)</div>
                </div>
                <div class="summary-box" style="background:#dcfce7;">
                    <div style="font-size:11px; color:#166534;">GROSS EARNED WAGES</div>
                    <div class="summary-val" style="color:#15803d;">₹${totalEarned.toInt()}</div>
                </div>
                <div class="summary-box" style="background:#fef3c7;">
                    <div style="font-size:11px; color:#92400e;">PAID / ADVANCES</div>
                    <div class="summary-val" style="color:#b45309;">₹${totalPaid.toInt()}</div>
                </div>
                <div class="summary-box" style="background:${if (netBalance <= 0) "#dcfce7" else "#fee2e2"};">
                    <div style="font-size:11px; color:${if (netBalance <= 0) "#166534" else "#991b1b"};">NET BALANCE DUE</div>
                    <div class="summary-val" style="color:${if (netBalance <= 0) "#15803d" else "#b91c1c"};">₹${netBalance.toInt()}</div>
                </div>
            </div>

            <div class="section-title">Account Ledger Transactions (Payments & Advances)</div>
            ${if (cashbookEntries.isEmpty()) "<p style='font-size:12px; color:#64748b;'>No payments or advances recorded yet.</p>" else """
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Category</th>
                        <th>Notes</th>
                        <th style="text-align:right;">Amount Paid</th>
                    </tr>
                </thead>
                <tbody>
                    $ledgerRows
                </tbody>
            </table>
            """.trimIndent()}

            <div class="section-title">Attendance & Daily Earnings Breakdown (With Notes & Remarks)</div>
            ${if (attendanceHistory.isEmpty()) "<p style='font-size:12px; color:#64748b;'>No attendance records recorded yet.</p>" else """
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Check-in & Shift</th>
                        <th>Notes / Remarks</th>
                        <th>Mode</th>
                        <th style="text-align:right;">Earned Wage</th>
                    </tr>
                </thead>
                <tbody>
                    $attendanceRows
                </tbody>
            </table>
            """.trimIndent()}

            <div class="footer">
                Official statement auto-generated by Attendance App • Page 1 of 1
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun buildMonthlySummaryHtmlReport(
        monthYear: String,
        summaries: List<com.example.util.MonthlyWageSummary>,
        viewModel: HaazriViewModel
    ): String {
        val displayMonth = formatDisplayPeriod(monthYear)
        val currentDate = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val totalStaff = summaries.size
        val totalGross = summaries.sumOf { it.grossBasePay + it.grossOvertimePay }
        val totalNet = summaries.sumOf { it.netMonthlyWage }
        val totalPresentDays = summaries.sumOf { it.totalPresentDays }
        val totalHalfDays = summaries.sumOf { it.totalHalfDays }
        val totalAbsentDays = summaries.sumOf { it.totalAbsentDays }
        val totalOtHours = summaries.sumOf { it.totalOvertimeHours }

        val rows = summaries.mapIndexed { index, s ->
            """
            <tr>
                <td style="text-align: center; color: #64748b; font-weight: bold;">${index + 1}</td>
                <td>
                    <strong>${s.workerName}</strong>
                    <div style="font-size: 11px; color: #64748b;">${s.wageType} • Rate: ₹${s.baseWageRate.toInt()}</div>
                </td>
                <td style="text-align: center;">
                    <span style="color: #16a34a; font-weight: bold; background: #dcfce7; padding: 2px 6px; border-radius: 4px;">${s.totalPresentDays} P</span>
                </td>
                <td style="text-align: center;">
                    <span style="color: #d97706; font-weight: bold; background: #fef3c7; padding: 2px 6px; border-radius: 4px;">${s.totalHalfDays} Half</span>
                </td>
                <td style="text-align: center;">
                    <span style="color: #dc2626; font-weight: bold; background: #fee2e2; padding: 2px 6px; border-radius: 4px;">${s.totalAbsentDays} A</span>
                </td>
                <td style="text-align: center; color: #b45309; font-weight: bold;">${if (s.totalOvertimeHours > 0) "${s.totalOvertimeHours} hrs" else "-"}</td>
                <td style="text-align: right; color: #334155;">₹${s.grossBasePay.toInt()}</td>
                <td style="text-align: right; color: #d97706;">+ ₹${s.grossOvertimePay.toInt()}</td>
                <td style="text-align: right; font-weight: bold; color: #1e3a8a; font-size: 13px;">₹${s.netMonthlyWage.toInt()}</td>
                <td style="border-bottom: 1px dashed #cbd5e1; width: 80px;"></td>
            </tr>
            """.trimIndent()
        }.joinToString("")

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>All Employees Monthly Report - $displayMonth</title>
            <style>
                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 20px; color: #1e293b; line-height: 1.4; }
                .header { border-bottom: 3px solid #1e3a8a; padding-bottom: 12px; margin-bottom: 20px; }
                .company-title { font-size: 24px; font-weight: bold; color: #1e3a8a; margin: 0; }
                .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
                .summary-grid { display: table; width: 100%; margin-bottom: 20px; border-spacing: 10px; }
                .summary-box { display: table-cell; width: 25%; background: #f1f5f9; padding: 12px; border-radius: 8px; text-align: center; }
                .summary-title { font-size: 11px; font-weight: bold; text-transform: uppercase; }
                .summary-val { font-size: 18px; font-weight: bold; margin-top: 4px; }
                table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 12px; }
                th { background: #1e3a8a; color: white; text-align: left; padding: 10px 8px; font-size: 11px; text-transform: uppercase; }
                td { padding: 9px 8px; border-bottom: 1px solid #e2e8f0; }
                tr:nth-child(even) { background: #f8fafc; }
                .signature-section { margin-top: 40px; display: table; width: 100%; }
                .sig-box { display: table-cell; width: 50%; padding-top: 20px; text-align: center; }
                .sig-line { width: 180px; border-top: 1px solid #64748b; margin: 0 auto 6px auto; }
                .footer { margin-top: 30px; font-size: 11px; text-align: center; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="company-title">ATTENDANCE APP</div>
                <div class="subtitle" style="font-weight: bold; font-size: 15px; color: #1e293b;">ALL EMPLOYEES MASTER ATTENDANCE & PAYROLL REPORT</div>
                <div class="subtitle" style="margin-top: 4px; font-weight: bold; color: #1e3a8a;">Billing Period: $displayMonth</div>
                <div class="subtitle" style="margin-top: 2px;">Generated on: $currentDate</div>
            </div>

            <div class="summary-grid">
                <div class="summary-box" style="background:#e0e7ff;">
                    <div class="summary-title" style="color:#3730a3;">Total Staff</div>
                    <div class="summary-val" style="color:#1e3a8a;">$totalStaff Employees</div>
                </div>
                <div class="summary-box" style="background:#dcfce7;">
                    <div class="summary-title" style="color:#166534;">Total Attendance</div>
                    <div class="summary-val" style="color:#15803d; font-size: 15px;">${totalPresentDays}P / ${totalHalfDays}Half</div>
                </div>
                <div class="summary-box" style="background:#fef3c7;">
                    <div class="summary-title" style="color:#92400e;">Total OT Hours</div>
                    <div class="summary-val" style="color:#b45309;">${totalOtHours} Hours</div>
                </div>
                <div class="summary-box" style="background:#dbeafe;">
                    <div class="summary-title" style="color:#1e40af;">Net Payable Payroll</div>
                    <div class="summary-val" style="color:#1e3a8a;">₹${totalNet.toInt()}</div>
                </div>
            </div>

            <table>
                <thead>
                    <tr>
                        <th style="text-align:center; width:30px;">#</th>
                        <th>Employee Details</th>
                        <th style="text-align:center;">P</th>
                        <th style="text-align:center;">Half</th>
                        <th style="text-align:center;">A</th>
                        <th style="text-align:center;">OT</th>
                        <th style="text-align:right;">Base Pay</th>
                        <th style="text-align:right;">OT Pay</th>
                        <th style="text-align:right;">Net Payable</th>
                        <th style="text-align:center;">Sign / Ack</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>

            <div class="signature-section">
                <div class="sig-box">
                    <div class="sig-line"></div>
                    <div style="font-size: 11px; font-weight: bold; color: #475569;">Prepared By (Accountant / Supervisor)</div>
                </div>
                <div class="sig-box">
                    <div class="sig-line"></div>
                    <div style="font-size: 11px; font-weight: bold; color: #475569;">Authorized Signature / Manager</div>
                </div>
            </div>

            <div class="footer">
                Official Master Attendance & Salary Disbursement Sheet • Attendance App
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun generateMonthlyPayrollPdf(
        context: Context,
        monthYear: String,
        summaries: List<com.example.util.MonthlyWageSummary>,
        viewModel: HaazriViewModel
    ) {
        val displayMonth = formatDisplayPeriod(monthYear)
        val htmlContent = buildMonthlySummaryHtmlReport(monthYear, summaries, viewModel)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("All_Employees_Payroll_$displayMonth")
                val jobName = "All_Staff_Report_$displayMonth"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        Toast.makeText(context, "Generating All Employees Monthly Report for $displayMonth...", Toast.LENGTH_SHORT).show()
    }

    fun exportMonthlySummaryCsv(
        context: Context,
        monthYear: String,
        summaries: List<com.example.util.MonthlyWageSummary>
    ) {
        val displayMonth = formatDisplayPeriod(monthYear)
        val csvHeader = "Sr No,Worker Name,Wage Type,Base Rate,Present Days,Half Days,Absent Days,Overtime Hours,Gross Base Pay,Overtime Pay,Net Monthly Wage\n"
        val csvBody = summaries.mapIndexed { index, s ->
            "${index + 1},\"${s.workerName}\",${s.wageType},${s.baseWageRate.toInt()},${s.totalPresentDays},${s.totalHalfDays},${s.totalAbsentDays},${s.totalOvertimeHours},${s.grossBasePay.toInt()},${s.grossOvertimePay.toInt()},${s.netMonthlyWage.toInt()}"
        }.joinToString("\n")

        val fullCsv = csvHeader + csvBody

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "All Employees Monthly Attendance & Payroll Report - $displayMonth")
                putExtra(Intent.EXTRA_TEXT, fullCsv)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share All Staff Monthly Report ($displayMonth)"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndPrintPdf(
        context: Context,
        worker: Worker,
        attendanceHistory: List<AttendanceRecord>,
        cashbookEntries: List<CashbookEntry>,
        viewModel: HaazriViewModel,
        reportPeriodTitle: String = "Full History"
    ) {
        val htmlContent = buildWorkerHtmlReport(worker, attendanceHistory, cashbookEntries, viewModel, reportPeriodTitle)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("${worker.name}_Statement")
                val jobName = "${worker.name}_Report_$reportPeriodTitle"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        Toast.makeText(context, "Opening PDF Report Generator ($reportPeriodTitle) for ${worker.name}...", Toast.LENGTH_SHORT).show()
    }
}
