package com.slm.domain

import com.slm.data.model.DoseLogEntity
import com.slm.data.model.DoseStatus
import com.slm.data.model.MealTiming
import com.slm.data.model.MedicineEntity
import com.slm.data.model.MedicineType
import com.slm.data.model.PatientEntity
import com.slm.data.model.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupJsonSerializerTest {

    @Test
    fun `toJson and fromJson preserve complete medication backup payload`() {
        val payload = MediAlertBackupPayload(
            patients = listOf(PatientEntity(id = 1L, name = "Rahim", relation = "Father", age = 62)),
            medicines = listOf(
                MedicineEntity(
                    id = 5L,
                    patientId = 1L,
                    name = "Atorvastatin",
                    type = MedicineType.TABLET,
                    dosageAmount = "10",
                    dosageUnit = "mg",
                    mealTiming = MealTiming.AFTER_FOOD,
                    startDateEpochDay = 20500L,
                    endDateEpochDay = 20530L,
                    stockCount = 28.0,
                    lowStockThreshold = 5.0
                )
            ),
            schedules = listOf(ScheduleEntity(id = 9L, medicineId = 5L, hour = 21, minute = 30)),
            doseLogs = listOf(
                DoseLogEntity(
                    id = 100L,
                    medicineId = 5L,
                    scheduleId = 9L,
                    patientId = 1L,
                    scheduledDateEpochDay = 20500L,
                    scheduledTimeMillis = 123456789L,
                    status = DoseStatus.TAKEN,
                    actionTimeMillis = 123459999L
                )
            )
        )

        val json = BackupJsonSerializer.toJson(payload)
        val restored = BackupJsonSerializer.fromJson(json)
        assertTrue(restored.isSuccess)
        val data = restored.getOrThrow()
        assertEquals(1, data.patients.size)
        assertEquals("Rahim", data.patients.first().name)
        assertEquals("Atorvastatin", data.medicines.first().name)
        assertEquals(MedicineType.TABLET, data.medicines.first().type)
        assertEquals(21, data.schedules.first().hour)
        assertEquals(DoseStatus.TAKEN, data.doseLogs.first().status)
    }

    @Test
    fun `fromJson fails gracefully on invalid schema`() {
        val result = BackupJsonSerializer.fromJson("""{"schemaVersion": 99}""")
        assertTrue(result.isFailure)
    }
}
