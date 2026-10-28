package com.agon.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agon.app.domain.model.profile.Units
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Property delegate keeps a single DataStore instance per process (required by DataStore). */
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")


class DefaultSettingsRepository (
    private val context: Context,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> = context.settingsStore.data.map(::toSettings)

    override suspend fun current(): UserSettings = toSettings(context.settingsStore.data.first())

    override suspend fun setUnits(units: Units) = edit { prefs ->
        prefs[Keys.UNITS] = units.name
    }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { prefs ->
        prefs[Keys.THEME] = mode.name
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) = edit { prefs ->
        prefs[Keys.REMINDERS] = enabled
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) = edit { prefs ->
        prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
        prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }

    override suspend fun setRestTimerSound(enabled: Boolean) = edit { prefs ->
        prefs[Keys.REST_SOUND] = enabled
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) = edit { prefs ->
        prefs[Keys.KEEP_SCREEN_ON] = enabled
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }

    private fun toSettings(prefs: Preferences): UserSettings = UserSettings(
        units = prefs[Keys.UNITS]?.let { runCatching { Units.valueOf(it) }.getOrNull() } ?: Units.KG,
        themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        remindersEnabled = prefs[Keys.REMINDERS] ?: false,
        reminderHour = prefs[Keys.REMINDER_HOUR] ?: 18,
        reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
        restTimerSoundEnabled = prefs[Keys.REST_SOUND] ?: true,
        keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
    )

    private object Keys {
        val UNITS = stringPreferencesKey("units")
        val THEME = stringPreferencesKey("theme_mode")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REST_SOUND = booleanPreferencesKey("rest_timer_sound")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }
}
