package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.domain.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medi_alert_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val activePatientId: Long? = null, // null means "All Patients"
    val customAlarmSoundUri: String? = null,
    val voiceReminderEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
    val autoMissedWindowMinutes: Int = 30
) {
    val hasPinLock: Boolean
        get() = !pinHash.isNullOrBlank()

    val isLockActive: Boolean
        get() = appLockEnabled && (hasPinLock || biometricEnabled)
}

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVE_PATIENT_ID = longPreferencesKey("active_patient_id")
        val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
        val VOICE_REMINDER = booleanPreferencesKey("voice_reminder")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_MISSED_MINUTES = intPreferencesKey("auto_missed_minutes")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            language = runCatching {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.name)
            }.getOrDefault(AppLanguage.ENGLISH),
            activePatientId = prefs[Keys.ACTIVE_PATIENT_ID]?.takeIf { it > 0L },
            customAlarmSoundUri = prefs[Keys.ALARM_SOUND_URI],
            voiceReminderEnabled = prefs[Keys.VOICE_REMINDER] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH],
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoMissedWindowMinutes = prefs[Keys.AUTO_MISSED_MINUTES] ?: 30
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setActivePatientId(patientId: Long?) {
        context.dataStore.edit {
            if (patientId == null) {
                it.remove(Keys.ACTIVE_PATIENT_ID)
            } else {
                it[Keys.ACTIVE_PATIENT_ID] = patientId
            }
        }
    }

    suspend fun setCustomAlarmSoundUri(uri: String?) {
        context.dataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(Keys.ALARM_SOUND_URI)
            } else {
                it[Keys.ALARM_SOUND_URI] = uri
            }
        }
    }

    suspend fun setVoiceReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_REMINDER] = enabled }
    }

    suspend fun enableAppLockWithPin(pin: String, biometricEnabled: Boolean) {
        val hash = PinHasher.hashPin(pin)
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.APP_LOCK_ENABLED] = true
            it[Keys.BIOMETRIC_ENABLED] = biometricEnabled
        }
    }

    suspend fun enableBiometricOnlyLock() {
        context.dataStore.edit {
            it.remove(Keys.PIN_HASH)
            it[Keys.BIOMETRIC_ENABLED] = true
            it[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun setPinLock(pin: String) {
        val hash = PinHasher.hashPin(pin)
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun clearPinLock() {
        context.dataStore.edit {
            it.remove(Keys.PIN_HASH)
            val bioActive = it[Keys.BIOMETRIC_ENABLED] ?: false
            it[Keys.APP_LOCK_ENABLED] = bioActive
        }
    }

    suspend fun disableAppLock() {
        context.dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = false
            it[Keys.BIOMETRIC_ENABLED] = false
            it.remove(Keys.PIN_HASH)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.BIOMETRIC_ENABLED] = enabled
            val hasPin = !it[Keys.PIN_HASH].isNullOrBlank()
            it[Keys.APP_LOCK_ENABLED] = enabled || hasPin
        }
    }

    suspend fun setAutoMissedWindowMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.AUTO_MISSED_MINUTES] = minutes.coerceIn(15, 120) }
    }
}
