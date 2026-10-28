package com.agon.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.reminder.ReminderScheduler
import com.agon.app.data.repository.PlanRepository
import com.agon.app.data.repository.SettingsRepository
import com.agon.app.data.repository.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-level state that outlives a single screen: the theme, and keeping the reminder in sync with
 * the stored settings (WorkManager work is not registered on a fresh install until something asks
 * for it).
 */

class AppViewModel (
    settingsRepository: SettingsRepository,
    planRepository: PlanRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeSettings()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            if (settings.remindersEnabled && planRepository.activePlan() != null) {
                reminderScheduler.reschedule(true, settings.reminderHour, settings.reminderMinute)
            }
        }
    }
}
