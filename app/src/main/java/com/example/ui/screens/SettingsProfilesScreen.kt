package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.PatientEntity
import com.example.data.model.ThemeMode
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldTaken
import com.example.ui.theme.GlassCard
import com.example.ui.theme.StatusPill
import com.example.ui.viewmodel.MediAlertUiState
import com.example.util.PermissionHelper
import com.example.util.TimeFormatter

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
    onEnableAppLock: (String, Boolean) -> Unit,
    onDisableAppLock: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onExportJsonBackup: () -> Unit,
    onRestoreJsonBackup: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = uiState.preferences

    var editingPatient by remember { mutableStateOf<PatientEntity?>(null) }
    var showPatientDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }

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
                            FilterChip(
                                selected = prefs.autoMissedWindowMinutes == mins,
                                onClick = { onMissedWindowChange(mins) },
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
                        isGranted = health.isIgnoringBatteryOptimizations,
                        grantedLabel = strings.granted,
                        fixLabel = strings.needsAttention,
                        onClick = { PermissionHelper.openBatteryOptimizationSettings(context) }
                    )
                }
            }
        }

        // Security: PIN & Biometric App Lock
        item(key = "security_section") {
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
                            Text(strings.appLockTitle, style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = prefs.appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showSetPinDialog = true
                                } else {
                                    onDisableAppLock()
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (prefs.appLockEnabled) {
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
                                Text(strings.biometricUnlock, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = prefs.biometricEnabled,
                                onCheckedChange = onBiometricToggle,
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
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
            onConfirm = { pin, biometric ->
                onEnableAppLock(pin, biometric)
                showSetPinDialog = false
            }
        )
    }
}

@Composable
private fun PermissionDiagnosticRow(
    title: String,
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
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
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
    onConfirm: (String, Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    var pin by remember { mutableStateOf("") }
    var useBiometric by remember { mutableStateOf(true) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.biometricUnlock, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = useBiometric, onCheckedChange = { useBiometric = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin, useBiometric) },
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
