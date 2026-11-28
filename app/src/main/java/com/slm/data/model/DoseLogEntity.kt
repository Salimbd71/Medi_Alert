package com.slm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["patientId"]),
        Index(value = ["scheduledDateEpochDay"]),
        Index(value = ["scheduleId", "scheduledDateEpochDay"], unique = true)
    ]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicineId: Long,
    val scheduleId: Long,
    val patientId: Long,
    val scheduledDateEpochDay: Long,
    val scheduledTimeMillis: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    val actionTimeMillis: Long? = null,
    val snoozeUntilMillis: Long? = null,
    val note: String = ""
)
