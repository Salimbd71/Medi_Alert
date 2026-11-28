package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicineEntity
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduleEntity

@Database(
    entities = [
        PatientEntity::class,
        MedicineEntity::class,
        ScheduleEntity::class,
        DoseLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(MediAlertConverters::class)
abstract class MediAlertDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var INSTANCE: MediAlertDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN customTypeName TEXT")
            }
        }

        fun getInstance(context: Context): MediAlertDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediAlertDatabase::class.java,
                    "medi_alert.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
