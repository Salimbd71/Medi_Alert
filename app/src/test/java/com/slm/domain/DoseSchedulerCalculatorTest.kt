package com.slm.domain

import com.slm.data.model.DoseLogEntity
import com.slm.data.model.DoseStatus
import com.slm.data.model.MealTiming
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineType
import com.slm.data.model.MedicineWithSchedules
import com.slm.data.model.PatientEntity
import com.slm.data.model.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DoseSchedulerCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `toEpochDay and yearMonthDayFromEpochDay round trip cleanly`() {
        val day = DoseSchedulerCalculator.toEpochDay(2026, 10, 25) // Nov 25, 2026
        val (y, m, d) = DoseSchedulerCalculator.yearMonthDayFromEpochDay(day)
        assertEquals(2026, y)
        assertEquals(10, m)
        assertEquals(25, d)
    }

    @Test
    fun `computeNextTriggerMillis returns future dose today or tomorrow`() {
        val startDay = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val med = MedicineEntity(
            id = 1L,
            patientId = 1L,
            name = "Napa",
            type = MedicineType.TABLET,
            startDateEpochDay = startDay
        )
        val schedule = ScheduleEntity(id = 10L, medicineId = 1L, hour = 9, minute = 0)

        val morning8am = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay, 8, 0, utc)
        val nextToday = DoseSchedulerCalculator.computeNextTriggerMillis(
            medicine = med,
            schedule = schedule,
            nowMillis = morning8am,
            timeZone = utc
        )
        val expected9am = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay, 9, 0, utc)
        assertEquals(expected9am, nextToday)

        val afternoon2pm = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay, 14, 0, utc)
        val nextTomorrow = DoseSchedulerCalculator.computeNextTriggerMillis(
            medicine = med,
            schedule = schedule,
            nowMillis = afternoon2pm,
            timeZone = utc
        )
        val expectedTomorrow9am = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay + 1, 9, 0, utc)
        assertEquals(expectedTomorrow9am, nextTomorrow)
    }

    @Test
    fun `computeNextTriggerMillis respects endDateEpochDay`() {
        val startDay = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val med = MedicineEntity(
            id = 1L,
            patientId = 1L,
            name = "Azithromycin",
            startDateEpochDay = startDay,
            endDateEpochDay = startDay
        )
        val schedule = ScheduleEntity(id = 11L, medicineId = 1L, hour = 8, minute = 0)
        val afterDose = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay, 10, 0, utc)

        val result = DoseSchedulerCalculator.computeNextTriggerMillis(
            medicine = med,
            schedule = schedule,
            nowMillis = afterDose,
            timeZone = utc
        )
        assertNull(result)
    }

    @Test
    fun `computeNextTriggerMillis prioritizes active snooze`() {
        val startDay = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val med = MedicineEntity(id = 1L, patientId = 1L, name = "Metformin", startDateEpochDay = startDay)
        val schedule = ScheduleEntity(id = 12L, medicineId = 1L, hour = 8, minute = 0)
        val now = DoseSchedulerCalculator.triggerMillisForEpochDay(startDay, 8, 5, utc)
        val snoozeUntil = now + 10 * 60_000L

        val next = DoseSchedulerCalculator.computeNextTriggerMillis(
            medicine = med,
            schedule = schedule,
            nowMillis = now,
            snoozeUntilMillis = snoozeUntil,
            timeZone = utc
        )
        assertEquals(snoozeUntil, next)
    }

    @Test
    fun `findNextUpcomingDose returns closest pending dose across patients`() {
        val day = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val patient = PatientEntity(id = 1L, name = "Rahim")
        val med1 = MedicineEntity(id = 1L, patientId = 1L, name = "Med A", startDateEpochDay = day)
        val med2 = MedicineEntity(id = 2L, patientId = 1L, name = "Med B", startDateEpochDay = day, mealTiming = MealTiming.BEFORE_FOOD)
        val sched1 = ScheduleEntity(id = 101L, medicineId = 1L, hour = 20, minute = 0)
        val sched2 = ScheduleEntity(id = 102L, medicineId = 2L, hour = 14, minute = 30)

        val list = listOf(
            MedicineWithSchedules(med1, listOf(sched1), patient),
            MedicineWithSchedules(med2, listOf(sched2), patient)
        )
        val now = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 12, 0, utc)
        val next = DoseSchedulerCalculator.findNextUpcomingDose(
            medicinesWithSchedules = list,
            logsByScheduleAndDay = emptyMap(),
            patientsById = mapOf(1L to patient),
            activePatientId = null,
            nowMillis = now,
            timeZone = utc
        )

        assertNotNull(next)
        assertEquals("Med B", next!!.item.medicine.name)
        assertEquals(150 * 60_000L, next.millisRemaining)
    }

    @Test
    fun `buildDosesForDate marks overdue pending doses as missed`() {
        val day = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val patient = PatientEntity(id = 1L, name = "Self")
        val med = MedicineEntity(id = 1L, patientId = 1L, name = "Vitamin D", startDateEpochDay = day)
        val s1 = ScheduleEntity(id = 1L, medicineId = 1L, hour = 8, minute = 0)
        val s2 = ScheduleEntity(id = 2L, medicineId = 1L, hour = 21, minute = 0)

        val now = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 12, 0, utc)
        val doses = DoseSchedulerCalculator.buildDosesForDate(
            epochDay = day,
            medicinesWithSchedules = listOf(MedicineWithSchedules(med, listOf(s1, s2), patient)),
            logsForDay = emptyList<DoseLogEntity>(),
            patientsById = mapOf(1L to patient),
            activePatientId = null,
            nowMillis = now,
            autoMissedWindowMinutes = 30,
            timeZone = utc
        )

        assertEquals(2, doses.size)
        assertEquals(DoseStatus.MISSED, doses[0].status)
        assertEquals(DoseStatus.PENDING, doses[1].status)
    }

    @Test
    fun `request codes for dose, snooze, and missed check never collide`() {
        val scheduleId = 42L
        val doseCode = DoseSchedulerCalculator.doseAlarmRequestCode(scheduleId)
        val missedCode = DoseSchedulerCalculator.missedCheckRequestCode(scheduleId)
        val snoozeCode = DoseSchedulerCalculator.snoozeAlarmRequestCode(scheduleId)

        assertNotEquals(doseCode, missedCode)
        assertNotEquals(doseCode, snoozeCode)
        assertNotEquals(missedCode, snoozeCode)
        assertTrue(doseCode > 0)
    }

    @Test
    fun `isActionWindowOpen unlocks only 15 minutes before pending dose`() {
        val day = DoseSchedulerCalculator.toEpochDay(2026, 10, 25)
        val patient = PatientEntity(id = 1L, name = "Self")
        val med = MedicineEntity(id = 1L, patientId = 1L, name = "Napa", startDateEpochDay = day)
        val sched = ScheduleEntity(id = 1L, medicineId = 1L, hour = 23, minute = 0)
        val trigger = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 23, 0, utc)
        val pendingDose = com.slm.data.model.ScheduledDoseItem(
            schedule = sched,
            medicine = med,
            patient = patient,
            doseLog = null,
            scheduledDateEpochDay = day,
            scheduledTimeMillis = trigger,
            effectiveTriggerMillis = trigger,
            status = DoseStatus.PENDING
        )

        val eightPm = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 20, 0, utc)
        val tenFortyFourPm = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 22, 44, utc)
        val tenFortyFivePm = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 22, 45, utc)
        val elevenTenPm = DoseSchedulerCalculator.triggerMillisForEpochDay(day, 23, 10, utc)

        assertEquals(false, DoseSchedulerCalculator.isActionWindowOpen(pendingDose, eightPm))
        assertEquals(false, DoseSchedulerCalculator.isActionWindowOpen(pendingDose, tenFortyFourPm))
        assertEquals(true, DoseSchedulerCalculator.isActionWindowOpen(pendingDose, tenFortyFivePm))
        assertEquals(true, DoseSchedulerCalculator.isActionWindowOpen(pendingDose, elevenTenPm))
        assertEquals(
            true,
            DoseSchedulerCalculator.isActionWindowOpen(pendingDose.copy(status = DoseStatus.MISSED), elevenTenPm)
        )
        assertEquals(
            false,
            DoseSchedulerCalculator.isActionWindowOpen(pendingDose.copy(status = DoseStatus.TAKEN), elevenTenPm)
        )
    }
}
