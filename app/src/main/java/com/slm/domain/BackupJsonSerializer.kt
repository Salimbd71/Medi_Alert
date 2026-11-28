package com.slm.domain

import com.slm.data.model.DoseLogEntity
import com.slm.data.model.DoseStatus
import com.slm.data.model.MealTiming
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineType
import com.slm.data.model.PatientEntity
import com.slm.data.model.ScheduleEntity
import org.json.JSONArray
import org.json.JSONObject

data class MediAlertBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val patients: List<PatientEntity>,
    val medicines: List<MedicineEntity>,
    val schedules: List<ScheduleEntity>,
    val doseLogs: List<DoseLogEntity>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

object BackupJsonSerializer {

    fun toJson(payload: MediAlertBackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", payload.schemaVersion)
        root.put("exportedAtMillis", payload.exportedAtMillis)

        val patientsArr = JSONArray()
        payload.patients.forEach { p ->
            patientsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("relation", p.relation)
                    if (p.age != null) put("age", p.age) else put("age", JSONObject.NULL)
                    put("avatarColorHex", p.avatarColorHex)
                    put("notes", p.notes)
                    put("isDefault", p.isDefault)
                    put("createdAtMillis", p.createdAtMillis)
                }
            )
        }
        root.put("patients", patientsArr)

        val medicinesArr = JSONArray()
        payload.medicines.forEach { m ->
            medicinesArr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("patientId", m.patientId)
                    put("name", m.name)
                    put("type", m.type.name)
                    put("customTypeName", m.customTypeName ?: JSONObject.NULL)
                    put("dosageAmount", m.dosageAmount)
                    put("dosageUnit", m.dosageUnit)
                    put("mealTiming", m.mealTiming.name)
                    put("startDateEpochDay", m.startDateEpochDay)
                    if (m.endDateEpochDay != null) put("endDateEpochDay", m.endDateEpochDay) else put("endDateEpochDay", JSONObject.NULL)
                    put("stockCount", m.stockCount)
                    put("lowStockThreshold", m.lowStockThreshold)
                    put("doseQuantityPerIntake", m.doseQuantityPerIntake)
                    put("prescriptionUri", m.prescriptionUri ?: JSONObject.NULL)
                    put("prescriptionMimeType", m.prescriptionMimeType ?: JSONObject.NULL)
                    put("customSoundUri", m.customSoundUri ?: JSONObject.NULL)
                    put("voiceReminderEnabled", m.voiceReminderEnabled)
                    put("notes", m.notes)
                    put("isActive", m.isActive)
                    put("createdAtMillis", m.createdAtMillis)
                }
            )
        }
        root.put("medicines", medicinesArr)

        val schedulesArr = JSONArray()
        payload.schedules.forEach { s ->
            schedulesArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("medicineId", s.medicineId)
                    put("hour", s.hour)
                    put("minute", s.minute)
                    put("label", s.label)
                    put("daysOfWeekMask", s.daysOfWeekMask)
                    put("isEnabled", s.isEnabled)
                }
            )
        }
        root.put("schedules", schedulesArr)

        val logsArr = JSONArray()
        payload.doseLogs.forEach { l ->
            logsArr.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("medicineId", l.medicineId)
                    put("scheduleId", l.scheduleId)
                    put("patientId", l.patientId)
                    put("scheduledDateEpochDay", l.scheduledDateEpochDay)
                    put("scheduledTimeMillis", l.scheduledTimeMillis)
                    put("status", l.status.name)
                    if (l.actionTimeMillis != null) put("actionTimeMillis", l.actionTimeMillis) else put("actionTimeMillis", JSONObject.NULL)
                    if (l.snoozeUntilMillis != null) put("snoozeUntilMillis", l.snoozeUntilMillis) else put("snoozeUntilMillis", JSONObject.NULL)
                    put("note", l.note)
                }
            )
        }
        root.put("doseLogs", logsArr)

        return root.toString(2)
    }

    fun fromJson(rawJson: String): Result<MediAlertBackupPayload> = runCatching {
        val root = JSONObject(rawJson)
        val schemaVersion = root.optInt("schemaVersion", -1)
        require(schemaVersion in 1..MediAlertBackupPayload.CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema version: $schemaVersion"
        }
        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val patients = mutableListOf<PatientEntity>()
        val patientsArr = root.getJSONArray("patients")
        for (i in 0 until patientsArr.length()) {
            val obj = patientsArr.getJSONObject(i)
            patients.add(
                PatientEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    relation = obj.optString("relation", "Self"),
                    age = if (obj.isNull("age")) null else obj.optInt("age"),
                    avatarColorHex = obj.optString("avatarColorHex", "#0A6C74"),
                    notes = obj.optString("notes", ""),
                    isDefault = obj.optBoolean("isDefault", false),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }

        val medicines = mutableListOf<MedicineEntity>()
        val medicinesArr = root.getJSONArray("medicines")
        for (i in 0 until medicinesArr.length()) {
            val obj = medicinesArr.getJSONObject(i)
            medicines.add(
                MedicineEntity(
                    id = obj.getLong("id"),
                    patientId = obj.getLong("patientId"),
                    name = obj.getString("name"),
                    type = runCatching { MedicineType.valueOf(obj.optString("type")) }.getOrDefault(MedicineType.TABLET),
                    customTypeName = if (obj.isNull("customTypeName")) null else obj.optString("customTypeName").takeIf { it.isNotBlank() },
                    dosageAmount = obj.optString("dosageAmount", "1"),
                    dosageUnit = obj.optString("dosageUnit", "Tablet"),
                    mealTiming = runCatching { MealTiming.valueOf(obj.optString("mealTiming")) }.getOrDefault(MealTiming.AFTER_FOOD),
                    startDateEpochDay = obj.getLong("startDateEpochDay"),
                    endDateEpochDay = if (obj.isNull("endDateEpochDay")) null else obj.optLong("endDateEpochDay"),
                    stockCount = obj.optDouble("stockCount", 30.0),
                    lowStockThreshold = obj.optDouble("lowStockThreshold", 5.0),
                    doseQuantityPerIntake = obj.optDouble("doseQuantityPerIntake", 1.0),
                    prescriptionUri = if (obj.isNull("prescriptionUri")) null else obj.optString("prescriptionUri"),
                    prescriptionMimeType = if (obj.isNull("prescriptionMimeType")) null else obj.optString("prescriptionMimeType"),
                    customSoundUri = if (obj.isNull("customSoundUri")) null else obj.optString("customSoundUri"),
                    voiceReminderEnabled = obj.optBoolean("voiceReminderEnabled", true),
                    notes = obj.optString("notes", ""),
                    isActive = obj.optBoolean("isActive", true),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }

        val schedules = mutableListOf<ScheduleEntity>()
        val schedulesArr = root.getJSONArray("schedules")
        for (i in 0 until schedulesArr.length()) {
            val obj = schedulesArr.getJSONObject(i)
            schedules.add(
                ScheduleEntity(
                    id = obj.getLong("id"),
                    medicineId = obj.getLong("medicineId"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    label = obj.optString("label", ""),
                    daysOfWeekMask = obj.optInt("daysOfWeekMask", ScheduleEntity.ALL_DAYS_MASK),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            )
        }

        val doseLogs = mutableListOf<DoseLogEntity>()
        val logsArr = root.getJSONArray("doseLogs")
        for (i in 0 until logsArr.length()) {
            val obj = logsArr.getJSONObject(i)
            doseLogs.add(
                DoseLogEntity(
                    id = obj.getLong("id"),
                    medicineId = obj.getLong("medicineId"),
                    scheduleId = obj.getLong("scheduleId"),
                    patientId = obj.getLong("patientId"),
                    scheduledDateEpochDay = obj.getLong("scheduledDateEpochDay"),
                    scheduledTimeMillis = obj.getLong("scheduledTimeMillis"),
                    status = runCatching { DoseStatus.valueOf(obj.optString("status")) }.getOrDefault(DoseStatus.PENDING),
                    actionTimeMillis = if (obj.isNull("actionTimeMillis")) null else obj.optLong("actionTimeMillis"),
                    snoozeUntilMillis = if (obj.isNull("snoozeUntilMillis")) null else obj.optLong("snoozeUntilMillis"),
                    note = obj.optString("note", "")
                )
            )
        }

        MediAlertBackupPayload(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAtMillis,
            patients = patients,
            medicines = medicines,
            schedules = schedules,
            doseLogs = doseLogs
        )
    }
}
