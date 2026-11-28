package com.slm.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithSchedules(
    @Embedded val medicine: MedicineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicineId"
    )
    val schedules: List<ScheduleEntity>,
    @Relation(
        parentColumn = "patientId",
        entityColumn = "id"
    )
    val patient: PatientEntity?
)
