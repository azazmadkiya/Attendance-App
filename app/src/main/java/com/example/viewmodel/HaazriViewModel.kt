package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.AuthenticationManager
import com.example.util.AuthResult
import com.example.util.AuthUserInfo
import com.example.util.UserProfile
import com.example.util.BackupManager
import com.example.util.DailyWageBreakdown
import com.example.util.MonthlyWageSummary
import com.example.util.NotificationHelper
import com.example.util.NotificationScheduler
import com.example.util.WageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

enum class AppTab {
    WORKERS, ATTENDANCE, CASHBOOK, SETTINGS
}

enum class ScreenState {
    MAIN_TABS, ADD_WORKER, SELECT_CONTACT, WORKER_DETAILS, NOTIFICATIONS_SETUP, MONTHLY_REPORT, BACKUP_RESTORE, PRIVACY_POLICY, TERMS_OF_SERVICE, DATA_SAFETY, ABOUT_APP
}

@OptIn(ExperimentalCoroutinesApi::class)
class HaazriViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HaazriRepository
    private val prefs = application.getSharedPreferences("haazri_user_prefs", android.content.Context.MODE_PRIVATE)

    val authManager = AuthenticationManager(application)
    val firebaseUserInfo: StateFlow<AuthUserInfo?> = authManager.authUserInfo

    // Auth state
    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false) || authManager.isUserSignedIn())
    val loggedInUserName = MutableStateFlow(
        authManager.getCurrentUserInfo()?.displayName
            ?: (prefs.getString("user_name", "Azaz Madkiya") ?: "Azaz Madkiya")
    )
    val loggedInCompanyName = MutableStateFlow(prefs.getString("company_name", "Madkiya Construction") ?: "Madkiya Construction")
    val loggedInPhone = MutableStateFlow(prefs.getString("user_phone", "9876543210") ?: "9876543210")
    val loggedInEmail = MutableStateFlow(authManager.getCurrentUserInfo()?.email ?: prefs.getString("user_email", "") ?: "")

    // App Lock preferences
    val isAppLockEnabled = MutableStateFlow(prefs.getBoolean("is_app_lock_enabled", false))
    val appLockPin = MutableStateFlow(prefs.getString("app_lock_pin", "1234") ?: "1234")
    val isAmountsHidden = MutableStateFlow(prefs.getBoolean("is_amounts_hidden", false))

    val activeTab = MutableStateFlow(AppTab.WORKERS)
    val activeScreen = MutableStateFlow(ScreenState.MAIN_TABS)

    val workerSearchQuery = MutableStateFlow("")
    val contactSearchQuery = MutableStateFlow("")

    val selectedWorkerId = MutableStateFlow<Long?>(null)
    val rollCallIndex = MutableStateFlow(0)
    val attendanceMode = MutableStateFlow("List") // "List" or "Roll-call"

    // Date state
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDate = MutableStateFlow(dateFormatter.format(Date()))

    init {
        val db = HaazriDatabase.getDatabase(application)
        repository = HaazriRepository(db)

        viewModelScope.launch {
            try {
                val hasClearedSample = prefs.getBoolean("has_cleared_initial_sample_data", false)
                if (!hasClearedSample) {
                    prefs.edit().putBoolean("has_cleared_initial_sample_data", true).apply()
                }

                // Ensure default notification settings
                val currentSettings = repository.notificationSettings.firstOrNull()
                val settingsToSchedule = if (currentSettings == null) {
                    val defaultSetting = NotificationSetting(
                        dailyReminderEnabled = true,
                        reminderTime = "09:00 AM",
                        missedCheckoutNudge = true,
                        weeklyReportEnabled = true
                    )
                    repository.saveNotificationSettings(defaultSetting)
                    defaultSetting
                } else {
                    currentSettings
                }
                NotificationScheduler.scheduleAll(application, settingsToSchedule)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Observed flows
    val workers: StateFlow<List<Worker>> = workerSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allWorkers else repository.searchWorkers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAttendanceRecords: StateFlow<List<AttendanceRecord>> = selectedDate
        .flatMapLatest { date -> repository.getAttendanceForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashbookEntries: StateFlow<List<CashbookEntry>> = repository.allCashbookEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationSettings: StateFlow<NotificationSetting?> = repository.notificationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedWorker: StateFlow<Worker?> = selectedWorkerId
        .flatMapLatest { id -> if (id != null) repository.getWorkerById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedWorkerAttendance: StateFlow<List<AttendanceRecord>> = selectedWorkerId
        .flatMapLatest { id -> if (id != null) repository.getAttendanceForWorker(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedWorkerCashbookEntries: StateFlow<List<CashbookEntry>> = combine(
        selectedWorkerId,
        cashbookEntries
    ) { id, entries ->
        if (id == null) emptyList()
        else entries.filter { it.workerId == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month string "YYYY-MM"
    val selectedMonthYear: StateFlow<String> = selectedDate.map { dateStr ->
        if (dateStr.length >= 7) dateStr.substring(0, 7) else "2026-08"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "2026-08")

    // Wage calculation for selected worker
    val selectedWorkerMonthlySummary: StateFlow<MonthlyWageSummary?> = combine(
        selectedWorker,
        selectedWorkerAttendance,
        selectedMonthYear
    ) { worker, records, monthYear ->
        if (worker == null) null
        else WageCalculator.calculateMonthlyWage(worker, monthYear, records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All workers monthly payroll summaries
    val allWorkersMonthlySummaries: StateFlow<List<MonthlyWageSummary>> = combine(
        workers,
        allAttendanceRecords,
        selectedMonthYear
    ) { workerList, allRecords, monthYear ->
        val recordsByWorker = allRecords.groupBy { it.workerId }
        workerList.map { worker ->
            val records = recordsByWorker[worker.id] ?: emptyList()
            WageCalculator.calculateMonthlyWage(worker, monthYear, records)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper functions for wage calculations
    fun getDailyWageBreakdown(worker: Worker, record: AttendanceRecord?): DailyWageBreakdown {
        return WageCalculator.calculateDailyWage(worker, record)
    }

    fun getMonthlyWageSummary(worker: Worker, monthYear: String, records: List<AttendanceRecord>): MonthlyWageSummary {
        return WageCalculator.calculateMonthlyWage(worker, monthYear, records)
    }

    // Mock system contacts for "Add from contacts"
    val sampleContacts = listOf(
        Contact("1. Kashmiri Maulana AL-IQRAM INDIA. Umrah Ziyarat", "91065385210"),
        Contact("2. Kashmiri Maulana AL-IQRAM Moulana Zahid Hushen INDIA. Umrah Ziyarat", "997979311800"),
        Contact("3. Saudi Kashmiri Maulana AL-IQRAM SAUDI Umrah Ziyarat", "+9660560978323000"),
        Contact("30 Mt Makvanabhai", "6354200700"),
        Contact("7947 Kanabhai", "7202826656"),
        Contact("814", "9898012345"),
        Contact("Ramesh Kumar", "9825012345"),
        Contact("Suresh Patel", "9426098765")
    )

    val filteredContacts = contactSearchQuery.map { query ->
        if (query.isBlank()) sampleContacts else sampleContacts.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sampleContacts)

    // Attendance Actions
    fun setAttendance(
        workerId: Long,
        status: String,
        checkInTime: String = "09:00 AM",
        customAmount: Double? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.setWorkerAttendance(
                workerId = workerId,
                date = selectedDate.value,
                status = status,
                checkInTime = checkInTime,
                customAmount = customAmount,
                notes = notes
            )
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            repository.markAllWorkersPresent(selectedDate.value, workers.value)
        }
    }

    fun changeDateByDays(days: Int) {
        try {
            val cal = Calendar.getInstance()
            cal.time = dateFormatter.parse(selectedDate.value) ?: Date()
            cal.add(Calendar.DAY_OF_YEAR, days)
            selectedDate.value = dateFormatter.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Add Worker
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
        notes: String
    ) {
        viewModelScope.launch {
            val worker = Worker(
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
            repository.insertWorker(worker)
            activeScreen.value = ScreenState.MAIN_TABS
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch {
            repository.updateWorker(worker)
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
            if (selectedWorkerId.value == worker.id) {
                selectedWorkerId.value = null
            }
            activeScreen.value = ScreenState.MAIN_TABS
        }
    }

    fun deleteAllWorkers() {
        viewModelScope.launch {
            repository.deleteAllWorkers()
            selectedWorkerId.value = null
        }
    }

    // Cashbook Entry
    fun addCashbookEntry(type: String, amount: Double, category: String, notes: String, workerId: Long? = null, customDate: String? = null) {
        viewModelScope.launch {
            val targetDate = customDate ?: selectedDate.value
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val entry = CashbookEntry(
                workerId = workerId,
                type = type,
                amount = amount,
                category = category,
                date = targetDate,
                time = time,
                notes = notes
            )
            repository.insertCashbookEntry(entry)
        }
    }

    fun deleteCashbookEntry(entry: CashbookEntry) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entry)
        }
    }

    fun saveNotificationSetting(dailyReminder: Boolean, reminderTime: String, missedCheckout: Boolean, weeklyReport: Boolean, hideAmounts: Boolean) {
        viewModelScope.launch {
            val setting = NotificationSetting(
                dailyReminderEnabled = dailyReminder,
                reminderTime = reminderTime,
                missedCheckoutNudge = missedCheckout,
                weeklyReportEnabled = weeklyReport,
                hideAmounts = hideAmounts
            )
            repository.saveNotificationSettings(setting)
            NotificationScheduler.scheduleAll(getApplication(), setting)
        }
    }

    // User Authentication with Firebase Cloud Sync & Firestore Recovery
    suspend fun loginUser(phoneOrEmail: String, passwordOrPin: String): AuthResult {
        // 1. Attempt Cloud Authentication with Firebase
        val result = authManager.loginUserInFirebase(phoneOrEmail, passwordOrPin)
        if (result is AuthResult.Success) {
            val profile = result.profile
            val cleanPhone = phoneOrEmail.filter { it.isDigit() }
            val finalPhone = if (profile != null && profile.phone.isNotBlank()) profile.phone else cleanPhone
            val finalName = profile?.managerName?.ifBlank { result.user.displayName ?: "Admin" }
                ?: result.user.displayName ?: "Admin"
            val finalCompany = profile?.companyName?.ifBlank { "My Business" } ?: "My Business"
            val finalEmail = profile?.email?.ifBlank { result.user.email ?: "" } ?: (result.user.email ?: "")

            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("company_name", finalCompany)
                .putString("user_name", finalName)
                .putString("user_phone", finalPhone)
                .putString("user_email", finalEmail)
                .putString("user_pin_$finalPhone", passwordOrPin)
                .putString("user_pass_$finalPhone", passwordOrPin)
                .apply()

            isLoggedIn.value = true
            loggedInCompanyName.value = finalCompany
            loggedInUserName.value = finalName
            loggedInPhone.value = finalPhone
            loggedInEmail.value = finalEmail
            return result
        }

        // 2. Offline Fallback for previously cached credentials or demo user
        val cleanPhone = phoneOrEmail.filter { it.isDigit() }
        val savedPass = prefs.getString("user_pin_$cleanPhone", null)
            ?: prefs.getString("user_pass_$cleanPhone", null)
            ?: if (cleanPhone == "9876543210") "1234" else null

        if (savedPass != null && (passwordOrPin == savedPass || (cleanPhone == "9876543210" && passwordOrPin == "1234"))) {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_phone", cleanPhone)
                .apply()
            isLoggedIn.value = true
            loggedInPhone.value = cleanPhone
            val restoredName = prefs.getString("user_name", "Admin") ?: "Admin"
            val restoredCompany = prefs.getString("company_name", "My Business") ?: "My Business"
            loggedInUserName.value = restoredName
            loggedInCompanyName.value = restoredCompany
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                return AuthResult.Success(currentUser)
            }
        }

        return result
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
        if (result is AuthResult.Success) {
            val user = result.user
            val profile = result.profile
            val finalName = profile?.managerName?.ifBlank { name } ?: name
            val finalCompany = profile?.companyName?.ifBlank { company } ?: company
            val finalPhone = profile?.phone?.ifBlank { phone } ?: phone
            val finalEmail = profile?.email?.ifBlank { email } ?: email

            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("company_name", finalCompany)
                .putString("user_name", finalName)
                .putString("user_phone", finalPhone)
                .putString("user_email", finalEmail)
                .putString("user_pin_$finalPhone", passwordOrPin)
                .putString("user_pass_$finalPhone", passwordOrPin)
                .apply()

            isLoggedIn.value = true
            loggedInCompanyName.value = finalCompany
            loggedInUserName.value = finalName
            loggedInPhone.value = finalPhone
            loggedInEmail.value = finalEmail
        }
        return result
    }

    fun updateUserProfile(name: String, company: String, phone: String, email: String = "") {
        prefs.edit()
            .putString("user_name", name)
            .putString("company_name", company)
            .putString("user_phone", phone)
            .putString("user_email", email)
            .apply()
        loggedInUserName.value = name
        loggedInCompanyName.value = company
        loggedInPhone.value = phone
        loggedInEmail.value = email

        viewModelScope.launch {
            val user = authManager.getCurrentUser()
            if (user != null) {
                authManager.saveProfileToFirestore(
                    UserProfile(
                        uid = user.uid,
                        companyName = company,
                        managerName = name,
                        phone = phone,
                        email = email.ifBlank { user.email ?: "" }
                    ),
                    authEmail = user.email ?: "${phone}@${AuthenticationManager.EMAIL_DOMAIN}"
                )
            }
        }
    }

    fun updateAppLock(enabled: Boolean, pin: String) {
        prefs.edit()
            .putBoolean("is_app_lock_enabled", enabled)
            .putString("app_lock_pin", pin)
            .apply()
        isAppLockEnabled.value = enabled
        appLockPin.value = pin
    }

    fun toggleHideAmounts() {
        val newValue = !isAmountsHidden.value
        prefs.edit().putBoolean("is_amounts_hidden", newValue).apply()
        isAmountsHidden.value = newValue
        viewModelScope.launch {
            val s = repository.notificationSettings.firstOrNull()
            if (s != null) {
                repository.saveNotificationSettings(s.copy(hideAmounts = newValue))
            } else {
                repository.saveNotificationSettings(
                    NotificationSetting(dailyReminderEnabled = true, reminderTime = "09:00 AM", missedCheckoutNudge = true, hideAmounts = newValue)
                )
            }
        }
    }

    fun maskAmount(amount: Double, prefix: String = "₹"): String {
        if (isAmountsHidden.value) return "$prefix••••"
        return if (amount % 1.0 == 0.0) {
            "$prefix${amount.toInt()}"
        } else {
            "$prefix${String.format(Locale.US, "%.1f", amount)}"
        }
    }

    fun maskAmountVal(amountVal: Any, prefix: String = "₹", suffix: String = ""): String {
        if (isAmountsHidden.value) return "$prefix••••$suffix"
        return "$prefix$amountVal$suffix"
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authManager.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .apply()
        isLoggedIn.value = false
    }

    suspend fun firebaseSignUpWithEmail(email: String, password: String, displayName: String, companyName: String): AuthResult {
        val result = authManager.signUpWithEmail(email, password, displayName)
        if (result is AuthResult.Success) {
            val user = result.user
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_name", displayName.ifBlank { user.displayName ?: "User" })
                .putString("company_name", companyName.ifBlank { "My Business" })
                .putString("user_email", email)
                .apply()
            isLoggedIn.value = true
            loggedInUserName.value = displayName.ifBlank { user.displayName ?: "User" }
            loggedInCompanyName.value = companyName.ifBlank { "My Business" }
            loggedInEmail.value = email
        }
        return result
    }

    suspend fun firebaseSignInWithEmail(email: String, password: String): AuthResult {
        val result = authManager.signInWithEmail(email, password)
        if (result is AuthResult.Success) {
            val user = result.user
            val name = user.displayName ?: user.email?.substringBefore("@") ?: "User"
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_name", name)
                .putString("user_email", email)
                .apply()
            isLoggedIn.value = true
            loggedInUserName.value = name
            loggedInEmail.value = email
        }
        return result
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return authManager.sendPasswordResetEmail(email)
    }

    suspend fun signInWithGoogle(activity: Activity? = null): AuthResult = firebaseSignInWithGoogle(activity)

    suspend fun firebaseSignInWithGoogle(activity: Activity? = null): AuthResult {
        val result = authManager.signInWithGoogle(activity)
        if (result is AuthResult.Success) {
            val user = result.user
            val profile = result.profile
            val name = profile?.managerName?.ifBlank { user.displayName ?: "User" }
                ?: user.displayName ?: "User"
            val company = profile?.companyName?.ifBlank { "My Business" } ?: "My Business"
            val email = user.email ?: profile?.email ?: ""
            val phone = profile?.phone ?: ""

            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_name", name)
                .putString("company_name", company)
                .putString("user_email", email)
                .putString("user_phone", phone)
                .apply()
            isLoggedIn.value = true
            loggedInUserName.value = name
            loggedInCompanyName.value = company
            loggedInEmail.value = email
            if (phone.isNotBlank()) loggedInPhone.value = phone
        }
        return result
    }

    // Backup & Restore
    suspend fun getFullBackupJson(): String {
        val data = repository.getAllDataForBackup()
        return BackupManager.exportToJson(
            workers = data.workers,
            attendanceRecords = data.attendanceRecords,
            cashbookEntries = data.cashbookEntries,
            notificationSetting = data.notificationSetting
        )
    }

    suspend fun exportEncryptedBackup(): Result<java.io.File> {
        val db = HaazriDatabase.getDatabase(getApplication())
        return com.example.util.DatabaseBackupManager.backupDatabase(getApplication(), db)
    }

    suspend fun restoreEncryptedBackup(): Result<Unit> {
        val db = HaazriDatabase.getDatabase(getApplication())
        return com.example.util.DatabaseBackupManager.restoreDatabase(getApplication(), db)
    }

    fun exportBackupJson(): String {
        return BackupManager.exportToJson(
            workers = workers.value,
            attendanceRecords = allAttendanceRecords.value,
            cashbookEntries = cashbookEntries.value,
            notificationSetting = notificationSettings.value
        )
    }

    fun exportBackupJsonAsync(onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = getFullBackupJson()
            withContext(Dispatchers.Main) {
                onReady(json)
            }
        }
    }

    fun restoreBackupData(
        jsonString: String,
        clearExisting: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
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
                    onResult(
                        true,
                        "Successfully restored ${backupData.workers.size} workers, ${backupData.attendanceRecords.size} attendance records, and ${backupData.cashbookEntries.size} ledger entries."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to restore backup: ${e.localizedMessage ?: "Invalid file or format"}")
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLocalData()
        }
    }
}

data class Contact(val name: String, val phone: String)
