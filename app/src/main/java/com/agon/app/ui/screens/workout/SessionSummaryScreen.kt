package com.agon.app.ui.screens.workout

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agon.app.domain.model.util.UnitConverter
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.ProgressRing
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.StatTile
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.Flame
import com.agon.app.ui.theme.Gold
import com.agon.app.ui.theme.Success
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.viewmodel.WorkoutUiState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Post-session recap: celebration hero, stats, PRs, share. */
@Composable
fun SessionSummaryScreen(
    state: WorkoutUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val context = LocalContext.current
    var showMilestone by remember(state.sessionMilestone) { mutableStateOf(true) }
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { celebrate = true }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Celebration hero
            Reveal(0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(TrainWithMeColors.goldGradient)
                        .padding(22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val checkScale by animateFloatAsState(
                            targetValue = if (celebrate) 1f else 0.3f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                            ),
                            label = "check pop",
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier.scale(checkScale),
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(34.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "SESSION COMPLETE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            summary?.title ?: state.day?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                }
            }

            if (showMilestone) {
                state.sessionMilestone?.let { milestone ->
                    Reveal(1) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.size(10.dp))
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
            }

            if (summary == null) {
                TwmCard {
                    Text("Your session was saved.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Reveal(1) {
                    TwmCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                progress = if (summary.plannedSets == 0) 1f
                                else summary.workingSets.toFloat() / summary.plannedSets,
                                label = "${summary.workingSets}",
                                caption = "of ${summary.plannedSets} sets",
                                size = 104.dp,
                            )
                            Spacer(Modifier.size(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile(
                                    label = "Volume lifted",
                                    value = UnitConverter.formatWeight(summary.volumeKg, state.units),
                                )
                                StatTile(label = "Time", value = "${summary.durationMinutes} min")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${summary.totalReps} reps in ${summary.workingSets} working sets. " +
                                "Next session, the app will adjust your loads from this data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Reveal(2) {
                    OutlinedButton(
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
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Share recap")
                    }
                }

                if (summary.personalRecords.isNotEmpty()) {
                    SectionTitle("New records")
                    Reveal(3) {
                        TwmCard(containerColor = MaterialTheme.colorScheme.primaryContainer, border = false) {
                            summary.personalRecords.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(7.dp)
                                                .size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.size(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Est. 1RM ${UnitConverter.formatWeight(record.estimatedOneRepMaxKg, state.units)}",
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

                SectionTitle("Sets you logged")
                Reveal(4) {
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
            }

            Spacer(Modifier.height(4.dp))
            EmberButton(
                text = "Back to dashboard",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Full-screen celebration confetti on entry
        if (celebrate && summary != null) {
            CelebrationConfetti(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CelebrationConfetti(modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf(true) }
    val particles = remember {
        val colors = listOf(Ember, Flame, Gold, Success, Color.White)
        List(90) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                delay = Random.nextFloat() * 0.45f,
                speed = Random.nextFloat() * 0.5f + 0.5f,
                size = Random.nextFloat() * 8f + 4f,
                color = colors.random(),
                drift = Random.nextFloat() * 0.3f - 0.15f,
            )
        }
    }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(2200, easing = FastOutSlowInEasing))
        active = false
    }
    if (!active) return
    Canvas(modifier = modifier) {
        val p = progress.value
        particles.forEach { pt ->
            val local = ((p - pt.delay) / (1f - pt.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val x = (pt.startX + pt.drift * local) * size.width
            val y = local * local * size.height * pt.speed - 40
            val sway = sin(local * 12 + pt.startX * 20) * 18
            drawCircle(
                color = pt.color.copy(alpha = (1f - local).coerceIn(0f, 1f)),
                radius = pt.size * (1f - local * 0.4f),
                center = androidx.compose.ui.geometry.Offset(x + sway, y),
            )
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val delay: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val drift: Float,
)
