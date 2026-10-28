package com.agon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.domain.model.plan.PlannedExercise
import com.agon.app.ui.theme.accent

/** One exercise inside a day list: art, name, muscle chips and prescription line. */
@Composable
fun ExerciseRow(
    index: Int,
    exercise: PlannedExercise,
    thumbnailAsset: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val accent = exercise.primaryMuscles.firstOrNull()?.accent()
        ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                ExerciseArt(
                    assetPath = thumbnailAsset,
                    contentDescription = null,
                    size = 62.dp,
                    muscle = exercise.primaryMuscles.firstOrNull(),
                )
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = exercise.prescriptionSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercise.primaryMuscles.take(2).forEach { muscle ->
                        MuscleChip(label = muscle.label, color = muscle.accent())
                    }
                    if (exercise.isCompound) {
                        MuscleChip(label = "compound", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun MuscleChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

/**
 * Staggered entrance wrapper: cards fade + rise in sequence when a screen first composes.
 */
@Composable
fun Reveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibility.targetState = true }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(durationMillis = 260, delayMillis = index * 55)) +
            slideInVertically(tween(durationMillis = 320, delayMillis = index * 55)) { it / 12 },
        modifier = modifier,
    ) {
        content()
    }
}

/** Full-bleed gradient header block with icon, kicker, title and subtitle. */
@Composable
fun HeroHeader(
    kicker: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            Color.Transparent,
        ),
    ),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(26.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = kicker.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
