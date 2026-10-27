package com.trainwithme.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.StatTile
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.model.util.UnitConverter
import com.trainwithme.feature.workout.vm.WorkoutUiState

/** Post-session recap: what was done, and what improved. */
@Composable
fun SessionSummaryScreen(
    state: WorkoutUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Session complete", style = MaterialTheme.typography.displaySmall)
        Text(
            summary?.title ?: state.day?.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (summary == null) {
            TwmCard {
                Text(
                    "Your session was saved.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            TwmCard {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StatTile(
                        label = "Volume",
                        value = UnitConverter.formatWeight(summary.volumeKg, state.units),
                    )
                    StatTile(label = "Sets", value = "${summary.workingSets}", caption = "of ${summary.plannedSets}")
                    StatTile(label = "Time", value = "${summary.durationMinutes} min")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${summary.totalReps} reps in ${summary.workingSets} working sets. " +
                        "Next session, the app will adjust your loads from this data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (summary.personalRecords.isNotEmpty()) {
                SectionTitle("New records")
                TwmCard {
                    summary.personalRecords.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp),
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Estimated 1RM ${UnitConverter.formatWeight(record.estimatedOneRepMaxKg, state.units)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                UnitConverter.formatWeight(record.weightKg, state.units) + " × ${record.reps}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            SectionTitle("Sets you logged")
            TwmCard {
                state.exercises.filter { it.loggedSets.isNotEmpty() }.forEach { progress ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(progress.planned.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            progress.loggedSets.filter { !it.isWarmup }.joinToString("  ·  ") { set ->
                                "${UnitConverter.formatWeight(set.weightKg, state.units)}×${set.reps}"
                            }.ifBlank { "no working sets" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Back to dashboard",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.navigationBarsPadding())
        Spacer(Modifier.height(24.dp))
    }
}
