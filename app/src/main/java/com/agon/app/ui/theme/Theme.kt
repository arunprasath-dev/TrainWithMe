package com.agon.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.agon.app.domain.model.exercise.MuscleGroup
import com.agon.app.domain.model.exercise.VolumeFamily

private val DarkScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF1A0B05),
    primaryContainer = EmberContainerDark,
    onPrimaryContainer = Color(0xFFFFD9C9),
    secondary = CyanPop,
    onSecondary = Color(0xFF06202B),
    secondaryContainer = Color(0xFF17323D),
    onSecondaryContainer = Color(0xFFD3EEFB),
    tertiary = Success,
    onTertiary = Color(0xFF04170F),
    tertiaryContainer = Color(0xFF0B2E23),
    onTertiaryContainer = Color(0xFFA9ECD3),
    background = Ink,
    onBackground = TextDark,
    surface = Ink,
    onSurface = TextDark,
    surfaceVariant = InkCard,
    onSurfaceVariant = TextMutedDark,
    surfaceContainer = InkElevated,
    surfaceContainerLow = Color(0xFF0E1217),
    surfaceContainerHigh = InkCard,
    surfaceContainerHighest = Color(0xFF202834),
    outline = InkBorder,
    outlineVariant = Color(0xFF222932),
    error = Danger,
    onError = Color(0xFF2B0A0A),
)

private val LightScheme = lightColorScheme(
    primary = EmberDark,
    onPrimary = Color.White,
    primaryContainer = EmberContainerLight,
    onPrimaryContainer = Color(0xFF43180A),
    secondary = CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEDFA),
    onSecondaryContainer = Color(0xFF04283B),
    tertiary = SuccessDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F5E4),
    onTertiaryContainer = Color(0xFF043B28),
    background = Paper,
    onBackground = TextLight,
    surface = Paper,
    onSurface = TextLight,
    surfaceVariant = PaperCard,
    onSurfaceVariant = TextMutedLight,
    surfaceContainer = Color(0xFFF3EEE9),
    surfaceContainerLow = Color(0xFFF8F4F1),
    surfaceContainerHigh = PaperCard,
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = PaperBorder,
    outlineVariant = Color(0xFFF0EBE6),
    error = DangerDark,
    onError = Color.White,
)

private val TwmTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)

private val TwmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Semantic colours that Material's scheme does not model. */
object TrainWithMeColors {
    val success: Color @Composable get() = if (isSystemInDarkTheme()) Success else SuccessDark
    val warning: Color @Composable get() = if (isSystemInDarkTheme()) Warning else WarningDark
    val danger: Color @Composable get() = if (isSystemInDarkTheme()) Danger else DangerDark
    val info: Color @Composable get() = if (isSystemInDarkTheme()) Info else InfoDark
    val chartLine: Color @Composable get() = MaterialTheme.colorScheme.primary
    val chartFill: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    /** The signature ember gradient: buttons, hero cards, active tabs. */
    val emberGradient: Brush
        get() = Brush.horizontalGradient(listOf(EmberDeep, Ember, Flame))

    val emberVertical: Brush
        get() = Brush.verticalGradient(listOf(Ember, EmberDeep))

    /** Gold→ember gradient for PRs, streaks and celebration moments. */
    val goldGradient: Brush
        get() = Brush.horizontalGradient(listOf(Gold, Flame, Ember))
}

/** Per-muscle accent used by focus chips, volume bars and exercise art. */
fun MuscleGroup.accent(): Color = when (this) {
    MuscleGroup.CHEST -> MuscleChest
    MuscleGroup.BACK, MuscleGroup.LATS, MuscleGroup.TRAPS -> MuscleBack
    MuscleGroup.SHOULDERS -> MuscleShoulders
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> MuscleArms
    MuscleGroup.QUADS -> MuscleQuads
    MuscleGroup.HAMSTRINGS -> MuscleHamstrings
    MuscleGroup.GLUTES -> MuscleGlutes
    MuscleGroup.CALVES -> MuscleCalves
    MuscleGroup.CORE -> MuscleCore
}

fun VolumeFamily.accent(): Color = when (this) {
    VolumeFamily.CHEST -> MuscleChest
    VolumeFamily.BACK -> MuscleBack
    VolumeFamily.SHOULDERS -> MuscleShoulders
    VolumeFamily.BICEPS, VolumeFamily.TRICEPS -> MuscleArms
    VolumeFamily.QUADS -> MuscleQuads
    VolumeFamily.HAMSTRINGS -> MuscleHamstrings
    VolumeFamily.GLUTES -> MuscleGlutes
    VolumeFamily.CALVES -> MuscleCalves
    VolumeFamily.CORE -> MuscleCore
}

/**
 * App theme. Dynamic colour is deliberately off: the brand accent and the data-visualisation
 * palette must stay predictable across OEM wallpapers.
 */
@Composable
fun TrainWithMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TwmTypography,
        shapes = TwmShapes,
        content = content,
    )
}

/** Backwards-compat alias for the scaffold's theme wrapper. */
@Composable
fun AgonAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    TrainWithMeTheme(darkTheme = darkTheme, content = content)
}
