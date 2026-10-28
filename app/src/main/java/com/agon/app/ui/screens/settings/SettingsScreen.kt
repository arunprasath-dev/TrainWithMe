package com.agon.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.data.repository.ThemeMode
import com.agon.app.domain.model.profile.Units
import com.agon.app.ui.components.BannerTone
import com.agon.app.ui.components.DetailRow
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.GhostButton
import com.agon.app.ui.components.MessageBanner
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.TwmCard
import com.agon.app.viewmodel.AppViewModelFactory
import com.agon.app.viewmodel.SettingsUiState
import com.agon.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onDataDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setRemindersEnabled(true)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        SettingsContent(
            state = state,
            modifier = Modifier.padding(padding),
            onUnits = viewModel::setUnits,
            onTheme = viewModel::setTheme,
            onReminders = { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setRemindersEnabled(enabled)
                }
            },
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
            title = { Text("Delete everything?") },
            text = {
                Text(
                    "This permanently deletes your profile, plan, sessions, measurements and settings on this device. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.requestDelete(false); viewModel.deleteEverything(onDataDeleted) },
                ) { Text("Delete all data", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.requestDelete(false) }) { Text("Keep my data") }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.message?.let {
            item(key = "msg") {
                MessageBanner(it, tone = BannerTone.INFO, actionLabel = "OK", onAction = onDismissMessage)
            }
        }

        item(key = "profile") {
            Reveal(0) {
                TwmCard {
                    SectionTitle("Profile & plan")
                    Spacer(Modifier.height(8.dp))
                    state.profileName?.let { DetailRow("Athlete", it) }
                    state.planSummary?.let { DetailRow("Plan", it) }
                    DetailRow("Sessions completed", "${state.completedSessions}")
                    Spacer(Modifier.height(10.dp))
                    GhostButton(
                        text = "Edit profile",
                        onClick = onEditProfile,
                        icon = Icons.Default.Person,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "appearance") {
            Reveal(1) {
                TwmCard {
                    SectionTitle("Appearance")
                    Spacer(Modifier.height(10.dp))
                    Text("Units", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    SegmentedChoice(
                        options = listOf(Units.KG to "Kilograms (kg)", Units.LB to "Pounds (lb)"),
                        selected = state.settings.units,
                        onSelect = onUnits,
                        icon = Icons.Default.Scale,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    SegmentedChoice(
                        options = listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                        ),
                        selected = state.settings.themeMode,
                        onSelect = onTheme,
                        icon = null,
                    )
                }
            }
        }

        item(key = "training") {
            Reveal(2) {
                TwmCard {
                    SectionTitle("Training")
                    Spacer(Modifier.height(4.dp))
                    SettingsToggle(
                        icon = Icons.Default.Timer,
                        title = "Rest timer sound & vibration",
                        subtitle = "Beep and buzz when rest ends",
                        checked = state.settings.restTimerSoundEnabled,
                        onCheckedChange = onRestSound,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsToggle(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Keep screen on",
                        subtitle = "While the session player is visible",
                        checked = state.settings.keepScreenOn,
                        onCheckedChange = onKeepScreenOn,
                    )
                }
            }
        }

        item(key = "reminders") {
            Reveal(3) {
                TwmCard {
                    SectionTitle("Reminders")
                    Spacer(Modifier.height(4.dp))
                    SettingsToggle(
                        icon = Icons.Default.Notifications,
                        title = "Training-day reminder",
                        subtitle = if (state.settings.remindersEnabled) {
                            "Daily at %02d:%02d on training days".format(
                                state.settings.reminderHour,
                                state.settings.reminderMinute,
                            )
                        } else "Off — never miss a session",
                        checked = state.settings.remindersEnabled,
                        onCheckedChange = onReminders,
                    )
                    if (state.settings.remindersEnabled) {
                        Spacer(Modifier.height(8.dp))
                        GhostButton(
                            text = "Change time (%02d:%02d)".format(
                                state.settings.reminderHour,
                                state.settings.reminderMinute,
                            ),
                            onClick = onReminderTime,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        item(key = "data") {
            Reveal(4) {
                TwmCard {
                    SectionTitle("Your data")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Everything lives on this device. Export a copy any time, or wipe it all.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    GhostButton(
                        text = if (state.exporting) "Preparing…" else "Export my data",
                        onClick = onExport,
                        enabled = !state.exporting,
                        icon = Icons.Default.IosShare,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    EmberButton(
                        text = "Delete all data",
                        onClick = { onRequestDelete(true) },
                        icon = Icons.Default.DeleteForever,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "about") {
            Reveal(5) {
                TwmCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                            Icon(
                                Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("TrainWithMe", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Offline-first training coach · v1.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item(key = "spacer") { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: ImageVector?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null && options.size == 2) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
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
        Surface(
            color = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = CircleShape,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
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

/** Shares the JSON export via the system sheet. */
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
