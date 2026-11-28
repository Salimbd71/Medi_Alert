package com.slm.data.repository

import androidx.room.withTransaction
import com.slm.data.local.MediAlertDatabase
import com.slm.data.model.DoseLogEntity
import com.slm.data.model.DoseStatus
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineWithSchedules
import com.slm.data.model.PatientEntity
import com.slm.data.model.ScheduleEntity
import com.slm.domain.BackupJsonSerializer
import com.slm.domain.MediAlertBackupPayload
import kotlinx.coroutines.flow.Flow

data class DoseActionOutcome(
    val updatedLog: DoseLogEntity,
    val lowStockMedicine: MedicineEntity? = null,
    val patient: PatientEntity? = null
)

class MedicationRepository(private val database: MediAlertDatabase) {

    private val patientDao = database.patientDao()
    private val medicineDao = database.medicineDao()
    private val scheduleDao = database.scheduleDao()
    private val doseLogDao = database.doseLogDao()

    val patientsFlow: Flow<List<PatientEntity>> = patientDao.observeAllPatients()
    val medicinesWithSchedulesFlow: Flow<List<MedicineWithSchedules>> =
        medicineDao.observeAllMedicinesWithSchedules()
    val allDoseLogsFlow: Flow<List<DoseLogEntity>> = doseLogDao.observeAllLogs()

    suspend fun ensureDefaultPatient(defaultName: String = "Self"): PatientEntity {
        val existing = patientDao.getAllPatientsOnce()
        if (existing.isNotEmpty()) return existing.first()
        val id = patientDao.insertPatient(
            PatientEntity(
                name = defaultName,
                relation = "Self",
                avatarColorHex = "#0A6C74",
                isDefault = true
            )
        )
        return patientDao.getPatientById(id)!!
    }

    suspend fun savePatient(patient: PatientEntity): Long {
        return if (patient.id == 0L) {
            patientDao.insertPatient(patient)
        } else {
            patientDao.updatePatient(patient)
            patient.id
        }
    }

    suspend fun deletePatient(patient: PatientEntity) {
        if (patientDao.countPatients() <= 1) return
        patientDao.deletePatient(patient)
    }

    suspend fun saveMedicineWithSchedules(
        medicine: MedicineEntity,
        schedules: List<ScheduleEntity>
    ): Long {
        return database.withTransaction {
            val savedId = if (medicine.id == 0L) {
                medicineDao.insertMedicine(medicine)
            } else {
                medicineDao.updateMedicine(medicine)
                medicine.id
            }
            scheduleDao.deleteSchedulesForMedicine(savedId)
            val normalizedSchedules = schedules.map {
                it.copy(id = 0L, medicineId = savedId)
            }
            if (normalizedSchedules.isNotEmpty()) {
                scheduleDao.insertSchedules(normalizedSchedules)
            }
            savedId
        }
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun refillMedicineStock(medicineId: Long, addedQuantity: Double) {
        if (addedQuantity <= 0.0) return
        database.withTransaction {
            val med = medicineDao.getMedicineById(medicineId) ?: return@withTransaction
            val updated = (med.stockCount + addedQuantity).coerceAtLeast(0.0)
            medicineDao.updateStock(medicineId, updated)
        }
    }

    suspend fun toggleMedicineActive(medicineId: Long, isActive: Boolean) {
        val med = medicineDao.getMedicineById(medicineId) ?: return
        medicineDao.updateMedicine(med.copy(isActive = isActive))
    }

    suspend fun markDoseTaken(
        medicineId: Long,
        scheduleId: Long,
        patientId: Long,
        epochDay: Long,
        scheduledMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): DoseActionOutcome {
        return database.withTransaction {
            val existing = doseLogDao.getLogForScheduleAndDay(scheduleId, epochDay)
            val wasAlreadyTaken = existing?.status == DoseStatus.TAKEN

            val log = DoseLogEntity(
                id = existing?.id ?: 0L,
                medicineId = medicineId,
                scheduleId = scheduleId,
                patientId = patientId,
                scheduledDateEpochDay = epochDay,
                scheduledTimeMillis = scheduledMillis,
                status = DoseStatus.TAKEN,
                actionTimeMillis = nowMillis,
                snoozeUntilMillis = null
            )
            val savedLogId = doseLogDao.upsertLog(log)
            val finalLog = log.copy(id = if (log.id == 0L) savedLogId else log.id)

            var lowStockMed: MedicineEntity? = null
            if (!wasAlreadyTaken) {
                val med = medicineDao.getMedicineById(medicineId)
                if (med != null) {
                    val newStock = (med.stockCount - med.doseQuantityPerIntake).coerceAtLeast(0.0)
                    medicineDao.updateStock(medicineId, newStock)
                    val updatedMed = med.copy(stockCount = newStock)
                    if (newStock <= updatedMed.lowStockThreshold) {
                        lowStockMed = updatedMed
                    }
                }
            }
            val patient = patientDao.getPatientById(patientId)
            DoseActionOutcome(updatedLog = finalLog, lowStockMedicine = lowStockMed, patient = patient)
        }
    }

    suspend fun markDoseSkipped(
        medicineId: Long,
        scheduleId: Long,
        patientId: Long,
        epochDay: Long,
        scheduledMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): DoseLogEntity {
        val existing = doseLogDao.getLogForScheduleAndDay(scheduleId, epochDay)
        val log = DoseLogEntity(
            id = existing?.id ?: 0L,
            medicineId = medicineId,
            scheduleId = scheduleId,
            patientId = patientId,
            scheduledDateEpochDay = epochDay,
            scheduledTimeMillis = scheduledMillis,
            status = DoseStatus.SKIPPED,
            actionTimeMillis = nowMillis,
            snoozeUntilMillis = null
        )
        val id = doseLogDao.upsertLog(log)
        return log.copy(id = if (log.id == 0L) id else log.id)
    }

    suspend fun markDoseMissedIfPending(
        medicineId: Long,
        scheduleId: Long,
        patientId: Long,
        epochDay: Long,
        scheduledMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): DoseLogEntity? {
        val existing = doseLogDao.getLogForScheduleAndDay(scheduleId, epochDay)
        if (existing != null && (existing.status == DoseStatus.TAKEN || existing.status == DoseStatus.SKIPPED || existing.status == DoseStatus.MISSED)) {
            return null
        }
        val log = DoseLogEntity(
            id = existing?.id ?: 0L,
            medicineId = medicineId,
            scheduleId = scheduleId,
            patientId = patientId,
            scheduledDateEpochDay = epochDay,
            scheduledTimeMillis = scheduledMillis,
            status = DoseStatus.MISSED,
            actionTimeMillis = nowMillis,
            snoozeUntilMillis = null
        )
        val id = doseLogDao.upsertLog(log)
        return log.copy(id = if (log.id == 0L) id else log.id)
    }

    suspend fun snoozeDose(
        medicineId: Long,
        scheduleId: Long,
        patientId: Long,
        epochDay: Long,
        scheduledMillis: Long,
        snoozeMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): DoseLogEntity {
        val existing = doseLogDao.getLogForScheduleAndDay(scheduleId, epochDay)
        val snoozeUntil = nowMillis + snoozeMinutes * 60_000L
        val log = DoseLogEntity(
            id = existing?.id ?: 0L,
            medicineId = medicineId,
            scheduleId = scheduleId,
            patientId = patientId,
            scheduledDateEpochDay = epochDay,
            scheduledTimeMillis = scheduledMillis,
            status = DoseStatus.SNOOZED,
            actionTimeMillis = nowMillis,
            snoozeUntilMillis = snoozeUntil
        )
        val id = doseLogDao.upsertLog(log)
        return log.copy(id = if (log.id == 0L) id else log.id)
    }

    suspend fun exportBackupJson(): String {
        val payload = MediAlertBackupPayload(
            patients = patientDao.getAllPatientsOnce(),
            medicines = medicineDao.getAllMedicinesOnce(),
            schedules = scheduleDao.getAllSchedulesOnce(),
            doseLogs = doseLogDao.getAllLogsOnce()
        )
        return BackupJsonSerializer.toJson(payload)
    }

    suspend fun restoreBackupJson(rawJson: String): Result<MediAlertBackupPayload> {
        val parsed = BackupJsonSerializer.fromJson(rawJson)
        val payload = parsed.getOrElse { return Result.failure(it) }
        return runCatching {
            database.withTransaction {
                doseLogDao.deleteAllLogs()
                scheduleDao.deleteAllSchedules()
                medicineDao.deleteAllMedicines()
                patientDao.deleteAllPatients()

                payload.patients.forEach { patientDao.insertPatient(it) }
                payload.medicines.forEach { medicineDao.insertMedicine(it) }
                if (payload.schedules.isNotEmpty()) {
                    scheduleDao.insertSchedules(payload.schedules)
                }
                if (payload.doseLogs.isNotEmpty()) {
                    doseLogDao.insertLogs(payload.doseLogs)
                }
                if (payload.patients.isEmpty()) {
                    ensureDefaultPatient()
                }
            }
            payload
        }
    }
}
