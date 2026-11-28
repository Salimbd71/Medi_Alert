package com.example

import android.app.Application
import com.example.ads.AdMobManager
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.local.MediAlertDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediAlertApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: MediAlertDatabase by lazy { MediAlertDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val medicationRepository: MedicationRepository by lazy { MedicationRepository(database) }
    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(this, database, preferencesRepository)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        applicationScope.launch(Dispatchers.IO) {
            AdMobManager.initialize(this@MediAlertApplication)
            medicationRepository.ensureDefaultPatient()
            alarmScheduler.rescheduleAll()
        }
    }
}
