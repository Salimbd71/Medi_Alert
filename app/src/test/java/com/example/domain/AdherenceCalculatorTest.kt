package com.example.domain

import com.example.data.model.DoseLogEntity
import com.example.data.model.DoseStatus
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineWithSchedules
import com.example.data.model.PatientEntity
import com.example.data.model.ReportPeriod
import com.example.data.model.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class AdherenceCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `calculateSummary computes daily and weekly adherence percentages accurately`() {
        val today = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val yesterday = today - 1
        val patient = PatientEntity(id = 1L, name = "Self")
        val med = MedicineEntity(id = 1L, patientId = 1L, name = "Losartan", startDateEpochDay = yesterday)
        val sMorning = ScheduleEntity(id = 1L, medicineId = 1L, hour = 8, minute = 0)
        val sNight = ScheduleEntity(id = 2L, medicineId = 1L, hour = 20, minute = 0)

        val meds = listOf(MedicineWithSchedules(med, listOf(sMorning, sNight), patient))
        val logs = listOf(
            DoseLogEntity(1L, 1L, 1L, 1L, yesterday, 0L, DoseStatus.TAKEN),
            DoseLogEntity(2L, 1L, 2L, 1L, yesterday, 0L, DoseStatus.MISSED),
            DoseLogEntity(3L, 1L, 1L, 1L, today, 0L, DoseStatus.TAKEN),
            DoseLogEntity(4L, 1L, 2L, 1L, today, 0L, DoseStatus.SKIPPED)
        )

        val now = DoseSchedulerCalculator.triggerMillisForEpochDay(today, 22, 0, utc)
        val daily = AdherenceCalculator.calculateSummary(
            period = ReportPeriod.DAILY,
            anchorEpochDay = today,
            medicinesWithSchedules = meds,
            allLogs = logs,
            patientsById = mapOf(1L to patient),
            activePatientId = null,
            nowMillis = now,
            timeZone = utc
        )
        assertEquals(50, daily.adherencePercent)
        assertEquals(1, daily.takenCount)
        assertEquals(1, daily.skippedCount)

        val weekly = AdherenceCalculator.calculateSummary(
            period = ReportPeriod.WEEKLY,
            anchorEpochDay = today,
            medicinesWithSchedules = meds,
            allLogs = logs,
            patientsById = mapOf(1L to patient),
            activePatientId = null,
            nowMillis = now,
            timeZone = utc
        )
        assertEquals(7, weekly.dailyBreakdown.size)
        assertEquals(2, weekly.takenCount)
        assertEquals(1, weekly.missedCount)
        assertEquals(1, weekly.skippedCount)
        assertEquals(50, weekly.adherencePercent)
    }

    @Test
    fun `calculateDaysOfStockRemaining computes expected days`() {
        val med = MedicineEntity(
            id = 1L,
            patientId = 1L,
            name = "Paracetamol",
            startDateEpochDay = 20000L,
            stockCount = 24.0,
            doseQuantityPerIntake = 2.0
        )
        assertEquals(6, AdherenceCalculator.calculateDaysOfStockRemaining(med, enabledScheduleCount = 2))
    }
}
