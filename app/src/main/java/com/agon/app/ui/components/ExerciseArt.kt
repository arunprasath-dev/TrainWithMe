package com.agon.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agon.app.domain.model.exercise.MuscleGroup
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.accent
import kotlin.math.cos
import kotlin.math.sin

/**
 * Loads an exercise schematic from app assets with a muscle-tinted backdrop.
 * Falls back to a drawn muscle-glyph placeholder when the asset is missing.
 */
@Composable
fun ExerciseArt(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 14.dp,
    muscle: MuscleGroup? = null,
    glow: Boolean = false,
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val accent = muscle?.accent() ?: Ember

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    radius = size.value * 2.2f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius)),
            )
            // Soft accent wash at the bottom edge for depth.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, accent.copy(alpha = 0.22f)),
                        ),
                    ),
            )
        } else {
            MuscleGlyph(
                muscle = muscle,
                modifier = Modifier.size(size * 0.52f),
                color = accent,
            )
        }
        if (glow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Transparent,
                                accent.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/** Small drawn glyph per muscle family — dumbbell variations so fallback art still feels alive. */
@Composable
fun MuscleGlyph(
    muscle: MuscleGroup?,
    modifier: Modifier = Modifier,
    color: Color = Ember,
) {
    val icon = when (muscle) {
        MuscleGroup.CORE -> Icons.Default.FitnessCenter
        else -> Icons.Default.FitnessCenter
    }
    Icon(icon, contentDescription = null, tint = color, modifier = modifier)
}

/**
 * Animated "energy ring" shown while media buffers: a rotating dashed arc in ember tones.
 */
@Composable
fun BufferingRing(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val transition = rememberInfiniteTransition(label = "buffer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "sweep",
    )
    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2 - 4.dp.toPx()
        for (i in 0 until 3) {
            val angle = sweep + i * 120f
            val start = Offset(
                center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * radius,
                center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * radius,
            )
            drawCircle(
                color = Ember.copy(alpha = 0.9f - i * 0.25f),
                radius = (5 - i).dp.toPx(),
                center = start,
            )
        }
        drawCircle(
            color = Ember.copy(alpha = 0.25f),
            radius = radius,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
