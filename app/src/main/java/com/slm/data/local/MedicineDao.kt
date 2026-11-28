package com.slm.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineWithSchedules
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM medicines ORDER BY isActive DESC, createdAtMillis DESC")
    fun observeAllMedicinesWithSchedules(): Flow<List<MedicineWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getActiveMedicinesWithSchedulesOnce(): List<MedicineWithSchedules>

    @Query("SELECT * FROM medicines ORDER BY createdAtMillis DESC")
    suspend fun getAllMedicinesOnce(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineWithSchedulesById(id: Long): MedicineWithSchedules?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET stockCount = :newStock WHERE id = :medicineId")
    suspend fun updateStock(medicineId: Long, newStock: Double)

    @Query("DELETE FROM medicines")
    suspend fun deleteAllMedicines()
}
