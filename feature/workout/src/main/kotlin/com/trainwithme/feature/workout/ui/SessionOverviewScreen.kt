package com.trainwithme.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.trainwithme.core.designsystem.component.BannerTone
import com.trainwithme.core.designsystem.component.MessageBanner
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.designsystem.component.ExerciseRow
import com.trainwithme.feature.workout.vm.WorkoutUiState

/** Pre-session briefing: what you are about to do, and the warm-up checklist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionOverviewScreen(
    state: WorkoutUiState,
    onStart: () -> Unit,
    onToggleWarmup: (Int) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.day?.title ?: "Session", style = MaterialTheme.typography.titleLarge)
                        Text(
                            state.plan?.let { plan ->
                                val week = plan.weekOf(state.day?.id.orEmpty())
                                "Week ${(week?.index ?: 0) + 1} · ${week?.label.orEmpty()}"
                            }.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                PrimaryButton(
                    text = "Begin workout",
                    onClick = onStart,
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (state.day?.exercises?.isNotEmpty() ?: false),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { MessageBanner(it, tone = BannerTone.INFO, actionLabel = "OK", onAction = onDismissMessage) }
            state.error?.let { MessageBanner(it, tone = BannerTone.ERROR) }

            TwmCard {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    OverviewStat("Exercises", "${state.day?.exerciseCount ?: 0}")
                    OverviewStat("Sets", "${state.day?.totalSets ?: 0}")
                    OverviewStat("Est. time", "${state.day?.estimatedMinutes ?: 0} min")
                }
                state.day?.focusLabel?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionTitle("Warm-up")
            TwmCard {
                Text(
                    "Tick these off as you go — the timer is optional, the warm-up is not.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                state.day?.warmup?.forEachIndexed { index, step ->
                    val checked = index in state.warmupChecked
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onToggleWarmup(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (checked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (checked) TextDecoration.LineThrough else null,
                            )
                            Text(
                                text = step.instructions,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionTitle("The session")
            state.exercises.forEachIndexed { index, progress ->
                ExerciseRow(
                    index = index,
                    exercise = progress.planned,
                    thumbnailAsset = "thumbnails/${progress.planned.exerciseId}.jpg",
                )
            }

            SectionTitle("Cool-down")
            TwmCard {
                state.day?.cooldown?.forEach { stretch ->
                    Text(
                        "• ${stretch.name} · ${stretch.durationSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun OverviewStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
