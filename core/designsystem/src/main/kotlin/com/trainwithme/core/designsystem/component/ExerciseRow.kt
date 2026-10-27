package com.trainwithme.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trainwithme.core.model.plan.PlannedExercise

/**
 * One exercise inside a day list: thumbnail, name and prescription line.
 *
 * Lives in the design system because both the plan browser and the pre-session briefing show the
 * same row and features never depend on each other.
 */
@Composable
fun ExerciseRow(
    index: Int,
    exercise: PlannedExercise,
    thumbnailAsset: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetImage(
            assetPath = thumbnailAsset,
            contentDescription = null,
            size = 52.dp,
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = exercise.prescriptionSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
