package com.slm.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.slm.BuildConfig
import com.slm.R
import com.slm.data.model.AppLanguage
import com.slm.data.model.PatientEntity
import com.slm.data.model.ThemeMode
import com.slm.ui.strings.LocalAppStrings
import com.slm.ui.theme.AmberWarning
import com.slm.ui.theme.CoralAlert
import com.slm.ui.theme.EmeraldTaken
import com.slm.ui.theme.GlassCard
import com.slm.ui.theme.StatusPill
import com.slm.ui.theme.mediAlertFilterChipBorder
import com.slm.ui.theme.mediAlertFilterChipColors
import com.slm.ui.theme.mediAlertSegmentedButtonColors
import com.slm.ui.theme.mediAlertSwitchColors
import com.slm.ui.viewmodel.MediAlertUiState
import com.slm.util.PermissionHelper
import com.slm.util.TimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsProfilesScreen(
    uiState: MediAlertUiState,
    onSavePatient: (PatientEntity) -> Unit,
    onDeletePatient: (PatientEntity) -> Unit,
    onSelectPatient: (Long?) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onCustomSoundSelected: (String?) -> Unit,
    onVoiceReminderChange: (Boolean) -> Unit,
    onTestVoiceClick: () -> Unit,
    onMissedWindowChange: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onSetPinLock: (String) -> Unit,
    onClearPinLock: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onExportJsonBackup: () -> Unit,
    onRestoreJsonBackup: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = uiState.preferences
    val scope = rememberCoroutineScope()

    var editingPatient by remember { mutableStateOf<PatientEntity?>(null) }
    var showPatientDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }
    var legalDialogContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshPermissions()
        scope.launch {
            delay(300L)
            onRefreshPermissions()
            if (!PermissionHelper.checkHealth(context).isIgnoringBatteryOptimizations) {
                showBatteryGuideDialog = true
            }
        }
    }

    val appInfoBatteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshPermissions()
        scope.launch {
            delay(300L)
            onRefreshPermissions()
        }
    }

    fun openMailDeveloper() {
        runCatching {
            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:salimbd.ga@gmail.com")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("salimbd.ga@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "${strings.appName} - Support / Feedback")
            }
            context.startActivity(Intent.createChooser(mailIntent, strings.mailMeLabel))
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            onCustomSoundSelected(uri?.toString())
        }
    }

    val ringtoneName = remember(prefs.customAlarmSoundUri) {
        val uriStr = prefs.customAlarmSoundUri
        if (uriStr.isNullOrBlank()) {
            strings.defaultAlarmSound
        } else {
            runCatching {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriStr))
                ringtone?.getTitle(context) ?: strings.customAlarmSound
            }.getOrDefault(strings.customAlarmSound)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-Patient Profiles
        item(key = "profiles_section") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.profilesSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                editingPatient = null
                                showPatientDialog = true
                            },
                            modifier = Modifier.testTag("add_patient_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.addPatient)
                        }
                    }

                    uiState.patients.forEach { patient ->
                        val isSelected = prefs.activePatientId == patient.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPatient(if (isSelected) null else patient.id) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = patient.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = patient.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val agePart = patient.age?.let {
                                    " • ${TimeFormatter.localizeDigits(it.toString(), prefs.language)}y"
                                } ?: ""
                                Text(
                                    text = "${patient.relation}$agePart",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingPatient = patient
                                    showPatientDialog = true
                                },
                                modifier = Modifier.testTag("edit_patient_${patient.id}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.editPatient)
                            }
                            if (uiState.patients.size > 1) {
                                IconButton(
                                    onClick = { onDeletePatient(patient) },
                                    modifier = Modifier.testTag("delete_patient_${patient.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = strings.delete,
                                        tint = CoralAlert
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Theme & Language
        item(key = "appearance_section") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.appearanceSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(strings.themeLabel, style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val themes = listOf(
                            ThemeMode.SYSTEM to strings.themeSystem,
                            ThemeMode.LIGHT to strings.themeLight,
                            ThemeMode.DARK to strings.themeDark
                        )
                        themes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                colors = mediAlertSegmentedButtonColors(),
                                modifier = Modifier.testTag("theme_${mode.name}")
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val langs = listOf(
                            AppLanguage.ENGLISH to "English",
                            AppLanguage.BANGLA to "বাংলা"
                        )
                        langs.forEachIndexed { idx, (lang, label) ->
                            SegmentedButton(
                                selected = prefs.language == lang,
                                onClick = { onLanguageChange(lang) },
                                shape = SegmentedButtonDefaults.itemShape(idx, langs.size),
                                colors = mediAlertSegmentedButtonColors(),
                                modifier = Modifier.testTag("lang_${lang.name}")
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        // Alarms & Voice
        item(key = "alarms_section") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.alarmsSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    prefs.customAlarmSoundUri?.let {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.customAlarmSound, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = ringtoneName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.voiceReminderLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = prefs.voiceReminderEnabled,
                            onCheckedChange = onVoiceReminderChange,
                            colors = mediAlertSwitchColors(),
                            modifier = Modifier.testTag("voice_reminder_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onTestVoiceClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Text(strings.testVoiceReminder)
                    }

                    Text(strings.missedGraceWindow, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { mins ->
                            val selected = prefs.autoMissedWindowMinutes == mins
                            FilterChip(
                                selected = selected,
                                onClick = { onMissedWindowChange(mins) },
                                colors = mediAlertFilterChipColors(),
                                border = mediAlertFilterChipBorder(selected),
                                label = {
                                    Text("${TimeFormatter.localizeDigits(mins.toString(), prefs.language)} ${strings.minutesSuffix}")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Permissions & Battery Health Diagnostics
        item(key = "permissions_section") {
            val health = uiState.permissionHealth
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                highlightColor = if (health.allOptimal) EmeraldTaken else AmberWarning
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.permissionsDiagnostics,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    PermissionDiagnosticRow(
                        title = strings.notificationPerm,
                        isGranted = health.hasNotificationPermission,
                        grantedLabel = strings.granted,
                        fixLabel = strings.needsAttention,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !health.hasNotificationPermission) {
                                onRequestNotificationPermission()
                            } else {
                                PermissionHelper.openNotificationSettings(context)
                            }
                        }
                    )

                    PermissionDiagnosticRow(
                        title = strings.exactAlarmPerm,
                        isGranted = health.canScheduleExactAlarms,
                        grantedLabel = strings.granted,
                        fixLabel = strings.needsAttention,
                        onClick = { PermissionHelper.openExactAlarmSettings(context) }
                    )

                    PermissionDiagnosticRow(
                        title = strings.fullScreenPerm,
                        isGranted = health.canUseFullScreenIntent,
                        grantedLabel = strings.granted,
                        fixLabel = strings.needsAttention,
                        onClick = { PermissionHelper.openFullScreenIntentSettings(context) }
                    )

                    PermissionDiagnosticRow(
                        title = strings.batteryOptPerm,
                        subtitle = if (!health.isIgnoringBatteryOptimizations) strings.batteryOptHint else null,
                        isGranted = health.isIgnoringBatteryOptimizations,
                        grantedLabel = strings.granted,
                        fixLabel = strings.needsAttention,
                        onClick = {
                            if (health.isIgnoringBatteryOptimizations) {
                                appInfoBatteryLauncher.launch(PermissionHelper.buildAppDetailsSettingsIntent(context))
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val directIntent = PermissionHelper.buildRequestIgnoreBatteryOptimizationsIntent(context)
                                if (PermissionHelper.canResolveIntent(context, directIntent)) {
                                    runCatching {
                                        batteryOptLauncher.launch(directIntent)
                                    }.onFailure {
                                        showBatteryGuideDialog = true
                                    }
                                } else {
                                    showBatteryGuideDialog = true
                                }
                            } else {
                                onRefreshPermissions()
                            }
                        }
                    )
                }
            }
        }

        // Security: Flexible PIN OR Fingerprint / Biometric App Lock
        item(key = "security_section") {
            val biometricAvailable = remember(context) {
                BiometricManager.from(context)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.securitySection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.pinLockTitle, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (prefs.hasPinLock) strings.pinLockSetDesc else strings.pinLockUnsetDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (prefs.hasPinLock) {
                                TextButton(
                                    onClick = { showSetPinDialog = true },
                                    modifier = Modifier.testTag("change_pin_button")
                                ) {
                                    Text(strings.changePinLabel)
                                }
                            }
                            Switch(
                                checked = prefs.hasPinLock,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showSetPinDialog = true
                                    } else {
                                        onClearPinLock()
                                    }
                                },
                                colors = mediAlertSwitchColors(),
                                modifier = Modifier.testTag("pin_lock_switch")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.biometricUnlock, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = strings.biometricLockDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !biometricAvailable) {
                                    onShowMessage(strings.biometricUnavailableMsg)
                                } else {
                                    onBiometricToggle(enabled)
                                }
                            },
                            colors = mediAlertSwitchColors(),
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }
                }
            }
        }

        // Offline JSON Backup & Restore
        item(key = "backup_section") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.backupSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportJsonBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.exportJsonBackup, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onRestoreJsonBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_json_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restoreJsonBackup, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Play Store-Friendly Legal Section (Privacy Policy, Terms & Conditions, Mail Me)
        item(key = "legal_section") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("legal_section_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.legalSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                legalDialogContent = strings.privacyPolicyTitle to strings.privacyPolicyBody
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("privacy_policy_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = strings.privacyPolicyTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                legalDialogContent = strings.termsConditionsTitle to strings.termsConditionsBody
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("terms_conditions_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = strings.termsConditionsTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = ::openMailDeveloper,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("legal_mail_me_button")
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = strings.mailMeLabel, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.mailMeLabel)
                    }
                }
            }
        }

        // About Section with Top Border Line
        item(key = "about_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_section_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = strings.aboutSection,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.aboutSection,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            StatusPill(
                                text = "${strings.versionLabel} ${
                                    TimeFormatter.localizeDigits(BuildConfig.VERSION_NAME, prefs.language)
                                }",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("about_version_badge")
                            )
                        }

                        Text(
                            text = strings.developerCredit,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("about_developer_text")
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AboutIconAction(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                testTag = "about_call_button",
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:01737462871"))
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = strings.callDeveloperDesc,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            AboutIconAction(
                                containerColor = EmeraldTaken.copy(alpha = 0.15f),
                                borderColor = EmeraldTaken.copy(alpha = 0.38f),
                                testTag = "about_whatsapp_button",
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801737462871"))
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = strings.whatsappDeveloperDesc,
                                    tint = EmeraldTaken,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            AboutIconAction(
                                containerColor = Color(0xFF1877F2).copy(alpha = 0.14f),
                                borderColor = Color(0xFF1877F2).copy(alpha = 0.38f),
                                testTag = "about_facebook_button",
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://fb.com/salim.naogaon"))
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_facebook),
                                    contentDescription = strings.facebookDeveloperDesc,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            AboutIconAction(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
                                testTag = "about_share_button",
                                onClick = {
                                    runCatching {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, strings.appName)
                                            putExtra(Intent.EXTRA_TEXT, strings.shareAppMessage)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, strings.shareAppDesc)
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = strings.shareAppDesc,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPatientDialog) {
        AddEditPatientDialog(
            initialPatient = editingPatient,
            onDismiss = { showPatientDialog = false },
            onSave = { patient ->
                onSavePatient(patient)
                showPatientDialog = false
            }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onConfirm = { pin ->
                onSetPinLock(pin)
                showSetPinDialog = false
            }
        )
    }

    legalDialogContent?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { legalDialogContent = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { openMailDeveloper() }) {
                    Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.mailMeLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { legalDialogContent = null }) {
                    Text(strings.closeLabel)
                }
            }
        )
    }

    if (showBatteryGuideDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryGuideDialog = false },
            title = { Text(strings.batteryGuideTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.batteryGuideBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            showBatteryGuideDialog = false
                            val listIntent = PermissionHelper.buildBatteryOptimizationListIntent()
                            if (PermissionHelper.canResolveIntent(context, listIntent)) {
                                runCatching { appInfoBatteryLauncher.launch(listIntent) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.openBatteryListLabel)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryGuideDialog = false
                        runCatching {
                            appInfoBatteryLauncher.launch(
                                PermissionHelper.buildAppDetailsSettingsIntent(context)
                            )
                        }
                    }
                ) {
                    Text(strings.openAppInfoLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuideDialog = false }) {
                    Text(strings.closeLabel)
                }
            }
        )
    }
}

@Composable
private fun PermissionDiagnosticRow(
    title: String,
    subtitle: String? = null,
    isGranted: Boolean,
    grantedLabel: String,
    fixLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isGranted) EmeraldTaken else AmberWarning,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(
            text = if (isGranted) grantedLabel else fixLabel,
            containerColor = if (isGranted) EmeraldTaken.copy(alpha = 0.16f) else AmberWarning.copy(alpha = 0.18f),
            contentColor = if (isGranted) EmeraldTaken else AmberWarning
        )
    }
}

@Composable
private fun AddEditPatientDialog(
    initialPatient: PatientEntity?,
    onDismiss: () -> Unit,
    onSave: (PatientEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(initialPatient?.name ?: "") }
    var relation by remember { mutableStateOf(initialPatient?.relation ?: "Family") }
    var ageText by remember { mutableStateOf(initialPatient?.age?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPatient == null) strings.addPatient else strings.editPatient) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.patientNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_name_input")
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text(strings.relationLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_relation_input")
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.ageLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(
                            PatientEntity(
                                id = initialPatient?.id ?: 0L,
                                name = trimmed,
                                relation = relation.ifBlank { "Family" },
                                age = ageText.toIntOrNull(),
                                avatarColorHex = initialPatient?.avatarColorHex ?: "#0A6C74",
                                isDefault = initialPatient?.isDefault ?: false,
                                createdAtMillis = initialPatient?.createdAtMillis ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_patient_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun SetPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.setPinPrompt) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        pin = digits
                    },
                    label = { Text("4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4,
                modifier = Modifier.testTag("confirm_pin_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun AboutIconAction(
    containerColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

