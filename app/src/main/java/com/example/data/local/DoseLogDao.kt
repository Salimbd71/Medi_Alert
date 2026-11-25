package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY scheduledTimeMillis DESC")
    fun observeAllLogs(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledDateEpochDay = :epochDay ORDER BY scheduledTimeMillis ASC")
    fun observeLogsForDay(epochDay: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledDateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY scheduledTimeMillis ASC")
    fun observeLogsInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs")
    suspend fun getAllLogsOnce(): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE scheduledDateEpochDay = :epochDay")
    suspend fun getLogsForDayOnce(epochDay: Long): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId AND scheduledDateEpochDay = :epochDay LIMIT 1")
    suspend fun getLogForScheduleAndDay(scheduleId: Long, epochDay: Long): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DoseLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DoseLogEntity>): List<Long>

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAllLogs()
}
