package com.trainwithme.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.core.designsystem.component.DetailRow
import com.trainwithme.core.designsystem.component.EmptyState
import com.trainwithme.core.designsystem.component.PillRow
import com.trainwithme.core.designsystem.component.ProgressRing
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.StatTile
import com.trainwithme.core.designsystem.component.TrendLineChart
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.designsystem.component.WeeklyBarChart
import com.trainwithme.core.model.profile.Units
import com.trainwithme.core.model.util.UnitConverter
import com.trainwithme.feature.progress.vm.ProgressUiState
import com.trainwithme.feature.progress.vm.ProgressViewModel
import com.trainwithme.feature.progress.vm.TrendRange
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Log measurement")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { Spacer(Modifier.navigationBarsPadding()) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        ProgressContent(
            state = state,
            modifier = Modifier.padding(padding),
            onRangeChange = viewModel::setRange,
        )
    }

    if (showAdd) {
        AddMeasurementDialog(
            units = state.units,
            onDismiss = { showAdd = false },
            onSave = { weight, bodyFat, waist ->
                viewModel.addMeasurement(weight, bodyFat, waist)
                showAdd = false
            },
        )
    }
}

@Composable
fun ProgressContent(
    state: ProgressUiState,
    modifier: Modifier = Modifier,
    onRangeChange: (TrendRange) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TwmCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = state.adherence,
                    label = "${(state.adherence * 100).toInt()}%",
                    caption = "adherence",
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    StatTile(label = "Sessions", value = "${state.completedSessions}/${state.plannedSessions}")
                    Spacer(Modifier.height(8.dp))
                    StatTile(label = "Streak", value = "${state.streak}", caption = "in a row")
                }
            }
        }

        SectionTitle("Body weight")
        TwmCard {
            PillRow(
                options = TrendRange.entries.toList(),
                selected = state.range,
                onSelect = onRangeChange,
                label = { it.label },
            )
            Spacer(Modifier.height(10.dp))
            TrendLineChart(
                points = state.filteredWeight,
                unitLabel = UnitConverter.unitLabel(state.units),
                emptyMessage = "No weigh-ins in this range. Log one from the dashboard or the + button.",
            )
            state.weightChangeKg?.let { delta ->
                val sign = if (delta > 0) "+" else ""
                DetailRow(
                    label = "Change in range",
                    value = "$sign${format(delta)} ${UnitConverter.unitLabel(state.units)}",
                )
            }
            state.trendKgPerWeek?.let { trend ->
                DetailRow(
                    label = "Weekly trend",
                    value = "${if (trend > 0) "+" else ""}${format(trend)} ${UnitConverter.unitLabel(state.units)}/week",
                )
            }
        }

        if (state.bodyFatPoints.size >= 2) {
            SectionTitle("Body fat")
            TwmCard {
                TrendLineChart(points = state.bodyFatPoints, unitLabel = "%", seriesLabel = "Body fat")
            }
        }

        SectionTitle("Training volume")
        TwmCard {
            val values = state.weeklyVolume.takeLast(8).map { week ->
                "W" + week.weekStart.format(DateTimeFormatter.ofPattern("dd")) to week.volumeKg
            }
            WeeklyBarChart(
                values = values,
                emptyMessage = "Finish a session to see weekly volume.",
                valueFormatter = { value -> format(UnitConverter.kgToDisplay(value, state.units)) },
            )
            Text(
                "Bar height is total load lifted (weight × reps) per week.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Personal records")
        if (state.personalRecords.isEmpty()) {
            TwmCard {
                EmptyState(
                    icon = Icons.Default.EmojiEvents,
                    title = "No records yet",
                    message = "Complete a session with weights and your estimated 1-rep maxes appear here.",
                )
            }
        } else {
            TwmCard {
                state.personalRecords.take(8).forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Best ${UnitConverter.formatWeight(record.weightKg, state.units)} × ${record.reps} · " +
                                    record.achievedOn.format(DateTimeFormatter.ofPattern("d MMM")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            UnitConverter.formatWeight(record.estimatedOneRepMaxKg, state.units),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        SectionTitle("Measurements")
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
                        label = date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                        value = buildString {
                            append(UnitConverter.formatWeight(measurement.weightKg, state.units))
                            measurement.bodyFatPercent?.let { append(" · $it%") }
                            measurement.circumference.waistCm?.let { append(" · waist ${format(it)} cm") }
                        },
                    )
                }
            }
        }

        SectionTitle("Session history")
        if (state.sessions.isEmpty()) {
            TwmCard {
                Text(
                    "Completed sessions will be listed here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TwmCard {
                state.sessions.take(12).forEach { session ->
                    DetailRow(
                        label = session.date.format(DateTimeFormatter.ofPattern("d MMM")) + " · " + session.title,
                        value = if (session.durationMinutes > 0) "${session.durationMinutes} min" else "done",
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
                    label = { Text("Body fat (optional)") },
                    suffix = { Text("%") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Waist (optional)") },
                    suffix = { Text(label) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
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
