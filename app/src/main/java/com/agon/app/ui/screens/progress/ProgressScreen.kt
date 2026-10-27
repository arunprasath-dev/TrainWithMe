package com.agon.app.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.domain.model.profile.Units
import com.agon.app.domain.model.util.UnitConverter
import com.agon.app.ui.components.DetailRow
import com.agon.app.ui.components.HeroHeader
import com.agon.app.ui.components.ProgressRing
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.Sparkline
import com.agon.app.ui.components.StatTile
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.components.VolumeBarChart
import com.agon.app.ui.components.WeightTrendChart
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.viewmodel.AppViewModelFactory
import com.agon.app.viewmodel.ProgressUiState
import com.agon.app.viewmodel.ProgressViewModel
import com.agon.app.viewmodel.TrendRange
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressRoute(
    factory: AppViewModelFactory,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log measurement")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "hero") {
                HeroHeader(
                    kicker = "Your progress",
                    title = headlineFor(state),
                    subtitle = subheadFor(state),
                    icon = Icons.Default.Insights,
                )
            }
            item(key = "stats") {
                Reveal(0) {
                    TwmCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                progress = state.adherence,
                                label = "${(state.adherence * 100).toInt()}%",
                                caption = "adherence",
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    StatTile(
                                        label = "Day streak",
                                        value = "${state.streak}",
                                    )
                                }
                                StatTile(
                                    label = "Sessions",
                                    value = "${state.completedSessions}",
                                    caption = "of ${state.plannedSessions} planned",
                                )
                            }
                        }
                    }
                }
            }

            item(key = "weight-title") {
                SectionTitle(
                    text = "Body weight",
                    trailing = { RangeChips(range = state.range, onSelect = viewModel::setRange) },
                )
            }
            item(key = "weight") {
                Reveal(1) {
                    TwmCard {
                        WeightTrendChart(
                            points = state.filteredWeight,
                            unitLabel = UnitConverter.unitLabel(state.units),
                        )
                        state.trendKgPerWeek?.let { trend ->
                            Spacer(Modifier.height(8.dp))
                            val display = UnitConverter.kgToDisplay(kotlin.math.abs(trend), state.units)
                            val direction = when {
                                trend > 0.05 -> "gaining"
                                trend < -0.05 -> "losing"
                                else -> "holding steady at"
                            }
                            Text(
                                "Trend: $direction ${trim(display)} ${UnitConverter.unitLabel(state.units)}/week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (state.bodyFatPoints.size >= 2) {
                item(key = "bf-title") { SectionTitle("Body fat") }
                item(key = "bf") {
                    Reveal(2) {
                        TwmCard {
                            WeightTrendChart(
                                points = state.bodyFatPoints,
                                unitLabel = "%",
                                accent = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            item(key = "vol-title") { SectionTitle("Training volume") }
            item(key = "vol") {
                Reveal(2) {
                    TwmCard {
                        VolumeBarChart(
                            weeks = state.weeklyVolume,
                            unitLabel = UnitConverter.unitLabel(state.units),
                        )
                    }
                }
            }

            item(key = "pr-title") { SectionTitle("Personal records") }
            item(key = "pr") {
                Reveal(3) {
                    if (state.personalRecords.isEmpty()) {
                        TwmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Finish sessions with logged sets and your records will land here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        TwmCard(containerColor = MaterialTheme.colorScheme.primaryContainer, border = false) {
                            state.personalRecords.take(8).forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Est. 1RM ${UnitConverter.formatWeight(record.estimatedOneRepMaxKg, state.units)} · ${record.achievedOn.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        UnitConverter.formatWeight(record.weightKg, state.units) + " × ${record.reps}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.weightPoints.size >= 2) {
                item(key = "spark") {
                    Reveal(3) {
                        TwmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MonitorWeight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Weight at a glance", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    UnitConverter.formatWeight(state.latestWeightKg ?: 0.0, state.units),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Sparkline(
                                values = state.weightPoints.takeLast(30).map { it.weightKg },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                accent = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            item(key = "meas-title") { SectionTitle("Measurements") }
            item(key = "meas") {
                Reveal(4) {
                    if (state.measurements.isEmpty()) {
                        TwmCard {
                            Text(
                                "Log weight, body fat and waist to track shape changes, not just the scale.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        TwmCard {
                            state.measurements.take(10).forEach { measurement ->
                                val date = java.time.Instant.ofEpochMilli(measurement.recordedAt)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                DetailRow(
                                    label = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)),
                                    value = buildString {
                                        append(UnitConverter.formatWeight(measurement.weightKg, state.units))
                                        measurement.bodyFatPercent?.let { append(" · $it%") }
                                        measurement.circumference.waistCm?.let { append(" · waist ${format(it)} cm") }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "hist-title") { SectionTitle("Session history") }
            item(key = "hist") {
                Reveal(5) {
                    if (state.sessions.isEmpty()) {
                        TwmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Completed sessions will be listed here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        TwmCard {
                            state.sessions.take(12).forEach { session ->
                                DetailRow(
                                    label = session.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.US)) + " · " + session.title,
                                    value = if (session.durationMinutes > 0) "${session.durationMinutes} min" else "done",
                                )
                            }
                        }
                    }
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(70.dp)) }
        }
    }

    if (showAddDialog) {
        AddMeasurementDialog(
            units = state.units,
            onDismiss = { showAddDialog = false },
            onSave = { weight, bf, waist ->
                viewModel.addMeasurement(weight, bf, waist)
                showAddDialog = false
            },
        )
    }
}

private fun headlineFor(state: ProgressUiState): String = when {
    state.completedSessions == 0 -> "Every rep starts here"
    state.streak >= 5 -> "On fire — ${state.streak} in a row"
    state.adherence >= 0.8f -> "Crushing this block"
    state.adherence >= 0.5f -> "Building momentum"
    else -> "Keep showing up"
}

private fun subheadFor(state: ProgressUiState): String = when {
    state.completedSessions == 0 -> "Log your first session and watch these charts come alive."
    else -> "${state.completedSessions} sessions banked. The trend is your friend."
}

@Composable
private fun RangeChips(range: TrendRange, onSelect: (TrendRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TrendRange.entries.forEach { option ->
            FilterChip(
                selected = range == option,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun AddMeasurementDialog(
    units: Units,
    onDismiss: () -> Unit,
    onSave: (Double, Double?, Double?) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    val label = if (units == Units.KG) "cm" else "in"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Weight") },
                    suffix = { Text(UnitConverter.unitLabel(units)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
                    label = { Text("Body fat (optional)") },
                    suffix = { Text("%") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Waist (optional)") },
                    suffix = { Text(label) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weightKg = weight.toDoubleOrNull()?.let { UnitConverter.displayToKg(it, units) }
                    val waistCm = waist.toDoubleOrNull()?.let {
                        if (units == Units.KG) it else UnitConverter.displayToCm(it, units)
                    }
                    if (weightKg != null) onSave(weightKg, bodyFat.toDoubleOrNull(), waistCm)
                },
                enabled = weight.toDoubleOrNull() != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun format(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun trim(value: Double): String = format(value)
