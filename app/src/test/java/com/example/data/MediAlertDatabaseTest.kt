package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediAlertDatabase
import com.example.data.model.MedicineEntity
import com.example.data.model.MedicineType
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduleEntity
import com.example.data.repository.MedicationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediAlertDatabaseTest {

    private lateinit var db: MediAlertDatabase
    private lateinit var repository: MedicationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediAlertDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `markDoseTaken decrements stock and triggers lowStockMedicine alert`() = runTest {
        val patient = repository.ensureDefaultPatient("Mother")
        val medId = repository.saveMedicineWithSchedules(
            medicine = MedicineEntity(
                patientId = patient.id,
                name = "Calcium",
                type = MedicineType.TABLET,
                startDateEpochDay = 20500L,
                stockCount = 6.0,
                lowStockThreshold = 5.0,
                doseQuantityPerIntake = 1.0
            ),
            schedules = listOf(ScheduleEntity(medicineId = 0L, hour = 9, minute = 0))
        )

        val schedules = db.scheduleDao().getSchedulesForMedicine(medId)
        assertEquals(1, schedules.size)

        val outcome = repository.markDoseTaken(
            medicineId = medId,
            scheduleId = schedules.first().id,
            patientId = patient.id,
            epochDay = 20500L,
            scheduledMillis = 1000L
        )

        assertNotNull(outcome.lowStockMedicine)
        assertEquals(5.0, outcome.lowStockMedicine!!.stockCount, 0.01)

        val storedMed = db.medicineDao().getMedicineById(medId)!!
        assertEquals(5.0, storedMed.stockCount, 0.01)
    }

    @Test
    fun `export and restore backup json replaces database cleanly`() = runTest {
        val pId = repository.savePatient(PatientEntity(name = "Karim", relation = "Self"))
        repository.saveMedicineWithSchedules(
            medicine = MedicineEntity(patientId = pId, name = "Omeprazole", startDateEpochDay = 20500L),
            schedules = listOf(ScheduleEntity(medicineId = 0L, hour = 7, minute = 30))
        )

        val backupJson = repository.exportBackupJson()
        db.patientDao().deleteAllPatients()
        assertTrue(repository.patientsFlow.first().isEmpty())

        val restoreResult = repository.restoreBackupJson(backupJson)
        assertTrue(restoreResult.isSuccess)
        val restoredMeds = repository.medicinesWithSchedulesFlow.first()
        assertEquals(1, restoredMeds.size)
        assertEquals("Omeprazole", restoredMeds.first().medicine.name)
    }
}
