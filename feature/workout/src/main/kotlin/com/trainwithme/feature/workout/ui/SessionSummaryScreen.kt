package com.trainwithme.feature.workout.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var showMilestone by remember(state.sessionMilestone) { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Session complete", style = MaterialTheme.typography.displaySmall)
        Text(
            summary?.title ?: state.day?.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (showMilestone) {
            state.sessionMilestone?.let { milestone ->
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            milestone,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showMilestone = false }) { Text("Dismiss") }
                    }
                }
            }
        }

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

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val recap = buildString {
                        append("TrainWithMe workout recap: ${summary.title}\n")
                        append("${summary.workingSets} working sets · ${summary.totalReps} reps · ${summary.durationMinutes} min")
                        if (summary.personalRecords.isNotEmpty()) {
                            append("\nNew PRs: ")
                            append(summary.personalRecords.joinToString { it.exerciseName })
                        }
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, recap)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share workout recap"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share recap")
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

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            text = "Back to dashboard",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
