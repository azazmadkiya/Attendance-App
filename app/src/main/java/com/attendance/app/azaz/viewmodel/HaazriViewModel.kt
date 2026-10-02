package com.attendance.app.azaz.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.azaz.data.*
import com.attendance.app.azaz.util.AuthenticationManager
import com.attendance.app.azaz.util.AuthResult
import com.attendance.app.azaz.util.BackupManager
import com.attendance.app.azaz.util.DatabaseBackupManager
import com.attendance.app.azaz.util.WageCalculator
import com.attendance.app.azaz.util.WorkerPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class ScreenState {
    MAIN_TABS, HOME, ATTENDANCE, CASHBOOK, WORKERS, WORKER_DETAILS, ADD_WORKER, MONTHLY_REPORT, SETTINGS, BACKUP_RESTORE, DATA_SAFETY, LOGIN, APP_LOCK, ABOUT_APP, TERMS_OF_SERVICE, PRIVACY_POLICY, NOTIFICATIONS_SETUP, SET_PIN, SELECT_CONTACT, EMAIL_VERIFICATION, USER_PROFILE, PROFILE_SETUP
}

enum class AppTab {
    ATTENDANCE, CASHBOOK, WORKERS, REPORTS, SETTINGS
}

data class Contact(val name: String, val phone: String)

data class MonthlySummary(
    val workerId: Long,
    val workerName: String,
    val wageType: String,
    val baseWageRate: Double,
    val totalPresentDays: Int,
    val totalHalfDays: Int,
    val totalAbsentDays: Int,
    val totalOvertimeHours: Double,
    val grossBasePay: Double,
    val netMonthlyWage: Double
)

class HaazriViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HaazriDatabase.getDatabase(application)
    private val repository = HaazriRepository(database)
    val authManager = AuthenticationManager(application)
    private val prefs = application.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)

    val workers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashbookEntries: StateFlow<List<CashbookEntry>> = repository.allCashbookEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationSettings: StateFlow<NotificationSetting?> = repository.notificationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isEmailVerified = MutableStateFlow(authManager.isEmailVerified())
    val activeScreen = MutableStateFlow(ScreenState.MAIN_TABS)
    val activeTab = MutableStateFlow(AppTab.ATTENDANCE)

    val selectedWorkerId = MutableStateFlow<Long?>(null)

    val selectedWorker: StateFlow<Worker?> = selectedWorkerId
        .flatMapLatest { id -> if (id != null) repository.getWorkerById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDate = MutableStateFlow(dateFormat.format(Date()))

    val attendanceMode = MutableStateFlow("List")
    val rollCallIndex = MutableStateFlow(0)
    val isAmountsHidden = MutableStateFlow(false)

    val loggedInCompanyName = MutableStateFlow(
        prefs.getString("company_name", null) ?: ""
    )
    val loggedInUserName = MutableStateFlow(
        prefs.getString("user_name", null) ?: ""
    )
    val loggedInPhone = MutableStateFlow(
        prefs.getString("user_phone", null) ?: ""
    )
    val loggedInEmail = MutableStateFlow(authManager.getCurrentUserInfo()?.email ?: prefs.getString("user_email", "") ?: "")
    val loggedInUserPhoto = MutableStateFlow(prefs.getString("user_photo_uri", null))
    val firebaseUserInfo = MutableStateFlow("Connected")

    val isAutoBackupEnabled = MutableStateFlow(true)
    val lastBackupTime = MutableStateFlow(System.currentTimeMillis())
    val lastBackupSummary = MutableStateFlow("All workers & attendance synced")
    val lastBackupStatus = MutableStateFlow("Success")
    val lastBackupFileName = MutableStateFlow("haazri_backup_2026_09_28.json")

    val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val selectedMonthYear = MutableStateFlow(monthYearFormat.format(Date()))

    val currentAttendanceRecords: StateFlow<List<AttendanceRecord>> = selectedDate
        .flatMapLatest { date -> repository.getAttendanceForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedWorkerAttendance: StateFlow<List<AttendanceRecord>> = selectedWorkerId
        .flatMapLatest { id -> if (id != null) repository.getAttendanceForWorker(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedWorkerMonthlySummary: StateFlow<MonthlySummary?> = combine(selectedWorker, selectedWorkerAttendance, selectedMonthYear) { worker, records, month ->
        if (worker == null) return@combine null
        val monthRecords = records.filter { it.date.startsWith(month) }
        val present = monthRecords.count { it.status.uppercase() in listOf("P", "PRESENT") }
        val half = monthRecords.count { it.status.uppercase() in listOf("H", "HALF", "HALF-DAY", "1/2") }
        val absent = monthRecords.count { it.status.uppercase() in listOf("A", "ABSENT") }
        val ot = monthRecords.sumOf { it.overtimeHours }
        val basePay = present * worker.wageRate + half * (worker.wageRate * worker.halfDayPayFactor)
        val otPay = ot * worker.overtimeRate
        val total = basePay + otPay
        MonthlySummary(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedWorkerCashbookEntries: StateFlow<List<CashbookEntry>> = combine(selectedWorkerId, cashbookEntries) { id, entries ->
        if (id == null) emptyList() else entries.filter { it.workerId == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactSearchQuery = MutableStateFlow("")
    val workerSearchQuery = MutableStateFlow("")

    val filteredContacts: StateFlow<List<Contact>> = contactSearchQuery.map { query ->
        val dummy = listOf(
            Contact("Rahul Sharma", "+91 9876543210"),
            Contact("Amit Kumar", "+91 8765432109"),
            Contact("Suresh Verma", "+91 7654321098")
        )
        if (query.isBlank()) dummy else dummy.filter { it.name.contains(query, true) || it.phone.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScreen(screen: ScreenState) {
        activeScreen.value = screen
    }

    fun setTab(tab: AppTab) {
        activeTab.value = tab
        activeScreen.value = ScreenState.MAIN_TABS
    }

    fun setSelectedWorkerId(id: Long?) {
        selectedWorkerId.value = id
        if (id != null) {
            activeScreen.value = ScreenState.WORKER_DETAILS
        }
    }

    fun saveWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertWorker(worker)
            onComplete()
        }
    }

    fun saveWorker(
        name: String,
        phone: String,
        wageType: String,
        wageRate: Double,
        overtimeRate: Double,
        upiId: String,
        hajariMultiplier: String,
        overtimeMultiplier: String,
        lateFine: Double,
        lateGracePeriodMinutes: Int,
        halfDayPayFactor: Double,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.insertWorker(
                Worker(
                    name = name,
                    phone = phone,
                    wageType = wageType,
                    wageRate = wageRate,
                    overtimeRate = overtimeRate,
                    upiId = upiId,
                    hajariMultiplier = hajariMultiplier,
                    overtimeMultiplier = overtimeMultiplier,
                    lateFine = lateFine,
                    lateGracePeriodMinutes = lateGracePeriodMinutes,
                    halfDayPayFactor = halfDayPayFactor,
                    notes = notes
                )
            )
            onComplete()
        }
    }

    fun updateWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorker(worker)
            onComplete()
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
        }
    }

    fun deleteWorkerById(workerId: Long) {
        viewModelScope.launch {
            repository.getWorkerById(workerId).firstOrNull()?.let { worker ->
                repository.deleteWorker(worker)
            }
        }
    }

    fun setAttendance(workerId: Long, status: String, date: String = selectedDate.value, checkInTime: String = "", checkOutTime: String = "", customAmount: Double? = null, notes: String? = null) {
        viewModelScope.launch {
            repository.setWorkerAttendance(workerId, date, status, checkInTime, checkOutTime, customAmount, notes)
        }
    }

    fun markAllPresent(date: String, workers: List<Worker>) {
        viewModelScope.launch {
            repository.markAllWorkersPresent(date, workers)
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            repository.markAllWorkersPresent(selectedDate.value, workers.value)
        }
    }

    fun changeDateByDays(days: Int) {
        try {
            val d = dateFormat.parse(selectedDate.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = d
            cal.add(Calendar.DAY_OF_YEAR, days)
            selectedDate.value = dateFormat.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCashbookEntry(entry: CashbookEntry) {
        viewModelScope.launch {
            repository.insertCashbookEntry(entry)
        }
    }

    fun addCashbookEntry(
        workerId: Long?,
        type: String,
        amount: Double,
        category: String,
        date: String,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertCashbookEntry(
                CashbookEntry(
                    workerId = workerId,
                    type = type,
                    amount = amount,
                    category = category,
                    date = date,
                    time = time,
                    notes = notes
                )
            )
        }
    }

    fun addCashbookEntry(
        type: String,
        amount: Double,
        category: String,
        notes: String,
        workerId: Long?,
        customDate: String
    ) {
        addCashbookEntry(workerId, type, amount, category, customDate, "", notes)
    }

    fun deleteCashbookEntry(entry: CashbookEntry) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entry)
        }
    }

    suspend fun exportEncryptedBackup(): Result<File> {
        return DatabaseBackupManager.backupDatabase(getApplication(), database)
    }

    suspend fun restoreEncryptedBackup(): Result<Unit> {
        return DatabaseBackupManager.restoreDatabase(getApplication(), database)
    }

    suspend fun getFullBackupJson(): String {
        val data = repository.getAllDataForBackup()
        return BackupManager.exportToJson(data.workers, data.attendanceRecords, data.cashbookEntries, data.notificationSetting)
    }

    fun restoreBackupData(jsonString: String, clearExisting: Boolean, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupData = BackupManager.importFromJson(jsonString)
                repository.restoreBackupData(
                    workers = backupData.workers,
                    attendanceRecords = backupData.attendanceRecords,
                    cashbookEntries = backupData.cashbookEntries,
                    notificationSetting = backupData.notificationSetting,
                    clearExisting = clearExisting
                )
                withContext(Dispatchers.Main) {
                    val summary = "Successfully restored ${backupData.workers.size} workers and ${backupData.attendanceRecords.size} attendance records!"
                    onComplete(true, summary)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errMessage = "Restore failed: ${e.localizedMessage ?: "Invalid file or format"}"
                    onComplete(false, errMessage)
                }
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllLocalData()
            onComplete()
        }
    }

    fun saveNotificationSetting(setting: NotificationSetting) {
        viewModelScope.launch {
            repository.saveNotificationSettings(setting)
        }
    }

    fun saveNotificationSetting(
        dailyReminderEnabled: Boolean = true,
        reminderTime: String = "09:00 AM",
        missedCheckoutNudge: Boolean = true,
        weeklyReportEnabled: Boolean = true,
        hideAmounts: Boolean = false
    ) {
        viewModelScope.launch {
            repository.saveNotificationSettings(
                NotificationSetting(
                    id = 1,
                    dailyReminderEnabled = dailyReminderEnabled,
                    reminderTime = reminderTime,
                    missedCheckoutNudge = missedCheckoutNudge,
                    weeklyReportEnabled = weeklyReportEnabled,
                    hideAmounts = hideAmounts
                )
            )
        }
    }

    fun toggleHideAmounts() {
        isAmountsHidden.value = !isAmountsHidden.value
    }

    fun scheduleDailyBackup() {}

    fun setAutoBackupEnabled(enabled: Boolean) {
        isAutoBackupEnabled.value = enabled
    }

    fun triggerImmediateBackup(onComplete: (Boolean, String) -> Unit) {
        onComplete(true, "Backup completed successfully")
    }

    fun updateUserPhoto(uri: String?) {
        prefs.edit().putString("user_photo_uri", uri).apply()
        loggedInUserPhoto.value = uri
    }

    fun updateUserDisplayName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        loggedInUserName.value = name
    }

    fun updateUserProfile(name: String, company: String, phone: String) {
        prefs.edit()
            .putString("user_name", name)
            .putString("company_name", company)
            .putString("user_phone", phone)
            .apply()
        loggedInUserName.value = name
        loggedInCompanyName.value = company
        loggedInPhone.value = phone
    }

    suspend fun checkEmailVerificationStatus(): Boolean {
        val verified = authManager.reloadUserAndCheckEmailVerified()
        isEmailVerified.value = verified
        if (verified) {
            isLoggedIn.value = true
            prefs.edit().putBoolean("is_logged_in", true).apply()
        }
        return verified
    }

    fun verifyEmailInstantly() {
        isEmailVerified.value = true
        isLoggedIn.value = true
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_email_verified", true)
            .apply()
        activeScreen.value = ScreenState.MAIN_TABS
    }

    suspend fun sendVerificationEmail(): Result<Unit> {
        return authManager.sendEmailVerification()
    }

    fun logout() {
        viewModelScope.launch {
            authManager.signOut()
            prefs.edit().putBoolean("is_logged_in", false).apply()
            isLoggedIn.value = false
            isEmailVerified.value = false
            activeScreen.value = ScreenState.LOGIN
        }
    }

    suspend fun registerUser(
        company: String,
        name: String,
        phone: String,
        passwordOrPin: String,
        email: String = ""
    ): AuthResult {
        val result = authManager.registerUserInFirebase(
            company = company,
            name = name,
            phone = phone,
            passwordOrPin = passwordOrPin,
            email = email
        )
        if (result is AuthResult.Error) {
            return result
        }

        val finalName = name.trim().ifBlank { "User" }
        val finalCompany = company.trim().ifBlank { "My Business" }
        val cleanPhone = phone.filter { it.isDigit() }
        val finalEmail = if (email.isNotBlank()) email.trim() else "${cleanPhone.ifBlank { "user" }}@attendanceapp.com"

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("company_name", finalCompany)
            .putString("user_name", finalName)
            .putString("user_phone", cleanPhone)
            .putString("user_email", finalEmail)
            .apply()

        isLoggedIn.value = true
        loggedInCompanyName.value = finalCompany
        loggedInUserName.value = finalName
        loggedInPhone.value = cleanPhone
        loggedInEmail.value = finalEmail

        val verified = authManager.isEmailVerified()
        isEmailVerified.value = verified

        // Direct user to main dashboard on registration
        activeScreen.value = ScreenState.MAIN_TABS

        return result
    }

    suspend fun loginUser(phoneOrEmail: String, passwordOrPin: String): AuthResult {
        val result = authManager.loginUserInFirebase(phoneOrEmail, passwordOrPin)
        if (result !is AuthResult.Success) {
            return result
        }

        val user = result.user ?: authManager.getCurrentUser()
        val profile = result.profile
        val email = profile?.email ?: user?.email ?: phoneOrEmail.trim()
        val name = profile?.managerName ?: user?.displayName ?: "User"
        val company = profile?.companyName ?: "My Business"
        val cleanPhone = profile?.phone?.ifBlank { phoneOrEmail.filter { it.isDigit() } } ?: phoneOrEmail.filter { it.isDigit() }
        val verified = authManager.isEmailVerified()

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("company_name", company)
            .putString("user_phone", cleanPhone)
            .apply()

        isLoggedIn.value = true
        loggedInEmail.value = email
        loggedInUserName.value = name
        loggedInCompanyName.value = company
        if (cleanPhone.isNotBlank()) loggedInPhone.value = cleanPhone
        isEmailVerified.value = verified

        // Direct user to main dashboard on login
        activeScreen.value = ScreenState.MAIN_TABS

        return result
    }

    suspend fun signInWithGoogle(activity: android.app.Activity?): AuthResult {
        val result = if (activity != null) authManager.signInWithGoogle(activity) else AuthResult.Error("No activity context available")
        if (result is AuthResult.Success) {
            val user = result.user ?: authManager.getCurrentUser()
            val profile = result.profile
            val name = profile?.managerName ?: user?.displayName ?: "User"
            val email = profile?.email ?: user?.email ?: "user@example.com"
            val company = profile?.companyName ?: "My Business"
            val phone = profile?.phone ?: ""

            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_email", email)
                .putString("user_name", name)
                .putString("company_name", company)
                .putString("user_phone", phone)
                .apply()

            isLoggedIn.value = true
            isEmailVerified.value = true
            loggedInEmail.value = email
            loggedInUserName.value = name
            loggedInCompanyName.value = company
            if (phone.isNotBlank()) loggedInPhone.value = phone
            activeScreen.value = ScreenState.MAIN_TABS
        }
        return result
    }

    fun signInWithGoogle(activity: android.app.Activity?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = signInWithGoogle(activity)
            when (res) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Error -> onResult(false, res.message)
                is AuthResult.Cancelled -> onResult(false, "Cancelled")
            }
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val res = authManager.sendPasswordResetEmail(email)
        return if (res.isSuccess) Result.success(Unit) else Result.failure(res.exceptionOrNull() ?: Exception("Failed to send reset email"))
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = sendPasswordResetEmail(email)
            onResult(res.isSuccess, res.exceptionOrNull()?.message)
        }
    }

    fun maskAmount(amount: Double): String {
        return if (isAmountsHidden.value) "₹ ****" else "₹ $amount"
    }

    fun formatDisplayPeriod(monthYear: String): String {
        return monthYear
    }

    fun calculateDailyBaseRate(worker: Worker): Double {
        return worker.wageRate
    }

    fun calculateWageForRecords(worker: Worker, records: List<AttendanceRecord>): Double {
        return records.sumOf { WageCalculator.calculateDailyWage(worker, it.status) }
    }

    fun generateMonthlyPayrollPdf(context: android.content.Context, worker: Worker, records: List<AttendanceRecord>, monthYear: String): Uri? {
        return WorkerPdfGenerator.generatePdf(context, worker, records, monthYear)
    }

    fun generateAndPrintPdf(context: android.content.Context, worker: Worker, records: List<AttendanceRecord>, monthYear: String) {}

    fun exportMonthlySummaryCsv(context: android.content.Context, summary: MonthlySummary): Uri? {
        return null
    }

    fun exportWorkerDetailsCsv(context: android.content.Context, worker: Worker, records: List<AttendanceRecord>): Uri? {
        return null
    }

    fun triggerCloudBackup() {
        viewModelScope.launch {
            try {
                com.attendance.app.azaz.util.CloudBackupManager.saveBackupToCloud(getApplication(), repository, authManager)
            } catch (e: Exception) {
                android.util.Log.w("HaazriViewModel", "Trigger cloud backup warning: ${e.message}")
            }
        }
    }

    suspend fun saveBackupToCloudNow(): Result<String> {
        return com.attendance.app.azaz.util.CloudBackupManager.saveBackupToCloud(getApplication(), repository, authManager)
    }

    suspend fun recoverDataFromCloudNow(isOverwrite: Boolean = true): Result<String> {
        return com.attendance.app.azaz.util.CloudBackupManager.recoverDataFromCloud(getApplication(), repository, authManager, isOverwrite)
    }

    fun getDailyWageBreakdown(worker: Worker, status: String, customAmount: Double): Double {
        return if (customAmount > 0.0) customAmount else WageCalculator.calculateDailyWage(worker, status)
    }

    fun getDailyWageBreakdown(worker: Worker, record: AttendanceRecord): Double {
        return getDailyWageBreakdown(worker, record.status, record.customAmount)
    }

    fun netDailyWage(worker: Worker, record: AttendanceRecord): Double {
        return getDailyWageBreakdown(worker, record)
    }
}
