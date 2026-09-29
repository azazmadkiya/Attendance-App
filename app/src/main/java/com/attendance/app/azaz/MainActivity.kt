package com.attendance.app.azaz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.attendance.app.azaz.ui.components.HaazriBottomBar
import com.attendance.app.azaz.ui.components.HaazriTopBar
import com.attendance.app.azaz.ui.screens.*
import com.attendance.app.azaz.ui.theme.HaazriTheme
import com.attendance.app.azaz.util.AppLockManager
import com.attendance.app.azaz.util.NotificationHelper
import com.attendance.app.azaz.viewmodel.AppTab
import com.attendance.app.azaz.viewmodel.HaazriViewModel
import com.attendance.app.azaz.viewmodel.ScreenState

class MainActivity : FragmentActivity() {
    private var crashTrace: String? by mutableStateOf(null)
    private val viewModel: HaazriViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager
    private var processObserver: DefaultLifecycleObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val sw = java.io.StringWriter()
            exception.printStackTrace(java.io.PrintWriter(sw))
            runOnUiThread {
                crashTrace = sw.toString()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appLockManager = AppLockManager(this)

        processObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                appLockManager.lockApp()
            }
        }.also {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }

        // Create system notification channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule automatic 24-hour WorkManager backup task
        com.attendance.app.azaz.util.AutoBackupScheduler.scheduleDailyBackup(this)

        setContent {
            if (crashTrace != null) {
                com.attendance.app.azaz.ui.screens.CrashScreen(crashTrace!!) {
                    crashTrace = null
                }
                return@setContent
            }
            // Request notification permission safely in Compose on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Permission result handled */ }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            HaazriTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                } else {
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    val isEmailVerified by viewModel.isEmailVerified.collectAsState()
                    val activeScreen by viewModel.activeScreen.collectAsState()

                    if (!isLoggedIn) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                if (!appLockManager.hasPromptedAppLock()) {
                                    viewModel.activeScreen.value = ScreenState.SET_PIN
                                } else {
                                    viewModel.activeScreen.value = ScreenState.MAIN_TABS
                                }
                            }
                        )
                    } else if (activeScreen == ScreenState.EMAIL_VERIFICATION) {
                        EmailVerificationScreen(
                            viewModel = viewModel,
                            onVerified = {
                                viewModel.isEmailVerified.value = true
                                if (!appLockManager.hasPromptedAppLock()) {
                                    viewModel.activeScreen.value = ScreenState.SET_PIN
                                } else {
                                    viewModel.activeScreen.value = ScreenState.MAIN_TABS
                                }
                            }
                        )
                    } else if (activeScreen == ScreenState.PROFILE_SETUP || activeScreen == ScreenState.USER_PROFILE) {
                        UserProfileScreen(
                            viewModel = viewModel,
                            isInitialSetup = (activeScreen == ScreenState.PROFILE_SETUP),
                            onComplete = {
                                if (!appLockManager.hasPromptedAppLock()) {
                                    viewModel.activeScreen.value = ScreenState.SET_PIN
                                } else {
                                    viewModel.activeScreen.value = ScreenState.MAIN_TABS
                                }
                            }
                        )
                    } else if (activeScreen == ScreenState.SET_PIN) {
                        SetPinScreen(
                            appLockManager = appLockManager,
                            onPinSet = {
                                viewModel.activeScreen.value = ScreenState.MAIN_TABS
                            }
                        )
                    } else {
                        val savedPin by appLockManager.pinFlow.collectAsState(initial = appLockManager.getSavedPinSync())
                        val isUnlocked by appLockManager.isUnlocked.collectAsState()
                        val isAppLockEnabled = !savedPin.isNullOrBlank()

                        if (isAppLockEnabled && !isUnlocked) {
                            AppLockScreen(
                                appLockManager = appLockManager,
                                onUnlocked = {
                                    // Successfully unlocked
                                }
                            )
                        } else {
                            HaazriApp(
                                viewModel = viewModel,
                                appLockManager = appLockManager
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        processObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
    }
}

@Composable
fun HaazriApp(
    viewModel: HaazriViewModel,
    appLockManager: AppLockManager = AppLockManager(LocalContext.current)
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val activeScreen by viewModel.activeScreen.collectAsState()

    // Title text for sub-screens
    val subScreenTitle = when (activeScreen) {
        ScreenState.ADD_WORKER -> "Add worker"
        ScreenState.SELECT_CONTACT -> "Select Contact"
        ScreenState.WORKER_DETAILS -> "Worker Details"
        ScreenState.NOTIFICATIONS_SETUP -> "Reminders & Notifications"
        ScreenState.MONTHLY_REPORT -> "Monthly Report"
        ScreenState.BACKUP_RESTORE -> "Backup & Restore"
        ScreenState.PRIVACY_POLICY -> "Privacy Policy"
        ScreenState.TERMS_OF_SERVICE -> "Terms & Conditions"
        ScreenState.DATA_SAFETY -> "Data Safety & Security"
        ScreenState.ABOUT_APP -> "About & Legal"
        ScreenState.SET_PIN -> "App Security Lock"
        ScreenState.APP_LOCK -> "App Locked"
        else -> ""
    }

    // Back button handling in sub-screens
    BackHandler(enabled = activeScreen != ScreenState.MAIN_TABS) {
        viewModel.activeScreen.value = ScreenState.MAIN_TABS
    }

    Scaffold(
        topBar = {
            if (activeScreen != ScreenState.SET_PIN && activeScreen != ScreenState.APP_LOCK) {
                HaazriTopBar(
                    screenState = activeScreen,
                    titleText = subScreenTitle,
                    onBackClick = { viewModel.activeScreen.value = ScreenState.MAIN_TABS }
                )
            }
        },
        bottomBar = {
            if (activeScreen == ScreenState.MAIN_TABS) {
                HaazriBottomBar(
                    currentTab = activeTab,
                    onTabSelected = { tab ->
                        viewModel.activeTab.value = tab
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (activeScreen == ScreenState.SET_PIN || activeScreen == ScreenState.APP_LOCK) PaddingValues(0.dp) else innerPadding)
        ) {
            when (activeScreen) {
                ScreenState.MAIN_TABS -> {
                    when (activeTab) {
                        AppTab.WORKERS -> WorkersScreen(
                            viewModel = viewModel,
                            onWorkerClick = { workerId ->
                                viewModel.setSelectedWorkerId(workerId)
                            }
                        )
                        AppTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                        AppTab.CASHBOOK -> CashbookScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        AppTab.REPORTS -> MonthlyReportScreen(viewModel = viewModel)
                    }
                }
                ScreenState.HOME -> WorkersScreen(viewModel = viewModel, onWorkerClick = { viewModel.setSelectedWorkerId(it) })
                ScreenState.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                ScreenState.CASHBOOK -> CashbookScreen(viewModel = viewModel)
                ScreenState.WORKERS -> WorkersScreen(viewModel = viewModel, onWorkerClick = { viewModel.setSelectedWorkerId(it) })
                ScreenState.SETTINGS -> SettingsScreen(viewModel = viewModel)
                ScreenState.LOGIN -> LoginScreen(viewModel = viewModel, onLoginSuccess = {
                    if (!appLockManager.hasPromptedAppLock()) {
                        viewModel.setScreen(ScreenState.SET_PIN)
                    } else {
                        viewModel.setScreen(ScreenState.MAIN_TABS)
                    }
                })
                ScreenState.EMAIL_VERIFICATION -> EmailVerificationScreen(viewModel = viewModel, onVerified = { viewModel.setScreen(ScreenState.MAIN_TABS) })
                ScreenState.USER_PROFILE -> UserProfileScreen(viewModel = viewModel, isInitialSetup = false, onComplete = { viewModel.setScreen(ScreenState.SETTINGS) })
                ScreenState.PROFILE_SETUP -> UserProfileScreen(viewModel = viewModel, isInitialSetup = true, onComplete = {
                    if (!appLockManager.hasPromptedAppLock()) {
                        viewModel.setScreen(ScreenState.SET_PIN)
                    } else {
                        viewModel.setScreen(ScreenState.MAIN_TABS)
                    }
                })
                ScreenState.APP_LOCK -> AppLockScreen(appLockManager = appLockManager, onUnlocked = { viewModel.setScreen(ScreenState.MAIN_TABS) })
                ScreenState.MONTHLY_REPORT -> MonthlyReportScreen(viewModel = viewModel)
                ScreenState.BACKUP_RESTORE -> BackupRestoreScreen(viewModel = viewModel)
                ScreenState.DATA_SAFETY -> DataSafetyScreen(viewModel = viewModel)
                ScreenState.ABOUT_APP -> AboutAppScreen(viewModel = viewModel)
                ScreenState.TERMS_OF_SERVICE -> TermsOfServiceScreen(viewModel = viewModel)
                ScreenState.PRIVACY_POLICY -> PrivacyPolicyScreen(viewModel = viewModel)
                ScreenState.NOTIFICATIONS_SETUP -> NotificationsSetupScreen(viewModel = viewModel)
                ScreenState.SET_PIN -> PinSecuritySettingsScreen(appLockManager = appLockManager, onBack = { viewModel.setScreen(ScreenState.SETTINGS) })
                ScreenState.SELECT_CONTACT -> SelectContactScreen(
                    viewModel = viewModel,
                    onContactPicked = { name, phone ->
                        viewModel.setScreen(ScreenState.ADD_WORKER)
                    }
                )
                ScreenState.ADD_WORKER -> AddWorkerScreen(
                    viewModel = viewModel,
                    onDone = { viewModel.setScreen(ScreenState.MAIN_TABS) }
                )
                ScreenState.WORKER_DETAILS -> WorkerDetailsScreen(viewModel = viewModel)
            }
        }
    }
}
