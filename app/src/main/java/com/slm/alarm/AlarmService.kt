package com.slm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.slm.data.local.MediAlertDatabase
import com.slm.data.local.UserPreferencesRepository
import com.slm.data.model.AppLanguage
import com.slm.data.model.MedicineEntity
import com.slm.data.model.PatientEntity
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
    private var pendingBanglaPrompt: String? = null
    private var pendingEnglishPrompt: String? = null
    private var activeEnglishFollowUp: String? = null
    private var autoSilenceJob: Job? = null
    private var voiceRepeatJob: Job? = null
    private var volumeRestoreFallbackJob: Job? = null

    companion object {
        private const val UTTERANCE_BANGLA = "medi_alert_voice_bn"
        private const val UTTERANCE_ENGLISH = "medi_alert_voice_en"
    }

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
                    val bnPrompt = buildBanglaVoicePrompt(medicine, patient)
                    val enPrompt = buildEnglishVoicePrompt(medicine, patient)
                    pendingBanglaPrompt = bnPrompt
                    pendingEnglishPrompt = enPrompt
                    if (ttsReady) {
                        speakBilingualReminder(bnPrompt, enPrompt)
                    }
                    voiceRepeatJob?.cancel()
                    voiceRepeatJob = serviceScope.launch {
                        repeat(3) {
                            delay(24_000L)
                            if (ttsReady && mediaPlayer != null) {
                                speakBilingualReminder(bnPrompt, enPrompt)
                            }
                        }
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
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts?.setSpeechRate(0.88f)
            tts?.setPitch(1.0f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    serviceScope.launch(Dispatchers.Main) {
                        setAlarmVolume(0.15f)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    serviceScope.launch(Dispatchers.Main) {
                        handleUtteranceComplete(utteranceId)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    serviceScope.launch(Dispatchers.Main) {
                        handleUtteranceComplete(utteranceId)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    serviceScope.launch(Dispatchers.Main) {
                        handleUtteranceComplete(utteranceId)
                    }
                }
            })
            val bn = pendingBanglaPrompt
            val en = pendingEnglishPrompt
            if (bn != null && en != null) {
                speakBilingualReminder(bn, en)
            }
        }
    }

    private fun handleUtteranceComplete(utteranceId: String?) {
        if (utteranceId == UTTERANCE_BANGLA) {
            val followUp = activeEnglishFollowUp
            activeEnglishFollowUp = null
            if (!followUp.isNullOrBlank()) {
                speakSingleUtterance(
                    text = followUp,
                    isBangla = false,
                    utteranceId = UTTERANCE_ENGLISH
                )
                return
            }
        }
        volumeRestoreFallbackJob?.cancel()
        setAlarmVolume(1.0f)
    }

    private fun speakBilingualReminder(bnText: String, enText: String) {
        val engine = tts ?: return
        setAlarmVolume(0.15f)
        volumeRestoreFallbackJob?.cancel()
        volumeRestoreFallbackJob = serviceScope.launch {
            delay(18_000L)
            setAlarmVolume(1.0f)
        }

        if (selectLanguage(engine, isBangla = true)) {
            activeEnglishFollowUp = enText
            speakSingleUtterance(
                text = bnText,
                isBangla = true,
                utteranceId = UTTERANCE_BANGLA
            )
        } else {
            // Bangla TTS voice data missing on device: skip untranslatable script and speak English clearly
            activeEnglishFollowUp = null
            speakSingleUtterance(
                text = enText,
                isBangla = false,
                utteranceId = UTTERANCE_ENGLISH
            )
        }
    }

    private fun speakSingleUtterance(
        text: String,
        isBangla: Boolean,
        utteranceId: String
    ) {
        val engine = tts ?: return
        selectLanguage(engine, isBangla = isBangla)
        engine.setSpeechRate(0.88f)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun selectLanguage(engine: TextToSpeech, isBangla: Boolean): Boolean {
        return if (isBangla) {
            val candidates = listOf(Locale("bn", "BD"), Locale("bn", "IN"), Locale("bn"))
            candidates.any { loc ->
                val res = engine.setLanguage(loc)
                res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        } else {
            val res = engine.setLanguage(Locale.US)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.ENGLISH)
            }
            true
        }
    }

    private fun setAlarmVolume(volume: Float) {
        runCatching {
            mediaPlayer?.setVolume(volume, volume)
        }
    }

    private fun buildBanglaVoicePrompt(
        medicine: MedicineEntity,
        patient: PatientEntity
    ): String {
        val meal = NotificationHelper.formatMealTiming(medicine.mealTiming, AppLanguage.BANGLA)
        val patientName = patient.name.trim()
        val greeting = if (patientName.isNotEmpty()) {
            "$patientName, আপনার ওষুধ খাওয়ার সময় হয়েছে।"
        } else {
            "আপনার ওষুধ খাওয়ার সময় হয়েছে।"
        }
        return "$greeting ${medicine.name}, ${medicine.dosageAmount} ${medicine.dosageUnit}, $meal।"
    }

    private fun buildEnglishVoicePrompt(
        medicine: MedicineEntity,
        patient: PatientEntity
    ): String {
        val meal = NotificationHelper.formatMealTiming(medicine.mealTiming, AppLanguage.ENGLISH)
        return "It is time to take your medicine. Reminder for ${patient.name}: ${medicine.name}, ${medicine.dosageAmount} ${medicine.dosageUnit}, $meal."
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
        voiceRepeatJob?.cancel()
        volumeRestoreFallbackJob?.cancel()
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
