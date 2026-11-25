package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineType
import com.example.data.model.MedicineWithSchedules
import com.example.domain.AdherenceCalculator
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldTaken
import com.example.ui.theme.GlassCard
import com.example.ui.theme.StatusPill
import com.example.ui.viewmodel.MediAlertUiState
import com.example.util.PrescriptionFileHelper
import com.example.util.TimeFormatter
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicinesScreen(
    uiState: MediAlertUiState,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (MedicineType?) -> Unit,
    onAddMedicineClick: () -> Unit,
    onEditMedicineClick: (MedicineWithSchedules) -> Unit,
    onDeleteMedicine: (MedicineEntity) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onRefillClick: (MedicineEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    val language = uiState.preferences.language
    var previewMedicine by remember { mutableStateOf<MedicineEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("medicines_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.searchMedicinesHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedMedicineTypeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text(strings.allTypes) },
                        modifier = Modifier.testTag("filter_type_all")
                    )
                    MedicineType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedMedicineTypeFilter == type,
                            onClick = {
                                onTypeFilterChange(
                                    if (uiState.selectedMedicineTypeFilter == type) null else type
                                )
                            },
                            label = { Text(strings.medicineType(type)) },
                            modifier = Modifier.testTag("filter_type_${type.name}")
                        )
                    }
                }
            }
        }

        if (uiState.filteredMedicines.isEmpty()) {
            item(key = "empty_meds") {
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
                            contentDescription = strings.emptyMedicinesTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Text(
                            text = strings.emptyMedicinesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.emptyMedicinesBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddMedicineClick,
                            modifier = Modifier.testTag("empty_medicines_add_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addMedicine)
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredMedicines,
                key = { "med_${it.medicine.id}" }
            ) { item ->
                val med = item.medicine
                val enabledCount = item.schedules.count { it.isEnabled }
                val daysLeft = AdherenceCalculator.calculateDaysOfStockRemaining(med, enabledCount)
                val isLow = med.stockCount <= med.lowStockThreshold

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_card_${med.id}"),
                    highlightColor = if (isLow) CoralAlert else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = medicineTypeIcon(med.type),
                                    contentDescription = strings.medicineType(med.type),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = med.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${med.dosageAmount} ${med.dosageUnit} • ${strings.medicineType(med.type)} • ${
                                        strings.mealTiming(med.mealTiming)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.patient?.let { patient ->
                                    Text(
                                        text = "${patient.name} (${patient.relation})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Switch(
                                checked = med.isActive,
                                onCheckedChange = { onToggleActive(med.id, it) },
                                modifier = Modifier.testTag("toggle_active_${med.id}")
                            )
                        }

                        // Daily schedule pills
                        if (item.schedules.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.schedules.forEach { schedule ->
                                    StatusPill(
                                        text = TimeFormatter.formatTime(schedule.hour, schedule.minute, language),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Stock Bar
                        val stockRatio = (med.stockCount / (med.lowStockThreshold * 4.0).coerceAtLeast(20.0))
                            .toFloat()
                            .coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.remainingStock}: ${
                                        TimeFormatter.localizeDigits(
                                            TimeFormatter.formatNumber(med.stockCount),
                                            language
                                        )
                                    } ${med.dosageUnit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isLow) CoralAlert else MaterialTheme.colorScheme.onSurface
                                )
                                if (daysLeft > 0) {
                                    Text(
                                        text = "${
                                            TimeFormatter.localizeDigits(daysLeft.toString(), language)
                                        } ${strings.daysLeftSuffix}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { stockRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    isLow -> CoralAlert
                                    stockRatio < 0.4f -> AmberWarning
                                    else -> EmeraldTaken
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onRefillClick(med) },
                                modifier = Modifier.testTag("refill_med_${med.id}"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.lowStockAction, style = MaterialTheme.typography.labelMedium)
                            }

                            if (!med.prescriptionUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { previewMedicine = med },
                                    modifier = Modifier.testTag("view_rx_${med.id}"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.viewPrescription, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(
                                onClick = { onEditMedicineClick(item) },
                                modifier = Modifier.testTag("edit_med_${med.id}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.editMedicine)
                            }
                            IconButton(
                                onClick = { onDeleteMedicine(med) },
                                modifier = Modifier.testTag("delete_med_${med.id}")
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

    previewMedicine?.let { med ->
        PrescriptionViewerDialog(
            medicine = med,
            onDismiss = { previewMedicine = null }
        )
    }
}

@Composable
fun RefillStockDialog(
    medicine: MedicineEntity,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmRefill: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var amountText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.refillTitle}: ${medicine.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${strings.remainingStock}: ${
                        TimeFormatter.localizeDigits(
                            TimeFormatter.formatNumber(medicine.stockCount),
                            language
                        )
                    } ${medicine.dosageUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.refillAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refill_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = amountText.toDoubleOrNull() ?: 0.0
                    if (qty > 0.0) {
                        onConfirmRefill(qty)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_refill_button")
            ) {
                Text(strings.refillButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun PrescriptionViewerDialog(
    medicine: MedicineEntity,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val path = medicine.prescriptionUri.orEmpty()
    val isPdf = (medicine.prescriptionMimeType?.contains("pdf", ignoreCase = true) == true) ||
        path.endsWith(".pdf", ignoreCase = true)

    var pdfPages by remember(path) { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(path, isPdf) {
        if (isPdf && path.isNotBlank()) {
            pdfPages = PrescriptionFileHelper.renderPdfPages(path)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Text("${medicine.name} — ${strings.prescriptionLabel}")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPdf) {
                    if (pdfPages.isEmpty()) {
                        Text(
                            text = "PDF attached (${File(path).name})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        pdfPages.forEachIndexed { idx, bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Prescription page ${idx + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = File(path),
                        contentDescription = strings.prescriptionLabel,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
