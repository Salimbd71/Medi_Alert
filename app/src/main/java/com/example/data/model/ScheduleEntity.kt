package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicineId"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicineId: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val daysOfWeekMask: Int = ALL_DAYS_MASK,
    val isEnabled: Boolean = true
) {
    companion object {
        const val ALL_DAYS_MASK = 0b1111111 // Bit 0 = Monday ... Bit 6 = Sunday
    }
}
