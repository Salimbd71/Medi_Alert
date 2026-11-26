package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineWithSchedules
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.GlassBackdrop
import com.example.ui.theme.MediAlertTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SavedPdfReport
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val prefs = uiState.preferences
    val locked = prefs.isLockActive && !isUnlocked

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

        fun openSavedPdf(report: SavedPdfReport) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(report.uri, "application/pdf")
                clipData = ClipData.newUri(context.contentResolver, report.fileName, report.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(Intent.createChooser(viewIntent, strings.openPdfButton))
            }.onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar(strings.noPdfReaderMsg)
                }
            }
        }

        LaunchedEffect(strings) {
            viewModel.pdfSavedEvents.collect { saved ->
                val result = snackbarHostState.showSnackbar(
                    message = "${strings.pdfSavedToPrefix}: ${saved.displayPath}",
                    actionLabel = strings.openPdfLabel,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    openSavedPdf(saved)
                }
            }
        }

        if (locked) {
            AppLockScreen(
                language = prefs.language,
                hasPinLock = prefs.hasPinLock,
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            MediAlertTopBar(
                                title = when (currentTab) {
                                    MainTab.TODAY -> strings.appName
                                    MainTab.MEDICINES -> strings.navMedicines
                                    MainTab.HISTORY -> strings.navHistory
                                    MainTab.SETTINGS -> strings.navSettings
                                },
                                subtitle = if (currentTab == MainTab.TODAY) {
                                    if (prefs.language == AppLanguage.BANGLA) "স্মার্ট অফলাইন পিল ট্র্যাকার" else "Smart Offline Pill Tracker"
                                } else {
                                    strings.appName
                                },
                                language = prefs.language,
                                onToggleLanguage = {
                                    val next = if (prefs.language == AppLanguage.ENGLISH) {
                                        AppLanguage.BANGLA
                                    } else {
                                        AppLanguage.ENGLISH
                                    }
                                    viewModel.setLanguage(next)
                                }
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.80f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    tonalElevation = 0.dp,
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
                                        viewModel.savePdfReportToDownloads(strings.backupErrorMsg)
                                    },
                                    onOpenSavedPdf = ::openSavedPdf,
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
                                    onSetPinLock = viewModel::setPinLock,
                                    onClearPinLock = viewModel::clearPinLock,
                                    onBiometricToggle = viewModel::setBiometricEnabled,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
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

@Composable
private fun MediAlertTopBar(
    title: String,
    subtitle: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val barShape = RoundedCornerShape(22.dp)
    val barBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.56f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF6FCFD).copy(alpha = 0.68f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f)
            )
        )
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(barShape)
            .background(brush = barBrush, shape = barShape),
        shape = barShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_topbar_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.32f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_medi_alert_fg),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                        .clickable(onClick = onToggleLanguage)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("topbar_language_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "EN" else "বাংলা",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

