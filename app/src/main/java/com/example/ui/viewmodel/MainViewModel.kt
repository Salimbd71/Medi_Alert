package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MediAlertApplication
import com.example.alarm.AlarmConstants
import com.example.alarm.AlarmService
import com.example.alarm.NotificationHelper
import com.example.data.local.UserPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineType
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.NextDoseInfo
import com.example.data.model.PatientEntity
import com.example.data.model.ReportPeriod
import com.example.data.model.ScheduleEntity
import com.example.data.model.ScheduledDoseItem
import com.example.data.model.ThemeMode
import com.example.domain.AdherenceCalculator
import com.example.domain.AdherenceSummary
import com.example.domain.DoseSchedulerCalculator
import com.example.domain.PinHasher
import com.example.util.PdfReportGenerator
import com.example.util.PermissionHealthState
import com.example.util.PermissionHelper
import com.example.util.PrescriptionFileHelper
import com.example.widget.MediAlertWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

data class MediAlertUiState(
    val preferences: UserPreferences = UserPreferences(),
    val patients: List<PatientEntity> = emptyList(),
    val allMedicines: List<MedicineWithSchedules> = emptyList(),
    val filteredMedicines: List<MedicineWithSchedules> = emptyList(),
    val todayDoses: List<ScheduledDoseItem> = emptyList(),
    val lowStockMedicines: List<MedicineWithSchedules> = emptyList(),
    val nextDoseInfo: NextDoseInfo? = null,
    val todayAdherencePercent: Int = 100,
    val selectedDateEpochDay: Long = DoseSchedulerCalculator.epochDayFromMillis(System.currentTimeMillis()),
    val selectedDateDoses: List<ScheduledDoseItem> = emptyList(),
    val reportPeriod: ReportPeriod = ReportPeriod.WEEKLY,
    val adherenceSummary: AdherenceSummary? = null,
    val allDoseLogs: List<DoseLogEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedMedicineTypeFilter: MedicineType? = null,
    val permissionHealth: PermissionHealthState = PermissionHealthState(true, true, true, true),
    val currentTimeMillis: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MediAlertApplication
    private val repository = app.medicationRepository
    private val prefsRepo = app.preferencesRepository
    private val alarmScheduler = app.alarmScheduler

    private val _selectedDateEpochDay = MutableStateFlow(
        DoseSchedulerCalculator.epochDayFromMillis(System.currentTimeMillis())
    )
    private val _reportPeriod = MutableStateFlow(ReportPeriod.WEEKLY)
    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<MedicineType?>(null)
    private val _permissionHealth = MutableStateFlow(PermissionHelper.checkHealth(app))

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var tts: TextToSpeech? = null

    private val secondTickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private data class CoreSnapshot(
        val prefs: UserPreferences,
        val patients: List<PatientEntity>,
        val meds: List<MedicineWithSchedules>,
        val logs: List<DoseLogEntity>
    )

    private val coreFlow = combine(
        prefsRepo.preferencesFlow,
        repository.patientsFlow,
        repository.medicinesWithSchedulesFlow,
        repository.allDoseLogsFlow
    ) { prefs, patients, meds, logs ->
        CoreSnapshot(prefs, patients, meds, logs)
    }

    private val filtersFlow = combine(
        _selectedDateEpochDay,
        _reportPeriod,
        _searchQuery,
        _typeFilter,
        _permissionHealth
    ) { dateDay, period, query, typeFilter, permHealth ->
        FilterState(dateDay, period, query, typeFilter, permHealth)
    }

    private data class FilterState(
        val selectedEpochDay: Long,
        val period: ReportPeriod,
        val query: String,
        val typeFilter: MedicineType?,
        val permissionHealth: PermissionHealthState
    )

    val uiState: StateFlow<MediAlertUiState> = combine(
        coreFlow,
        filtersFlow,
        secondTickerFlow
    ) { core, filters, nowMillis ->
        val timeZone = TimeZone.getDefault()
        val todayEpochDay = DoseSchedulerCalculator.epochDayFromMillis(nowMillis, timeZone)
        val patientsById = core.patients.associateBy { it.id }
        val activePatientId = core.prefs.activePatientId

        val logsByDay = core.logs.groupBy { it.scheduledDateEpochDay }
        val logsByScheduleAndDay = core.logs.associateBy { it.scheduleId to it.scheduledDateEpochDay }

        val todayDoses = DoseSchedulerCalculator.buildDosesForDate(
            epochDay = todayEpochDay,
            medicinesWithSchedules = core.meds,
            logsForDay = logsByDay[todayEpochDay].orEmpty(),
            patientsById = patientsById,
            activePatientId = activePatientId,
            nowMillis = nowMillis,
            autoMissedWindowMinutes = core.prefs.autoMissedWindowMinutes,
            timeZone = timeZone
        )

        val selectedDoses = if (filters.selectedEpochDay == todayEpochDay) {
            todayDoses
        } else {
            DoseSchedulerCalculator.buildDosesForDate(
                epochDay = filters.selectedEpochDay,
                medicinesWithSchedules = core.meds,
                logsForDay = logsByDay[filters.selectedEpochDay].orEmpty(),
                patientsById = patientsById,
                activePatientId = activePatientId,
                nowMillis = nowMillis,
                autoMissedWindowMinutes = core.prefs.autoMissedWindowMinutes,
                timeZone = timeZone
            )
        }

        val nextDose = DoseSchedulerCalculator.findNextUpcomingDose(
            medicinesWithSchedules = core.meds,
            logsByScheduleAndDay = logsByScheduleAndDay,
            patientsById = patientsById,
            activePatientId = activePatientId,
            nowMillis = nowMillis,
            timeZone = timeZone
        )

        val todaySummary = AdherenceCalculator.calculateSummary(
            period = ReportPeriod.DAILY,
            anchorEpochDay = todayEpochDay,
            medicinesWithSchedules = core.meds,
            allLogs = core.logs,
            patientsById = patientsById,
            activePatientId = activePatientId,
            nowMillis = nowMillis,
            autoMissedWindowMinutes = core.prefs.autoMissedWindowMinutes,
            timeZone = timeZone
        )

        val reportSummary = AdherenceCalculator.calculateSummary(
            period = filters.period,
            anchorEpochDay = todayEpochDay,
            medicinesWithSchedules = core.meds,
            allLogs = core.logs,
            patientsById = patientsById,
            activePatientId = activePatientId,
            nowMillis = nowMillis,
            autoMissedWindowMinutes = core.prefs.autoMissedWindowMinutes,
            timeZone = timeZone
        )

        val patientScopedMeds = core.meds.filter {
            activePatientId == null || it.medicine.patientId == activePatientId
        }
        val queryTrimmed = filters.query.trim()
        val filteredMeds = patientScopedMeds.filter { item ->
            val matchesType = filters.typeFilter == null || item.medicine.type == filters.typeFilter
            val matchesQuery = queryTrimmed.isEmpty() ||
                item.medicine.name.contains(queryTrimmed, ignoreCase = true) ||
                (item.patient?.name?.contains(queryTrimmed, ignoreCase = true) == true)
            matchesType && matchesQuery
        }

        val lowStockMeds = patientScopedMeds.filter {
            it.medicine.isActive && it.medicine.stockCount <= it.medicine.lowStockThreshold
        }

        MediAlertUiState(
            preferences = core.prefs,
            patients = core.patients,
            allMedicines = patientScopedMeds,
            filteredMedicines = filteredMeds,
            todayDoses = todayDoses,
            lowStockMedicines = lowStockMeds,
            nextDoseInfo = nextDose,
            todayAdherencePercent = todaySummary.adherencePercent,
            selectedDateEpochDay = filters.selectedEpochDay,
            selectedDateDoses = selectedDoses,
            reportPeriod = filters.period,
            adherenceSummary = reportSummary,
            allDoseLogs = core.logs,
            searchQuery = filters.query,
            selectedMedicineTypeFilter = filters.typeFilter,
            permissionHealth = filters.permissionHealth,
            currentTimeMillis = nowMillis
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediAlertUiState(permissionHealth = PermissionHelper.checkHealth(app))
    )

    fun refreshPermissionsHealth() {
        _permissionHealth.value = PermissionHelper.checkHealth(app)
    }

    fun selectDateEpochDay(epochDay: Long) {
        _selectedDateEpochDay.value = epochDay
    }

    fun selectReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMedicineTypeFilter(type: MedicineType?) {
        _typeFilter.value = type
    }

    fun setActivePatientId(patientId: Long?) {
        viewModelScope.launch {
            prefsRepo.setActivePatientId(patientId)
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun savePatient(patient: PatientEntity) {
        viewModelScope.launch {
            val savedId = repository.savePatient(patient)
            if (patient.id == 0L) {
                prefsRepo.setActivePatientId(savedId)
            }
        }
    }

    fun deletePatient(patient: PatientEntity) {
        viewModelScope.launch {
            repository.deletePatient(patient)
            if (uiState.value.preferences.activePatientId == patient.id) {
                prefsRepo.setActivePatientId(null)
            }
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun saveMedicine(
        medicine: MedicineEntity,
        schedules: List<ScheduleEntity>,
        previousPrescriptionPath: String? = null
    ) {
        viewModelScope.launch {
            if (previousPrescriptionPath != null && previousPrescriptionPath != medicine.prescriptionUri) {
                PrescriptionFileHelper.deleteIfInternal(previousPrescriptionPath)
            }
            repository.saveMedicineWithSchedules(medicine, schedules)
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            PrescriptionFileHelper.deleteIfInternal(medicine.prescriptionUri)
            repository.deleteMedicine(medicine)
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun toggleMedicineActive(medicineId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleMedicineActive(medicineId, isActive)
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun refillMedicine(medicineId: Long, addedAmount: Double) {
        viewModelScope.launch {
            repository.refillMedicineStock(medicineId, addedAmount)
        }
    }

    fun markDoseTaken(dose: ScheduledDoseItem) {
        viewModelScope.launch {
            stopActiveAlarmForSchedule(dose.schedule.id)
            val outcome = repository.markDoseTaken(
                medicineId = dose.medicine.id,
                scheduleId = dose.schedule.id,
                patientId = dose.patient.id,
                epochDay = dose.scheduledDateEpochDay,
                scheduledMillis = dose.scheduledTimeMillis
            )
            val prefs = prefsRepo.getPreferencesOnce()
            outcome.lowStockMedicine?.let { lowMed ->
                outcome.patient?.let { patient ->
                    NotificationHelper.showLowStockNotification(app, lowMed, patient, prefs.language)
                }
            }
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun markDoseSkipped(dose: ScheduledDoseItem) {
        viewModelScope.launch {
            stopActiveAlarmForSchedule(dose.schedule.id)
            repository.markDoseSkipped(
                medicineId = dose.medicine.id,
                scheduleId = dose.schedule.id,
                patientId = dose.patient.id,
                epochDay = dose.scheduledDateEpochDay,
                scheduledMillis = dose.scheduledTimeMillis
            )
            alarmScheduler.rescheduleAll()
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun snoozeDose(dose: ScheduledDoseItem, minutes: Int) {
        viewModelScope.launch {
            stopActiveAlarmForSchedule(dose.schedule.id)
            val log = repository.snoozeDose(
                medicineId = dose.medicine.id,
                scheduleId = dose.schedule.id,
                patientId = dose.patient.id,
                epochDay = dose.scheduledDateEpochDay,
                scheduledMillis = dose.scheduledTimeMillis,
                snoozeMinutes = minutes
            )
            val prefs = prefsRepo.getPreferencesOnce()
            val snoozeUntil = log.snoozeUntilMillis
            if (snoozeUntil != null) {
                alarmScheduler.scheduleSnoozeAlarm(
                    medicine = dose.medicine,
                    schedule = dose.schedule,
                    snoozeUntilMillis = snoozeUntil,
                    epochDay = dose.scheduledDateEpochDay,
                    autoMissedWindowMinutes = prefs.autoMissedWindowMinutes
                )
            }
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    private fun stopActiveAlarmForSchedule(scheduleId: Long) {
        app.stopService(Intent(app, AlarmService::class.java))
        NotificationHelper.cancelNotification(app, NotificationHelper.alarmNotificationId(scheduleId))
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguage(language)
            MediAlertWidgetProvider.requestWidgetUpdate(app)
        }
    }

    fun setCustomAlarmSoundUri(uri: String?) {
        viewModelScope.launch { prefsRepo.setCustomAlarmSoundUri(uri) }
    }

    fun setVoiceReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setVoiceReminderEnabled(enabled) }
    }

    fun setAutoMissedWindowMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setAutoMissedWindowMinutes(minutes)
            alarmScheduler.rescheduleAll()
        }
    }

    fun enableAppLock(pin: String, biometricEnabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.enableAppLockWithPin(pin, biometricEnabled)
            _isUnlocked.value = true
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            prefsRepo.disableAppLock()
            _isUnlocked.value = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBiometricEnabled(enabled) }
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val storedHash = uiState.value.preferences.pinHash
        val ok = PinHasher.verifyPin(pin, storedHash)
        if (ok) {
            _isUnlocked.value = true
        }
        return ok
    }

    fun unlockBiometric() {
        _isUnlocked.value = true
    }

    fun lockIfEnabled() {
        if (uiState.value.preferences.appLockEnabled) {
            _isUnlocked.value = false
        }
    }

    fun testVoiceReminder(language: AppLanguage) {
        val sampleText = if (language == AppLanguage.BANGLA) {
            "মেডি অ্যালার্ট ভয়েস রিমাইন্ডার সক্রিয় রয়েছে।"
        } else {
            "Medi Alert voice reminder is active and ready."
        }
        if (tts == null) {
            tts = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
                    val res = tts?.setLanguage(locale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "test_tts")
                }
            }
        } else {
            val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
            val res = tts?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "test_tts")
        }
    }

    fun exportBackupToUri(uri: Uri, successMessage: String, errorMessage: String) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Failed to open output stream")
                }
            }.onSuccess {
                _events.emit(successMessage)
            }.onFailure {
                _events.emit(errorMessage)
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri, successMessage: String, errorMessage: String) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Failed to read backup")
                }
                repository.restoreBackupJson(raw).getOrThrow()
                alarmScheduler.rescheduleAll()
                MediAlertWidgetProvider.requestWidgetUpdate(app)
            }.onSuccess {
                _events.emit(successMessage)
            }.onFailure {
                _events.emit(errorMessage)
            }
        }
    }

    fun exportPdfReportToUri(uri: Uri, successMessage: String, errorMessage: String) {
        viewModelScope.launch {
            val state = uiState.value
            val summary = state.adherenceSummary ?: return@launch
            val patientName = state.preferences.activePatientId?.let { pid ->
                state.patients.find { it.id == pid }?.name
            } ?: "All Patients"

            val res = PdfReportGenerator.generateReportToUri(
                context = app,
                outputUri = uri,
                patientName = patientName,
                summary = summary,
                medicines = state.allMedicines,
                dosesForSelectedDay = state.selectedDateDoses,
                selectedEpochDay = state.selectedDateEpochDay,
                language = state.preferences.language
            )
            if (res.isSuccess) {
                _events.emit(successMessage)
            } else {
                _events.emit(errorMessage)
            }
        }
    }

    fun sharePdfReport(onUriReady: (Uri) -> Unit, errorMessage: String) {
        viewModelScope.launch {
            val state = uiState.value
            val summary = state.adherenceSummary ?: return@launch
            val patientName = state.preferences.activePatientId?.let { pid ->
                state.patients.find { it.id == pid }?.name
            } ?: "All Patients"

            val res = PdfReportGenerator.generateShareableReportFile(
                context = app,
                patientName = patientName,
                summary = summary,
                medicines = state.allMedicines,
                dosesForSelectedDay = state.selectedDateDoses,
                selectedEpochDay = state.selectedDateEpochDay,
                language = state.preferences.language
            )
            res.onSuccess(onUriReady).onFailure {
                _events.emit(errorMessage)
            }
        }
    }

    override fun onCleared() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        super.onCleared()
    }
}
