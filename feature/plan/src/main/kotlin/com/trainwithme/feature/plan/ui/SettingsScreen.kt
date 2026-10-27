package com.trainwithme.feature.plan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.core.data.repository.ThemeMode
import com.trainwithme.core.designsystem.component.BannerTone
import com.trainwithme.core.designsystem.component.DetailRow
import com.trainwithme.core.designsystem.component.MessageBanner
import com.trainwithme.core.designsystem.component.PillRow
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.SecondaryButton
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.model.profile.Units
import com.trainwithme.feature.plan.vm.SettingsUiState
import com.trainwithme.feature.plan.vm.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onDataDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { Spacer(Modifier.navigationBarsPadding()) },
    ) { padding ->
        SettingsContent(
            state = state,
            modifier = Modifier.padding(padding),
            onUnits = viewModel::setUnits,
            onTheme = viewModel::setTheme,
            onReminders = viewModel::setRemindersEnabled,
            onReminderTime = { showTimePicker = true },
            onRestSound = viewModel::setRestSound,
            onKeepScreenOn = viewModel::setKeepScreenOn,
            onEditProfile = onEditProfile,
            onExport = { viewModel.exportData { payload -> shareJson(context, payload) } },
            onRequestDelete = viewModel::requestDelete,
            onConfirmDelete = { viewModel.deleteEverything(onDataDeleted) },
            onDismissMessage = viewModel::dismissMessage,
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.settings.reminderHour,
            initialMinute = state.settings.reminderMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = pickerState) },
            title = { Text("Reminder time") },
        )
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.requestDelete(false) },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This removes your profile, plan, sessions and measurements from this device. " +
                        "It cannot be undone. Export first if you want a copy.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.requestDelete(false); viewModel.deleteEverything(onDataDeleted) }) {
                    Text("Delete everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.requestDelete(false) }) { Text("Keep my data") }
            },
        )
    }
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onUnits: (Units) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onReminders: (Boolean) -> Unit,
    onReminderTime: () -> Unit,
    onRestSound: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onExport: () -> Unit,
    onRequestDelete: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.message?.let { MessageBanner(it, tone = BannerTone.ERROR, actionLabel = "Dismiss", onAction = onDismissMessage) }

        TwmCard {
            SectionTitle("Profile")
            Spacer(Modifier.height(6.dp))
            DetailRow("Name", state.profileName ?: "—")
            DetailRow("Plan", state.planSummary ?: "no active plan")
            DetailRow("Completed sessions", "${state.completedSessions}")
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Edit body data", onClick = onEditProfile, modifier = Modifier.fillMaxWidth())
        }

        TwmCard {
            SectionTitle("Units")
            Spacer(Modifier.height(8.dp))
            PillRow(
                options = Units.entries.toList(),
                selected = settings.units,
                onSelect = onUnits,
                label = { if (it == Units.KG) "Kilograms" else "Pounds" },
            )
            Text(
                "Stored data stays metric — this only changes what you see.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TwmCard {
            SectionTitle("Appearance")
            Spacer(Modifier.height(8.dp))
            PillRow(
                options = ThemeMode.entries.toList(),
                selected = settings.themeMode,
                onSelect = onTheme,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                },
            )
        }

        TwmCard {
            SectionTitle("Reminders")
            ToggleRow(
                title = "Training-day reminder",
                subtitle = "One notification on days you have a session",
                checked = settings.remindersEnabled,
                onCheckedChange = onReminders,
            )
            if (settings.remindersEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Time", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onReminderTime) {
                        Text("%02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                    }
                }
            }
        }

        TwmCard {
            SectionTitle("During a workout")
            ToggleRow(
                title = "Rest timer alert",
                subtitle = "Vibrate and beep when rest ends",
                checked = settings.restTimerSoundEnabled,
                onCheckedChange = onRestSound,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ToggleRow(
                title = "Keep screen on",
                subtitle = "Prevents the screen sleeping mid-set",
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOn,
            )
        }

        TwmCard {
            SectionTitle("Your data")
            Spacer(Modifier.height(6.dp))
            Text(
                "Everything lives on this device. No account, no sync, no analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = if (state.exporting) "Preparing…" else "Export as JSON",
                onClick = onExport,
                enabled = !state.exporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Delete all data",
                onClick = { onRequestDelete(true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        TwmCard {
            SectionTitle("About")
            Spacer(Modifier.height(6.dp))
            Text(
                "TrainWithMe builds and coaches personalised gym programmes. It is a training guide, " +
                    "not medical advice — consult a professional before starting a new programme, " +
                    "especially with injuries or health conditions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Writes the export to a user-chosen file and offers to share it. */
private fun shareJson(context: android.content.Context, payload: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_TEXT, payload)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "TrainWithMe data export")
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, "Export TrainWithMe data").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
