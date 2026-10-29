package dev.engram.edge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Calibrated for a tiny phone & Full Android 15 watch (~2.0"–2.4" diagonal) reporting `sw360dp` to `sw426dp`.
 *
 * Because `360dp`–`426dp` squeezed onto a ~2-inch physical panel has high effective density,
 * standard 14sp text and 48dp buttons become microscopic. EngramMicroTheme enforces:
 *  - True AMOLED pitch-black background (`#000000`) for battery savings & outdoor glare contrast
 *  - 20sp–26sp typography and 76dp–88dp touch target heights
 */
object EngramDimens {
    val PrimaryTouchHeight = 80.dp
    val SecondaryTouchHeight = 68.dp
    val CardCornerRadius = 22.dp
    val ScreenPadding = 12.dp
}

object EngramColors {
    val AmoledBlack = Color(0xFF000000)
    val CardSurface = Color(0xFF10161B)
    val CardSurfaceElevated = Color(0xFF18222A)
    val EmeraldEdge = Color(0xFF00E676)
    val CyanCloud = Color(0xFF18FFFF)
    val AmberWarn = Color(0xFFFFCA28)
    val CoralDanger = Color(0xFFFF5252)
    val TextPrimary = Color(0xFFF4F8FB)
    val TextMuted = Color(0xFFA2B4C0)
}

private val EngramColorScheme = darkColorScheme(
    primary = EngramColors.EmeraldEdge,
    onPrimary = Color(0xFF002411),
    secondary = EngramColors.CyanCloud,
    onSecondary = Color(0xFF002529),
    tertiary = EngramColors.AmberWarn,
    background = EngramColors.AmoledBlack,
    onBackground = EngramColors.TextPrimary,
    surface = EngramColors.CardSurface,
    onSurface = EngramColors.TextPrimary,
    surfaceVariant = EngramColors.CardSurfaceElevated,
    onSurfaceVariant = EngramColors.TextMuted,
    error = EngramColors.CoralDanger
)

private val EngramTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp
    )
)

@Composable
fun EngramMicroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EngramColorScheme,
        typography = EngramTypography,
        content = content
    )
}
