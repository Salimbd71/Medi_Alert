package com.slm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slm.R
import com.slm.data.model.AppLanguage
import com.slm.data.model.DoseStatus
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineType
import com.slm.data.model.ScheduledDoseItem
import com.slm.data.model.TimeOfDayBucket
import com.slm.domain.DoseSchedulerCalculator
import com.slm.ui.strings.LocalAppStrings
import com.slm.ui.theme.AdherenceRingChart
import com.slm.ui.theme.AmberWarning
import com.slm.ui.theme.CoralAlert
import com.slm.ui.theme.EmeraldTaken
import com.slm.ui.theme.GlassCard
import com.slm.ui.theme.IndigoMuted
import com.slm.ui.theme.StatusPill
import com.slm.ui.theme.mediAlertFilterChipBorder
import com.slm.ui.theme.mediAlertFilterChipColors
import com.slm.ui.viewmodel.MediAlertUiState
import com.slm.util.TimeFormatter

@Composable
fun DashboardScreen(
    uiState: MediAlertUiState,
    onSelectPatient: (Long?) -> Unit,
    onTakeDose: (ScheduledDoseItem) -> Unit,
    onSnoozeDose: (ScheduledDoseItem, Int) -> Unit,
    onSkipDose: (ScheduledDoseItem) -> Unit,
    onRefillClick: (MedicineEntity) -> Unit,
    onFixPermissionsClick: () -> Unit,
    onAddMedicineClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val language = uiState.preferences.language

    val groupedDoses = remember(uiState.todayDoses) {
        uiState.todayDoses.groupBy { it.bucket }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Patient Switcher Row
        item(key = "patient_switcher") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = uiState.preferences.activePatientId == null
                FilterChip(
                    selected = allSelected,
                    onClick = { onSelectPatient(null) },
                    label = { Text(strings.allPatients) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = mediAlertFilterChipColors(),
                    border = mediAlertFilterChipBorder(allSelected),
                    modifier = Modifier.testTag("patient_chip_all")
                )
                uiState.patients.forEach { patient ->
                    val patientSelected = uiState.preferences.activePatientId == patient.id
                    FilterChip(
                        selected = patientSelected,
                        onClick = { onSelectPatient(patient.id) },
                        label = { Text("${patient.name} (${patient.relation})") },
                        colors = mediAlertFilterChipColors(),
                        border = mediAlertFilterChipBorder(patientSelected),
                        modifier = Modifier.testTag("patient_chip_${patient.id}")
                    )
                }
            }
        }

        // Hero Banner + Live Countdown + Today's Adherence Ring
        item(key = "hero_countdown") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_wellness_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.28f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.nextDoseTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val nextDose = uiState.nextDoseInfo
                            if (nextDose != null) {
                                Text(
                                    text = TimeFormatter.formatCountdown(nextDose.millisRemaining, language),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${nextDose.item.medicine.name} • ${
                                        TimeFormatter.formatTime(
                                            nextDose.item.schedule.hour,
                                            nextDose.item.schedule.minute,
                                            language
                                        )
                                    }",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${nextDose.item.patient.name} • ${strings.mealTiming(nextDose.item.medicine.mealTiming)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = strings.noUpcomingDose,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.allDoneSubtext,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))
                        AdherenceRingChart(
                            percentage = uiState.todayAdherencePercent,
                            language = language,
                            label = strings.adherenceToday,
                            size = 92.dp
                        )
                    }
                }
            }
        }

        // Permission Advisory Banner
        if (!uiState.permissionHealth.allOptimal) {
            item(key = "permission_banner") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightColor = AmberWarning
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmberWarning.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = strings.permissionBannerTitle,
                                tint = AmberWarning
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.permissionBannerTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.permissionBannerBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onFixPermissionsClick,
                            modifier = Modifier.testTag("fix_permissions_button")
                        ) {
                            Text(strings.fixNow)
                        }
                    }
                }
            }
        }

        // Low Stock Alert Strip
        if (uiState.lowStockMedicines.isNotEmpty()) {
            item(key = "low_stock_strip") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightColor = CoralAlert
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = strings.lowStockTitle,
                                tint = CoralAlert
                            )
                            Text(
                                text = strings.lowStockTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralAlert
                            )
                        }
                        uiState.lowStockMedicines.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.medicine.name} — ${
                                        TimeFormatter.localizeDigits(
                                            TimeFormatter.formatNumber(item.medicine.stockCount),
                                            language
                                        )
                                    } ${item.medicine.dosageUnit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { onRefillClick(item.medicine) },
                                    modifier = Modifier.testTag("refill_quick_${item.medicine.id}")
                                ) {
                                    Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.lowStockAction)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty State or Time-of-Day Dose Timeline
        if (uiState.todayDoses.isEmpty()) {
            item(key = "empty_doses") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_meds),
                            contentDescription = strings.emptyDosesTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Text(
                            text = strings.emptyDosesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.emptyDosesBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddMedicineClick,
                            modifier = Modifier.testTag("empty_add_medicine_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addMedicine)
                        }
                    }
                }
            }
        } else {
            TimeOfDayBucket.entries.forEach { bucket ->
                val doses = groupedDoses[bucket].orEmpty()
                if (doses.isNotEmpty()) {
                    item(key = "bucket_${bucket.name}") {
                        Text(
                            text = strings.bucket(bucket),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    items(
                        items = doses,
                        key = { "dose_${it.schedule.id}_${it.scheduledDateEpochDay}" }
                    ) { dose ->
                        DoseTimelineCard(
                            dose = dose,
                            nowMillis = uiState.currentTimeMillis,
                            language = language,
                            onTake = { onTakeDose(dose) },
                            onSnooze = { mins -> onSnoozeDose(dose, mins) },
                            onSkip = { onSkipDose(dose) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoseTimelineCard(
    dose: ScheduledDoseItem,
    nowMillis: Long,
    language: AppLanguage,
    onTake: () -> Unit,
    onSnooze: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val actionWindowOpen = DoseSchedulerCalculator.isActionWindowOpen(dose, nowMillis)

    val (statusBg, statusFg) = when (dose.status) {
        DoseStatus.TAKEN -> EmeraldTaken.copy(alpha = 0.16f) to EmeraldTaken
        DoseStatus.SKIPPED -> IndigoMuted.copy(alpha = 0.16f) to IndigoMuted
        DoseStatus.MISSED -> CoralAlert.copy(alpha = 0.16f) to CoralAlert
        DoseStatus.SNOOZED -> AmberWarning.copy(alpha = 0.18f) to AmberWarning
        DoseStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dose_card_${dose.schedule.id}"),
        highlightColor = statusFg
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(statusBg)
                            .then(
                                if (dose.status == DoseStatus.TAKEN) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = EmeraldTaken,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = medicineTypeIcon(dose.medicine.type),
                            contentDescription = strings.medicineDisplayType(dose.medicine.type, dose.medicine.customTypeName),
                            tint = statusFg
                        )
                    }
                    if (dose.status == DoseStatus.TAKEN) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(EmeraldTaken)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .testTag("taken_tick_badge_${dose.schedule.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = strings.doseStatus(DoseStatus.TAKEN),
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dose.medicine.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        StatusPill(
                            text = strings.doseStatus(dose.status),
                            containerColor = statusBg,
                            contentColor = statusFg
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${
                                TimeFormatter.formatTime(
                                    dose.schedule.hour,
                                    dose.schedule.minute,
                                    language
                                )
                            } • ${dose.medicine.dosageAmount} ${dose.medicine.dosageUnit} • ${
                                strings.mealTiming(dose.medicine.mealTiming)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dose.patient.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = actionWindowOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTake,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("take_dose_${dose.schedule.id}"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.takeAction,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("snooze_dose_${dose.schedule.id}"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = strings.snoozeAction,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.snooze5) },
                                onClick = {
                                    showSnoozeMenu = false
                                    onSnooze(5)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.snooze10) },
                                onClick = {
                                    showSnoozeMenu = false
                                    onSnooze(10)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.snooze15) },
                                onClick = {
                                    showSnoozeMenu = false
                                    onSnooze(15)
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("skip_dose_${dose.schedule.id}"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.skipAction,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !actionWindowOpen && dose.status == DoseStatus.PENDING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.actionsUnlockHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun medicineTypeIcon(type: MedicineType): ImageVector = when (type) {
    MedicineType.TABLET -> Icons.Rounded.Medication
    MedicineType.CAPSULE -> Icons.Rounded.LocalPharmacy
    MedicineType.SYRUP -> Icons.Rounded.WaterDrop
    MedicineType.INJECTION -> Icons.Rounded.Vaccines
    MedicineType.DROPS -> Icons.Rounded.Opacity
    MedicineType.INHALER -> Icons.Rounded.Air
    MedicineType.CREAM_OINTMENT -> Icons.Rounded.Healing
    MedicineType.SPRAY -> Icons.Rounded.Shower
    MedicineType.POWDER_SACHET -> Icons.Rounded.Grain
    MedicineType.SUPPOSITORY -> Icons.Rounded.Science
    MedicineType.OTHER -> Icons.Rounded.Category
}
