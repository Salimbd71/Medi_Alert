package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.MealTiming
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineType
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduleEntity
import com.example.domain.DoseSchedulerCalculator
import com.example.ui.strings.LocalAppStrings
import com.example.util.PrescriptionFileHelper
import com.example.util.TimeFormatter
import kotlinx.coroutines.launch

data class TimeSlotDraft(
    val hour: Int,
    val minute: Int
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineDialog(
    initialMedicine: MedicineWithSchedules?,
    patients: List<PatientEntity>,
    defaultPatientId: Long?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (MedicineEntity, List<ScheduleEntity>, String?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val existing = initialMedicine?.medicine
    var selectedPatientId by remember {
        mutableStateOf(
            existing?.patientId ?: defaultPatientId ?: patients.firstOrNull()?.id ?: 1L
        )
    }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: MedicineType.TABLET) }
    var customTypeName by remember { mutableStateOf(existing?.customTypeName ?: "") }
    var dosageAmount by remember { mutableStateOf(existing?.dosageAmount ?: "1") }
    var dosageUnit by remember { mutableStateOf(existing?.dosageUnit ?: strings.defaultUnitFor(type)) }
    val allKnownUnits = remember(strings) {
        MedicineType.entries
            .flatMap { listOf(strings.defaultUnitFor(it)) + strings.suggestedUnitsFor(it) }
            .map { it.lowercase() }
            .toSet()
    }
    var unitManuallyEdited by remember {
        mutableStateOf(
            existing != null && existing.dosageUnit.trim().lowercase() !in allKnownUnits
        )
    }
    var formDropdownExpanded by remember { mutableStateOf(false) }
    var formSearchQuery by remember { mutableStateOf("") }
    var showMoreFormChips by remember {
        mutableStateOf(
            existing != null && existing.type !in MedicineType.commonEntries.take(5)
        )
    }
    var mealTiming by remember { mutableStateOf(existing?.mealTiming ?: MealTiming.AFTER_FOOD) }

    val todayEpochDay = remember { DoseSchedulerCalculator.epochDayFromMillis(System.currentTimeMillis()) }
    var startDateEpochDay by remember { mutableStateOf(existing?.startDateEpochDay ?: todayEpochDay) }
    var hasEndDate by remember { mutableStateOf(existing?.endDateEpochDay != null) }
    var endDateEpochDay by remember { mutableStateOf(existing?.endDateEpochDay ?: (todayEpochDay + 14)) }

    var stockCountText by remember {
        mutableStateOf(existing?.let { TimeFormatter.formatNumber(it.stockCount) } ?: "30")
    }
    var lowStockText by remember {
        mutableStateOf(existing?.let { TimeFormatter.formatNumber(it.lowStockThreshold) } ?: "5")
    }
    var doseQtyText by remember {
        mutableStateOf(existing?.let { TimeFormatter.formatNumber(it.doseQuantityPerIntake) } ?: "1")
    }
    var voiceReminderEnabled by remember { mutableStateOf(existing?.voiceReminderEnabled ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var prescriptionUri by remember { mutableStateOf(existing?.prescriptionUri) }
    var prescriptionMime by remember { mutableStateOf(existing?.prescriptionMimeType) }

    val timeSlots = remember {
        mutableStateListOf<TimeSlotDraft>().apply {
            val existingSchedules = initialMedicine?.schedules.orEmpty()
            if (existingSchedules.isEmpty()) {
                add(TimeSlotDraft(8, 0))
            } else {
                existingSchedules.forEach { add(TimeSlotDraft(it.hour, it.minute)) }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                PrescriptionFileHelper.copyToInternalStorage(context, uri).onSuccess { copied ->
                    prescriptionUri = copied.internalPath
                    prescriptionMime = copied.mimeType
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                PrescriptionFileHelper.copyToInternalStorage(context, uri).onSuccess { copied ->
                    prescriptionUri = copied.internalPath
                    prescriptionMime = copied.mimeType
                }
            }
        }
    }

    fun pickDate(initialEpochDay: Long, onPicked: (Long) -> Unit) {
        val (y, m, d) = DoseSchedulerCalculator.yearMonthDayFromEpochDay(initialEpochDay)
        DatePickerDialog(context, { _, year, month, day ->
            onPicked(DoseSchedulerCalculator.toEpochDay(year, month, day))
        }, y, m, d).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) strings.addMedicine else strings.editMedicine,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (patients.size > 1) {
                    Text(strings.patientLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        patients.forEach { patient ->
                            FilterChip(
                                selected = selectedPatientId == patient.id,
                                onClick = { selectedPatientId = patient.id },
                                label = { Text(patient.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.medicineNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                fun selectMedicineForm(medType: MedicineType, customName: String? = null) {
                    val currentNormalized = dosageUnit.trim().lowercase()
                    val shouldAutoSet = !unitManuallyEdited ||
                        currentNormalized.isEmpty() ||
                        currentNormalized in allKnownUnits
                    type = medType
                    if (medType == MedicineType.OTHER && customName != null) {
                        customTypeName = customName
                    }
                    if (shouldAutoSet) {
                        dosageUnit = strings.defaultUnitFor(medType)
                        unitManuallyEdited = false
                    }
                    formDropdownExpanded = false
                    formSearchQuery = ""
                }

                Text(strings.medicineTypeLabel, style = MaterialTheme.typography.labelLarge)

                val filteredFormOptions = remember(formSearchQuery, language) {
                    val q = formSearchQuery.trim().lowercase()
                    if (q.isEmpty()) {
                        MedicineType.entries.toList()
                    } else {
                        MedicineType.entries.filter { medType ->
                            strings.medicineType(medType).lowercase().contains(q) ||
                                medType.name.lowercase().replace('_', ' ').contains(q) ||
                                strings.defaultUnitFor(medType).lowercase().contains(q)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = formDropdownExpanded,
                    onExpandedChange = { expanding ->
                        formDropdownExpanded = expanding
                        if (!expanding) formSearchQuery = ""
                    }
                ) {
                    OutlinedTextField(
                        value = if (formDropdownExpanded) formSearchQuery else strings.medicineDisplayType(type, customTypeName),
                        onValueChange = {
                            formSearchQuery = it
                            if (!formDropdownExpanded) formDropdownExpanded = true
                        },
                        placeholder = { Text(strings.searchFormPlaceholder) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (formDropdownExpanded) Icons.Rounded.Search else medicineTypeIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (formDropdownExpanded && formSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { formSearchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = null)
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = formDropdownExpanded)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                            .testTag("med_form_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = formDropdownExpanded,
                        onDismissRequest = {
                            formDropdownExpanded = false
                            formSearchQuery = ""
                        }
                    ) {
                        filteredFormOptions.forEach { medType ->
                            DropdownMenuItem(
                                text = { Text(strings.medicineType(medType)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = medicineTypeIcon(medType),
                                        contentDescription = null,
                                        tint = if (type == medType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (type == medType) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                onClick = { selectMedicineForm(medType) }
                            )
                        }
                        val trimmedSearch = formSearchQuery.trim()
                        if (trimmedSearch.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("${strings.useCustomFormPrefix} \"$trimmedSearch\"") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = medicineTypeIcon(MedicineType.OTHER),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = { selectMedicineForm(MedicineType.OTHER, trimmedSearch) }
                            )
                        }
                    }
                }

                Text(
                    text = strings.commonFormsQuickLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val primaryFiveForms = remember { MedicineType.commonEntries.take(5) }
                val visibleQuickForms = remember(showMoreFormChips) {
                    if (showMoreFormChips) {
                        MedicineType.commonEntries + MedicineType.OTHER
                    } else {
                        primaryFiveForms
                    }
                }
                FlowRow(
                    modifier = Modifier.animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    visibleQuickForms.forEach { medType ->
                        FilterChip(
                            selected = type == medType,
                            onClick = { selectMedicineForm(medType) },
                            leadingIcon = {
                                Icon(
                                    imageVector = medicineTypeIcon(medType),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(strings.medicineType(medType)) },
                            modifier = Modifier.testTag("med_type_${medType.name}")
                        )
                    }
                    FilterChip(
                        selected = showMoreFormChips,
                        onClick = { showMoreFormChips = !showMoreFormChips },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showMoreFormChips) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(if (showMoreFormChips) strings.lessFormsLabel else strings.moreFormsLabel)
                        },
                        modifier = Modifier.testTag("med_type_more_toggle")
                    )
                }

                AnimatedVisibility(visible = type == MedicineType.OTHER) {
                    OutlinedTextField(
                        value = customTypeName,
                        onValueChange = { customTypeName = it },
                        label = { Text(strings.customFormLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_custom_form_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dosageAmount,
                        onValueChange = { dosageAmount = it },
                        label = { Text(strings.dosageAmountLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_dosage_input")
                    )
                    OutlinedTextField(
                        value = dosageUnit,
                        onValueChange = { newValue ->
                            dosageUnit = newValue
                            val normalized = newValue.trim().lowercase()
                            unitManuallyEdited = normalized.isNotEmpty() && normalized !in allKnownUnits
                        },
                        label = { Text(strings.dosageUnitLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_unit_input")
                    )
                }

                val suggestedUnits = remember(type, strings) { strings.suggestedUnitsFor(type) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.quickUnitSuggestionsLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    suggestedUnits.forEach { unitOption ->
                        FilterChip(
                            selected = dosageUnit.equals(unitOption, ignoreCase = true),
                            onClick = {
                                dosageUnit = unitOption
                                unitManuallyEdited = false
                            },
                            label = { Text(unitOption, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("quick_unit_${unitOption}")
                        )
                    }
                }

                Text(strings.mealTimingLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealTiming.entries.forEach { timing ->
                        FilterChip(
                            selected = mealTiming == timing,
                            onClick = { mealTiming = timing },
                            label = { Text(strings.mealTiming(timing)) }
                        )
                    }
                }

                // Schedules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.schedulesLabel, style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = {
                            val defaultHour = if (timeSlots.isEmpty()) 8 else (timeSlots.last().hour + 6) % 24
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    if (timeSlots.none { it.hour == hour && it.minute == minute }) {
                                        timeSlots.add(TimeSlotDraft(hour, minute))
                                    }
                                },
                                defaultHour,
                                0,
                                false
                            ).show()
                        },
                        modifier = Modifier.testTag("add_time_slot_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addTimeSlot)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    timeSlots.forEachIndexed { index, slot ->
                        InputChip(
                            selected = true,
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        timeSlots[index] = TimeSlotDraft(hour, minute)
                                    },
                                    slot.hour,
                                    slot.minute,
                                    false
                                ).show()
                            },
                            label = { Text(TimeFormatter.formatTime(slot.hour, slot.minute, language)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (timeSlots.size > 1) {
                                    IconButton(
                                        onClick = { timeSlots.removeAt(index) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = strings.delete, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        )
                    }
                }

                // Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickDate(startDateEpochDay) { startDateEpochDay = it } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${strings.startDateLabel}: ${TimeFormatter.formatEpochDay(startDateEpochDay, language, "dd MMM")}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (hasEndDate) {
                        OutlinedButton(
                            onClick = { pickDate(endDateEpochDay) { endDateEpochDay = maxOf(it, startDateEpochDay) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${strings.endDateLabel}: ${TimeFormatter.formatEpochDay(endDateEpochDay, language, "dd MMM")}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = !hasEndDate,
                        onCheckedChange = { ongoing -> hasEndDate = !ongoing }
                    )
                    Text(strings.ongoingLabel, style = MaterialTheme.typography.bodySmall)
                }

                // Stock Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockCountText,
                        onValueChange = { stockCountText = it },
                        label = { Text(strings.stockCountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowStockText,
                        onValueChange = { lowStockText = it },
                        label = { Text(strings.lowStockThresholdLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = doseQtyText,
                    onValueChange = { doseQtyText = it },
                    label = { Text(strings.dosePerIntakeLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Voice Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.voiceReminderLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = voiceReminderEnabled,
                        onCheckedChange = { voiceReminderEnabled = it }
                    )
                }

                // Prescription Upload (Photo Picker & SAF PDF)
                Text(strings.prescriptionLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_rx_image_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.pickImage)
                    }
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_rx_pdf_button")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.pickPdf)
                    }
                }
                if (!prescriptionUri.isNullOrBlank()) {
                    Text(
                        text = "✓ ${strings.prescriptionLabel} attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) return@Button
                    val entity = MedicineEntity(
                        id = existing?.id ?: 0L,
                        patientId = selectedPatientId,
                        name = trimmedName,
                        type = type,
                        customTypeName = if (type == MedicineType.OTHER) {
                            customTypeName.trim().takeIf { it.isNotEmpty() }
                        } else {
                            null
                        },
                        dosageAmount = dosageAmount.ifBlank { "1" },
                        dosageUnit = dosageUnit.ifBlank { strings.defaultUnitFor(type) },
                        mealTiming = mealTiming,
                        startDateEpochDay = startDateEpochDay,
                        endDateEpochDay = if (hasEndDate) endDateEpochDay else null,
                        stockCount = stockCountText.toDoubleOrNull() ?: 30.0,
                        lowStockThreshold = lowStockText.toDoubleOrNull() ?: 5.0,
                        doseQuantityPerIntake = doseQtyText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0,
                        prescriptionUri = prescriptionUri,
                        prescriptionMimeType = prescriptionMime,
                        customSoundUri = existing?.customSoundUri,
                        voiceReminderEnabled = voiceReminderEnabled,
                        notes = notes.trim(),
                        isActive = existing?.isActive ?: true,
                        createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                    )
                    val schedules = timeSlots.distinct().map { slot ->
                        ScheduleEntity(
                            medicineId = existing?.id ?: 0L,
                            hour = slot.hour,
                            minute = slot.minute,
                            daysOfWeekMask = ScheduleEntity.ALL_DAYS_MASK,
                            isEnabled = true
                        )
                    }
                    onSave(entity, schedules, existing?.prescriptionUri)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_medicine_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
