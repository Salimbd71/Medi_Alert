package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalLayoutApi::class)
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
    var dosageAmount by remember { mutableStateOf(existing?.dosageAmount ?: "1") }
    var dosageUnit by remember { mutableStateOf(existing?.dosageUnit ?: "Tablet") }
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

                Text(strings.medicineTypeLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MedicineType.entries.forEach { medType ->
                        FilterChip(
                            selected = type == medType,
                            onClick = {
                                type = medType
                                if (existing == null) {
                                    dosageUnit = when (medType) {
                                        MedicineType.TABLET -> "Tablet"
                                        MedicineType.CAPSULE -> "Capsule"
                                        MedicineType.SYRUP -> "ml"
                                        MedicineType.INJECTION -> "IU"
                                    }
                                }
                            },
                            label = { Text(strings.medicineType(medType)) },
                            modifier = Modifier.testTag("med_type_${medType.name}")
                        )
                    }
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
                        onValueChange = { dosageUnit = it },
                        label = { Text(strings.dosageUnitLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_unit_input")
                    )
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
                        dosageAmount = dosageAmount.ifBlank { "1" },
                        dosageUnit = dosageUnit.ifBlank { "Tablet" },
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
