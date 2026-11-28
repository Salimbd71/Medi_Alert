package com.slm.data.model

data class ScheduledDoseItem(
    val schedule: ScheduleEntity,
    val medicine: MedicineEntity,
    val patient: PatientEntity,
    val doseLog: DoseLogEntity?,
    val scheduledDateEpochDay: Long,
    val scheduledTimeMillis: Long,
    val effectiveTriggerMillis: Long,
    val status: DoseStatus
) {
    val isLowStock: Boolean
        get() = medicine.stockCount <= medicine.lowStockThreshold

    val bucket: TimeOfDayBucket
        get() = when (schedule.hour) {
            in 5..11 -> TimeOfDayBucket.MORNING
            in 12..16 -> TimeOfDayBucket.AFTERNOON
            in 17..20 -> TimeOfDayBucket.EVENING
            else -> TimeOfDayBucket.NIGHT
        }
}

data class NextDoseInfo(
    val item: ScheduledDoseItem,
    val millisRemaining: Long
)
