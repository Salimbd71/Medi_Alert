package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour ASC, minute ASC")
    fun observeAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedulesOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY hour ASC, minute ASC")
    suspend fun getSchedulesForMedicine(medicineId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): List<Long>

    @Query("DELETE FROM schedules WHERE medicineId = :medicineId")
    suspend fun deleteSchedulesForMedicine(medicineId: Long)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
}
