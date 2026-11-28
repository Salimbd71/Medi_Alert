package com.example.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.MediAlertDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.MedicineEntity
import com.example.data.model.PatientEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.ThemeMode
import com.example.ui.screens.medicineTypeIcon
import com.example.ui.strings.LocalAppStrings
import com.example.ui.theme.EmeraldTaken
import com.example.ui.theme.GlassBackdrop
import com.example.ui.theme.GlassCard
import com.example.ui.theme.MediAlertTheme
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SkipRed
import com.example.ui.theme.StatusPill
import com.example.util.TimeFormatter

class AlarmFullScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        enableEdgeToEdge()

        val scheduleId = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULE_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmConstants.EXTRA_MEDICINE_ID, -1L)
        val patientId = intent.getLongExtra(AlarmConstants.EXTRA_PATIENT_ID, -1L)
        val epochDay = intent.getLongExtra(AlarmConstants.EXTRA_EPOCH_DAY, 0L)
        val scheduledMillis = intent.getLongExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, System.currentTimeMillis())

        setContent {
            var medicine by remember { mutableStateOf<MedicineEntity?>(null) }
            var patient by remember { mutableStateOf<PatientEntity?>(null) }
            var schedule by remember { mutableStateOf<ScheduleEntity?>(null) }
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var language by remember { mutableStateOf(AppLanguage.ENGLISH) }

            LaunchedEffect(scheduleId, medicineId) {
                val db = MediAlertDatabase.getInstance(applicationContext)
                val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
                themeMode = prefs.themeMode
                language = prefs.language
                medicine = db.medicineDao().getMedicineById(medicineId)
                schedule = db.scheduleDao().getScheduleById(scheduleId)
                patient = db.patientDao().getPatientById(patientId)
                    ?: db.patientDao().getAllPatientsOnce().firstOrNull()
            }

            MediAlertTheme(themeMode = themeMode, language = language) {
                FullScreenAlarmContent(
                    medicine = medicine,
                    patient = patient,
                    schedule = schedule,
                    language = language,
                    onAction = { action ->
                        val broadcast = Intent(this, AlarmActionReceiver::class.java).apply {
                            this.action = action
                            putExtra(AlarmConstants.EXTRA_SCHEDULE_ID, scheduleId)
                            putExtra(AlarmConstants.EXTRA_MEDICINE_ID, medicineId)
                            putExtra(AlarmConstants.EXTRA_PATIENT_ID, patient?.id ?: patientId)
                            putExtra(AlarmConstants.EXTRA_EPOCH_DAY, epochDay)
                            putExtra(AlarmConstants.EXTRA_SCHEDULED_MILLIS, scheduledMillis)
                            putExtra(
                                AlarmConstants.EXTRA_NOTIFICATION_ID,
                                NotificationHelper.alarmNotificationId(scheduleId)
                            )
                        }
                        sendBroadcast(broadcast)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun FullScreenAlarmContent(
    medicine: MedicineEntity?,
    patient: PatientEntity?,
    schedule: ScheduleEntity?,
    language: AppLanguage,
    onAction: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    GlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        MintAccent.copy(alpha = 0.05f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = strings.nextDoseTitle,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (schedule != null) {
                            Text(
                                text = TimeFormatter.formatTime(schedule.hour, schedule.minute, language),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = medicine?.name ?: strings.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (medicine != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = medicineTypeIcon(medicine.type),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${medicine.dosageAmount} ${medicine.dosageUnit} • ${
                                        strings.medicineDisplayType(medicine.type, medicine.customTypeName)
                                    }",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(
                                    text = strings.mealTiming(medicine.mealTiming),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (patient != null) {
                                    StatusPill(
                                        text = patient.name,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onAction(AlarmConstants.ACTION_MARK_TAKEN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("alarm_take_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldTaken,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = strings.takeAction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onAction(AlarmConstants.ACTION_SNOOZE_5) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_snooze_5"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(strings.snooze5)
                    }
                    FilledTonalButton(
                        onClick = { onAction(AlarmConstants.ACTION_SNOOZE_10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_snooze_10"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.snooze10)
                    }
                    FilledTonalButton(
                        onClick = { onAction(AlarmConstants.ACTION_SNOOZE_15) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_snooze_15"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.snooze15)
                    }
                }

                Button(
                    onClick = { onAction(AlarmConstants.ACTION_MARK_SKIPPED) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("alarm_skip_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkipRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = strings.skipAction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
