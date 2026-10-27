package com.trainwithme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainwithme.core.data.reminder.ReminderScheduler
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.SettingsRepository
import com.trainwithme.core.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
@HiltViewModel
class AppViewModel @Inject constructor(
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
