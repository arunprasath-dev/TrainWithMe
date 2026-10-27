package com.trainwithme.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------------------------
// Brand palette. A single accent (ember orange) carries "effort"; everything else is neutral so
// data (charts, numbers) stays legible. Contrast ratios are checked in the UI tests' checklist.
// ---------------------------------------------------------------------------------------------

private val Ember = Color(0xFFFF6A3D)
private val EmberDark = Color(0xFFC2410C)
private val EmberContainerDark = Color(0xFF3A1B10)
private val EmberContainerLight = Color(0xFFFFE3D8)

private val Ink = Color(0xFF0E1116)
private val InkElevated = Color(0xFF161B22)
private val InkCard = Color(0xFF1C222B)
private val InkBorder = Color(0xFF2A313C)
private val Paper = Color(0xFFFDFCFB)
private val PaperCard = Color(0xFFFFFFFF)
private val PaperBorder = Color(0xFFE6E1DC)
private val TextDark = Color(0xFFE9EDF3)
private val TextMutedDark = Color(0xFF9AA5B5)
private val TextLight = Color(0xFF12161C)
private val TextMutedLight = Color(0xFF5A6675)

private val Success = Color(0xFF34D399)
private val Warning = Color(0xFFFBBF24)
private val Danger = Color(0xFFF87171)
private val Info = Color(0xFF60A5FA)

private val DarkScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF1A0B05),
    primaryContainer = EmberContainerDark,
    onPrimaryContainer = Color(0xFFFFD9C9),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF06202B),
    secondaryContainer = Color(0xFF17323D),
    onSecondaryContainer = Color(0xFFD3EEFB),
    tertiary = Success,
    onTertiary = Color(0xFF04170F),
    background = Ink,
    onBackground = TextDark,
    surface = Ink,
    onSurface = TextDark,
    surfaceVariant = InkCard,
    onSurfaceVariant = TextMutedDark,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkCard,
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
    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEDFA),
    onSecondaryContainer = Color(0xFF04283B),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    background = Paper,
    onBackground = TextLight,
    surface = Paper,
    onSurface = TextLight,
    surfaceVariant = PaperCard,
    onSurfaceVariant = TextMutedLight,
    surfaceContainer = Color(0xFFF6F3F0),
    surfaceContainerHigh = PaperCard,
    outline = PaperBorder,
    outlineVariant = Color(0xFFF0EBE6),
    error = Color(0xFFB91C1C),
    onError = Color.White,
)

/** Semantic colours that Material's scheme does not model. */
object TrainWithMeColors {
    val success: Color @Composable get() = if (isSystemInDarkTheme()) Success else Color(0xFF047857)
    val warning: Color @Composable get() = if (isSystemInDarkTheme()) Warning else Color(0xFFB45309)
    val danger: Color @Composable get() = if (isSystemInDarkTheme()) Danger else Color(0xFFB91C1C)
    val info: Color @Composable get() = if (isSystemInDarkTheme()) Info else Color(0xFF1D4ED8)
    val chartLine: Color @Composable get() = MaterialTheme.colorScheme.primary
    val chartFill: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
}

private val TrainWithMeTypography = Typography(
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
        letterSpacing = 0.6.sp,
    ),
)

private val TrainWithMeShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

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
        typography = TrainWithMeTypography,
        shapes = TrainWithMeShapes,
        content = content,
    )
}
