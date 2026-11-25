package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY isDefault DESC, createdAtMillis ASC")
    fun observeAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY isDefault DESC, createdAtMillis ASC")
    suspend fun getAllPatientsOnce(): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("DELETE FROM patients")
    suspend fun deleteAllPatients()

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun countPatients(): Int
}
