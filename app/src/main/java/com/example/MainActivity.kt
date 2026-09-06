package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ui.components.HaazriBottomBar
import com.example.ui.components.HaazriTopBar
import com.example.ui.screens.*
import com.example.ui.theme.HaazriTheme
import com.example.util.AppLockManager
import com.example.util.NotificationHelper
import com.example.viewmodel.AppTab
import com.example.viewmodel.HaazriViewModel
import com.example.viewmodel.ScreenState

class MainActivity : FragmentActivity() {
    private val viewModel: HaazriViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission state updated
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appLockManager = AppLockManager(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                appLockManager.lockApp()
            }
        })

        // Create system notification channels
        NotificationHelper.createNotificationChannels(this)

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
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
                    val savedPin by appLockManager.getPin().collectAsState(initial = "LOADING")
                    val isUnlocked by appLockManager.isUnlocked.collectAsState()

                    if (savedPin == "LOADING") {
                        // Wait for DataStore to load
                    } else if (isLoggedIn) {
                        if (savedPin == null) {
                            SetPinScreen(
                                appLockManager = appLockManager,
                                onPinSet = {}
                            )
                        } else if (!isUnlocked) {
                            AppLockScreen(
                                appLockManager = appLockManager,
                                onUnlocked = {}
                            )
                        } else {
                            HaazriApp(viewModel = viewModel)
                        }
                    } else {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                viewModel.activeScreen.value = ScreenState.MAIN_TABS
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HaazriApp(viewModel: HaazriViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val activeScreen by viewModel.activeScreen.collectAsState()

    // Title text for sub-screens
    val subScreenTitle = when (activeScreen) {
        ScreenState.ADD_WORKER -> "Add worker"
        ScreenState.SELECT_CONTACT -> "Select Contact"
        ScreenState.WORKER_DETAILS -> "Worker Details"
        ScreenState.GEOFENCE_ADMIN -> "Geofence Settings"
        ScreenState.NOTIFICATIONS_SETUP -> "Reminders & Notifications"
        ScreenState.MONTHLY_REPORT -> "Monthly Report"
        ScreenState.BACKUP_RESTORE -> "Backup & Restore"
        ScreenState.PRIVACY_POLICY -> "Privacy Policy"
        ScreenState.TERMS_OF_SERVICE -> "Terms & Conditions"
        ScreenState.DATA_SAFETY -> "Data Safety & Security"
        ScreenState.ABOUT_APP -> "About & Legal"
        else -> ""
    }

    // Back button handling in sub-screens
    BackHandler(enabled = activeScreen != ScreenState.MAIN_TABS) {
        viewModel.activeScreen.value = ScreenState.MAIN_TABS
    }

    Scaffold(
        topBar = {
            HaazriTopBar(
                screenState = activeScreen,
                titleText = subScreenTitle,
                onBackClick = { viewModel.activeScreen.value = ScreenState.MAIN_TABS }
            )
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
                .padding(innerPadding)
        ) {
            when (activeScreen) {
                ScreenState.MAIN_TABS -> {
                    when (activeTab) {
                        AppTab.WORKERS -> WorkersScreen(
                            viewModel = viewModel,
                            onWorkerClick = { workerId ->
                                viewModel.selectedWorkerId.value = workerId
                                viewModel.activeScreen.value = ScreenState.WORKER_DETAILS
                            }
                        )
                        AppTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                        AppTab.CASHBOOK -> CashbookScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
                ScreenState.ADD_WORKER -> AddWorkerScreen(
                    viewModel = viewModel,
                    onDone = { viewModel.activeScreen.value = ScreenState.MAIN_TABS }
                )
                ScreenState.SELECT_CONTACT -> SelectContactScreen(
                    viewModel = viewModel,
                    onContactPicked = { name, phone ->
                        // Navigate to AddWorker with prefilled info
                        viewModel.activeScreen.value = ScreenState.ADD_WORKER
                    }
                )
                ScreenState.WORKER_DETAILS -> WorkerDetailsScreen(viewModel = viewModel)
                ScreenState.GEOFENCE_ADMIN -> GeofenceAdminScreen(viewModel = viewModel)
                ScreenState.NOTIFICATIONS_SETUP -> NotificationsSetupScreen(viewModel = viewModel)
                ScreenState.MONTHLY_REPORT -> MonthlyReportScreen(viewModel = viewModel)
                ScreenState.BACKUP_RESTORE -> BackupRestoreScreen(viewModel = viewModel)
                ScreenState.PRIVACY_POLICY -> PrivacyPolicyScreen(viewModel = viewModel)
                ScreenState.TERMS_OF_SERVICE -> TermsOfServiceScreen(viewModel = viewModel)
                ScreenState.DATA_SAFETY -> DataSafetyScreen(viewModel = viewModel)
                ScreenState.ABOUT_APP -> AboutAppScreen(viewModel = viewModel)
            }
        }
    }
}
