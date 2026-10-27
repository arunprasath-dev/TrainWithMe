package com.trainwithme.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme

/** Standard app card: one level of elevation, generous padding, no shadows-on-shadows. */
@Composable
fun TwmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val shape = RoundedCornerShape(20.dp)
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
        trailing?.invoke()
    }
}

/** Big-number tile used on the dashboard and progress screens. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = accent)
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Horizontal single-choice selector used across onboarding. */
@Composable
fun <T> OptionChips(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    supportingText: (T) -> String? = { null },
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            val container = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            val border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
                color = container,
                shape = RoundedCornerShape(14.dp),
                border = border,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = label(option), style = MaterialTheme.typography.titleMedium)
                        supportingText(option)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Compact pill row, good for 2–4 mutually exclusive values (kg/lb, RPE chips). */
@Composable
fun <T> PillRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape = CircleShape,
                modifier = Modifier
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) },
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** Numeric stepper with large touch targets (weight, reps, minutes). */
@Composable
fun NumberStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    modifier: Modifier = Modifier,
    min: Double = 0.0,
    max: Double = 500.0,
    format: (Double) -> String = { it.toInt().toString() },
    contentDescription: String = "value",
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onValueChange((value - step).coerceIn(min, max)) },
            enabled = value > min,
            modifier = Modifier
                .size(48.dp)
                .semantics { this.contentDescription = "Decrease $contentDescription" },
        ) {
            Icon(Icons.Default.Remove, contentDescription = null)
        }
        Text(
            text = format(value),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        IconButton(
            onClick = { onValueChange((value + step).coerceIn(min, max)) },
            enabled = value < max,
            modifier = Modifier
                .size(48.dp)
                .semantics { this.contentDescription = "Increase $contentDescription" },
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            TonalButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Inline informational banner — errors, warnings and safety notes all use this. */
@Composable
fun MessageBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.INFO,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val color = when (tone) {
        BannerTone.INFO -> MaterialTheme.colorScheme.secondary
        BannerTone.WARNING -> MaterialTheme.colorScheme.tertiary
        BannerTone.ERROR -> MaterialTheme.colorScheme.error
        BannerTone.SUCCESS -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

enum class BannerTone { INFO, WARNING, ERROR, SUCCESS }

/** A key/value row with a divider-friendly layout, used in summaries and settings. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentsPreview() {
    TrainWithMeTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TwmCard {
                SectionTitle("Today", trailing = { Text("Week 2", style = MaterialTheme.typography.labelLarge) })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatTile("Volume", "4,820 kg", caption = "+8% vs last week")
                    StatTile("Sets", "18", caption = "of 20 planned")
                }
            }
            MessageBanner("Deload week: keep 3 reps in reserve.", tone = BannerTone.WARNING)
            PrimaryButton(text = "Start workout", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
