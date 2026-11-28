package com.slm

import android.app.Application
import com.slm.ads.AdMobManager
import com.slm.alarm.AlarmScheduler
import com.slm.alarm.NotificationHelper
import com.slm.data.local.MediAlertDatabase
import com.slm.data.local.UserPreferencesRepository
import com.slm.data.repository.MedicationRepository
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
        AdMobManager.initialize(this)
        applicationScope.launch(Dispatchers.IO) {
            medicationRepository.ensureDefaultPatient()
            alarmScheduler.rescheduleAll()
        }
    }
}
