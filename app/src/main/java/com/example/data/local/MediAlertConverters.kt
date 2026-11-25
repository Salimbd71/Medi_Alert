package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.DoseStatus
import com.example.data.model.MealTiming
import com.example.data.model.MedicineType

class MediAlertConverters {
    @TypeConverter
    fun fromMedicineType(value: MedicineType): String = value.name

    @TypeConverter
    fun toMedicineType(value: String): MedicineType =
        runCatching { MedicineType.valueOf(value) }.getOrDefault(MedicineType.TABLET)

    @TypeConverter
    fun fromMealTiming(value: MealTiming): String = value.name

    @TypeConverter
    fun toMealTiming(value: String): MealTiming =
        runCatching { MealTiming.valueOf(value) }.getOrDefault(MealTiming.AFTER_FOOD)

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus =
        runCatching { DoseStatus.valueOf(value) }.getOrDefault(DoseStatus.PENDING)
}
