package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineWithSchedules
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.GlassBackdrop
import com.example.ui.theme.MediAlertTheme
import com.example.ui.viewmodel.MainViewModel

enum class MainTab {
    TODAY,
    MEDICINES,
    HISTORY,
    SETTINGS
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediAlertApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = uiState.preferences
    val locked = prefs.appLockEnabled && !isUnlocked

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsHealth()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !uiState.permissionHealth.hasNotificationPermission
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MediAlertTheme(
        themeMode = prefs.themeMode,
        language = prefs.language
    ) {
        val strings = LocalAppStrings.current

        val exportBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                viewModel.exportBackupToUri(
                    uri = uri,
                    successMessage = strings.backupExportedMsg,
                    errorMessage = strings.backupErrorMsg
                )
            }
        }

        val restoreBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.restoreBackupFromUri(
                    uri = uri,
                    successMessage = strings.backupRestoredMsg,
                    errorMessage = strings.backupErrorMsg
                )
            }
        }

        val exportPdfLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                viewModel.exportPdfReportToUri(
                    uri = uri,
                    successMessage = strings.pdfSavedMsg,
                    errorMessage = strings.backupErrorMsg
                )
            }
        }

        if (locked) {
            AppLockScreen(
                language = prefs.language,
                biometricEnabled = prefs.biometricEnabled,
                onVerifyPin = { pin -> viewModel.verifyAndUnlock(pin) },
                onBiometricSuccess = { viewModel.unlockBiometric() }
            )
            return@MediAlertTheme
        }

        var currentTab by remember { mutableStateOf(MainTab.TODAY) }
        var showAddEditDialog by remember { mutableStateOf(false) }
        var editingMedicine by remember { mutableStateOf<MedicineWithSchedules?>(null) }
        var refillingMedicine by remember { mutableStateOf<MedicineEntity?>(null) }

        GlassBackdrop {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ) {
                            Spacer(Modifier.size(16.dp))
                            MainTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tabLabel(tab, strings)
                                        )
                                    },
                                    label = { Text(tabLabel(tab, strings)) },
                                    modifier = Modifier.testTag("rail_${tab.name}")
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (currentTab) {
                                            MainTab.TODAY -> strings.appName
                                            MainTab.MEDICINES -> strings.navMedicines
                                            MainTab.HISTORY -> strings.navHistory
                                            MainTab.SETTINGS -> strings.navSettings
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        },
                        floatingActionButton = {
                            if (currentTab == MainTab.TODAY || currentTab == MainTab.MEDICINES) {
                                FloatingActionButton(
                                    onClick = {
                                        editingMedicine = null
                                        showAddEditDialog = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_add_medicine")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = strings.addMedicine)
                                }
                            }
                        },
                        bottomBar = {
                            if (!useNavRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = tabIcon(tab, selected),
                                                    contentDescription = tabLabel(tab, strings)
                                                )
                                            },
                                            label = { Text(tabLabel(tab, strings)) },
                                            modifier = Modifier.testTag("nav_${tab.name}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Crossfade(
                            targetState = currentTab,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            label = "tab_crossfade"
                        ) { tab ->
                            when (tab) {
                                MainTab.TODAY -> DashboardScreen(
                                    uiState = uiState,
                                    onSelectPatient = viewModel::setActivePatientId,
                                    onTakeDose = viewModel::markDoseTaken,
                                    onSnoozeDose = viewModel::snoozeDose,
                                    onSkipDose = viewModel::markDoseSkipped,
                                    onRefillClick = { refillingMedicine = it },
                                    onFixPermissionsClick = { currentTab = MainTab.SETTINGS },
                                    onAddMedicineClick = {
                                        editingMedicine = null
                                        showAddEditDialog = true
                                    }
                                )

                                MainTab.MEDICINES -> MedicinesScreen(
                                    uiState = uiState,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onTypeFilterChange = viewModel::selectMedicineTypeFilter,
                                    onAddMedicineClick = {
                                        editingMedicine = null
                                        showAddEditDialog = true
                                    },
                                    onEditMedicineClick = { item ->
                                        editingMedicine = item
                                        showAddEditDialog = true
                                    },
                                    onDeleteMedicine = viewModel::deleteMedicine,
                                    onToggleActive = viewModel::toggleMedicineActive,
                                    onRefillClick = { refillingMedicine = it }
                                )

                                MainTab.HISTORY -> HistoryReportsScreen(
                                    uiState = uiState,
                                    onSelectPeriod = viewModel::selectReportPeriod,
                                    onSelectDateEpochDay = viewModel::selectDateEpochDay,
                                    onSavePdfReport = {
                                        exportPdfLauncher.launch("medi_alert_report_${System.currentTimeMillis()}.pdf")
                                    },
                                    onSharePdfReport = {
                                        viewModel.sharePdfReport(
                                            onUriReady = { uri ->
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(shareIntent, strings.sharePdfReport)
                                                )
                                            },
                                            errorMessage = strings.backupErrorMsg
                                        )
                                    },
                                    onTakeDose = viewModel::markDoseTaken,
                                    onSnoozeDose = viewModel::snoozeDose,
                                    onSkipDose = viewModel::markDoseSkipped
                                )

                                MainTab.SETTINGS -> SettingsProfilesScreen(
                                    uiState = uiState,
                                    onSavePatient = viewModel::savePatient,
                                    onDeletePatient = viewModel::deletePatient,
                                    onSelectPatient = viewModel::setActivePatientId,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onLanguageChange = viewModel::setLanguage,
                                    onCustomSoundSelected = viewModel::setCustomAlarmSoundUri,
                                    onVoiceReminderChange = viewModel::setVoiceReminderEnabled,
                                    onTestVoiceClick = { viewModel.testVoiceReminder(prefs.language) },
                                    onMissedWindowChange = viewModel::setAutoMissedWindowMinutes,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onEnableAppLock = viewModel::enableAppLock,
                                    onDisableAppLock = viewModel::disableAppLock,
                                    onBiometricToggle = viewModel::setBiometricEnabled,
                                    onExportJsonBackup = {
                                        exportBackupLauncher.launch("medi_alert_backup_${System.currentTimeMillis()}.json")
                                    },
                                    onRestoreJsonBackup = {
                                        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddEditDialog) {
            AddEditMedicineDialog(
                initialMedicine = editingMedicine,
                patients = uiState.patients,
                defaultPatientId = prefs.activePatientId,
                language = prefs.language,
                onDismiss = { showAddEditDialog = false },
                onSave = { med, schedules, oldRxPath ->
                    viewModel.saveMedicine(med, schedules, oldRxPath)
                    showAddEditDialog = false
                }
            )
        }

        refillingMedicine?.let { med ->
            RefillStockDialog(
                medicine = med,
                language = prefs.language,
                onDismiss = { refillingMedicine = null },
                onConfirmRefill = { qty -> viewModel.refillMedicine(med.id, qty) }
            )
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.TODAY -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainTab.MEDICINES -> if (selected) Icons.Rounded.Medication else Icons.Outlined.Medication
    MainTab.HISTORY -> if (selected) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth
    MainTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}

private fun tabLabel(tab: MainTab, strings: com.example.ui.strings.AppStrings): String = when (tab) {
    MainTab.TODAY -> strings.navToday
    MainTab.MEDICINES -> strings.navMedicines
    MainTab.HISTORY -> strings.navHistory
    MainTab.SETTINGS -> strings.navSettings
}
