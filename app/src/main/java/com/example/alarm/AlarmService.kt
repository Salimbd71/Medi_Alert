package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.data.local.MediAlertDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.MedicineEntity
import com.example.data.model.PatientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingVoicePrompt: String? = null
    private var voiceLanguage: AppLanguage = AppLanguage.ENGLISH
    private var autoSilenceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AlarmConstants.ACTION_STOP_SERVICE) {
            stopAlarmPlayback()
            stopSelf()
            return START_NOT_STICKY
        }

        val scheduleId = intent?.getLongExtra(AlarmConstants.EXTRA_SCHEDULE_ID, -1L) ?: -1L
        val medicineId = intent?.getLongExtra(AlarmConstants.EXTRA_MEDICINE_ID, -1L) ?: -1L
        val patientId = intent?.getLongExtra(AlarmConstants.EXTRA_PATIENT_ID, -1L) ?: -1L
        val epochDay = intent?.getLongExtra(AlarmConstants.EXTRA_EPOCH_DAY, 0L) ?: 0L
        val scheduledMillis = intent?.getLongExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, System.currentTimeMillis())
            ?: System.currentTimeMillis()

        if (scheduleId <= 0L || medicineId <= 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch(Dispatchers.IO) {
            val db = MediAlertDatabase.getInstance(applicationContext)
            val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
            val medicine = db.medicineDao().getMedicineById(medicineId)
            val schedule = db.scheduleDao().getScheduleById(scheduleId)
            val patient = db.patientDao().getPatientById(patientId)
                ?: db.patientDao().getAllPatientsOnce().firstOrNull()

            if (medicine == null || schedule == null || patient == null) {
                stopSelf()
                return@launch
            }

            val notification = NotificationHelper.buildAlarmForegroundNotification(
                context = applicationContext,
                scheduleId = scheduleId,
                medicine = medicine,
                patient = patient,
                schedule = schedule,
                epochDay = epochDay,
                scheduledMillis = scheduledMillis,
                language = prefs.language
            )

            launch(Dispatchers.Main) {
                startForeground(NotificationHelper.alarmNotificationId(scheduleId), notification)
                startSoundAndVibration(medicine.customSoundUri ?: prefs.customAlarmSoundUri)

                if (prefs.voiceReminderEnabled && medicine.voiceReminderEnabled) {
                    voiceLanguage = prefs.language
                    val prompt = buildVoicePrompt(medicine, patient, prefs.language)
                    pendingVoicePrompt = prompt
                    if (ttsReady) {
                        speakPrompt(prompt, prefs.language)
                    }
                }

                // Auto-stop ringing after 90 seconds while leaving notification visible
                autoSilenceJob?.cancel()
                autoSilenceJob = serviceScope.launch {
                    delay(90_000L)
                    stopAlarmPlayback()
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            pendingVoicePrompt?.let { prompt ->
                speakPrompt(prompt, voiceLanguage)
            }
        }
    }

    private fun speakPrompt(text: String, language: AppLanguage) {
        val engine = tts ?: return
        val targetLocale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.ENGLISH)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medi_alert_voice")
    }

    private fun buildVoicePrompt(
        medicine: MedicineEntity,
        patient: PatientEntity,
        language: AppLanguage
    ): String {
        val meal = NotificationHelper.formatMealTiming(medicine.mealTiming, language)
        return if (language == AppLanguage.BANGLA) {
            "${patient.name}, আপনার ${medicine.name}, ${medicine.dosageAmount} ${medicine.dosageUnit}, $meal নেওয়ার সময় হয়েছে।"
        } else {
            "Reminder for ${patient.name}. Time to take ${medicine.name}, ${medicine.dosageAmount} ${medicine.dosageUnit}, $meal."
        }
    }

    private fun startSoundAndVibration(customSoundUri: String?) {
        stopAlarmPlayback()

        val soundUri: Uri = customSoundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, soundUri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            // Fallback to default notification sound if custom URI fails
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib

        val pattern = longArrayOf(0, 600, 400, 600, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmPlayback() {
        autoSilenceJob?.cancel()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmPlayback()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
