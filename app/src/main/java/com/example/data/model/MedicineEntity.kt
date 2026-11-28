package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patientId"])]
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val patientId: Long,
    val name: String,
    val type: MedicineType = MedicineType.TABLET,
    val customTypeName: String? = null,
    val dosageAmount: String = "1",
    val dosageUnit: String = "Tablet",
    val mealTiming: MealTiming = MealTiming.AFTER_FOOD,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val stockCount: Double = 30.0,
    val lowStockThreshold: Double = 5.0,
    val doseQuantityPerIntake: Double = 1.0,
    val prescriptionUri: String? = null,
    val prescriptionMimeType: String? = null,
    val customSoundUri: String? = null,
    val voiceReminderEnabled: Boolean = true,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)
