package com.agon.app.ui.screens.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.agon.app.ui.components.BannerTone
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.ExerciseRow
import com.agon.app.ui.components.MessageBanner
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.viewmodel.WorkoutUiState

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                EmberButton(
                    text = if (state.session != null) "Resume workout" else "Begin workout",
                    onClick = onStart,
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (state.day?.exercises?.isNotEmpty() ?: false),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let {
                item(key = "msg") {
                    MessageBanner(it, tone = BannerTone.INFO, actionLabel = "OK", onAction = onDismissMessage)
                }
            }
            state.error?.let {
                item(key = "err") { MessageBanner(it, tone = BannerTone.ERROR) }
            }

            item(key = "stats") {
                Reveal(0) {
                    TwmCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            OverviewStat("Exercises", "${state.day?.exerciseCount ?: 0}")
                            OverviewStat("Sets", "${state.day?.totalSets ?: 0}")
                            OverviewStat("Est. time", "${state.day?.estimatedMinutes ?: 0} min")
                        }
                        state.day?.focusLabel?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item(key = "warmup-title") { SectionTitle("Warm-up") }
            item(key = "warmup") {
                Reveal(1) {
                    TwmCard {
                        Text(
                            "Tick these off as you go — the timer is optional, the warm-up is not.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        state.day?.warmup?.forEachIndexed { index, step ->
                            val checked = index in state.warmupChecked
                            val bg by animateColorAsState(
                                targetValue = if (checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                animationSpec = tween(200),
                                label = "warmup check",
                            )
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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(bg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Whatshot,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
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
                }
            }

            item(key = "session-title") { SectionTitle("The session") }
            itemsIndexed(state.exercises, key = { _, p -> p.planned.exerciseId + p.planned.slotIndex }) { index, progress ->
                ExerciseRow(
                    index = index,
                    exercise = progress.planned,
                    thumbnailAsset = "thumbnails/${progress.planned.exerciseId}.jpg",
                )
            }

            if (!state.day?.cooldown.isNullOrEmpty()) {
                item(key = "cool-title") { SectionTitle("Cool-down") }
                item(key = "cool") {
                    TwmCard {
                        state.day?.cooldown?.forEach { stretch ->
                            Text(
                                "• ${stretch.name} · ${stretch.durationSeconds}s",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
