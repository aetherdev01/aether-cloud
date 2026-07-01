package com.aether.x.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Skema gelap kustom AetherX — dasar hitam pekat + aksen biru pucat,
// dipakai sebagai default (bukan lagi Material You bawaan Android).
private val AetherXDarkScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccentBlue,
    primaryContainer = SurfaceCardAlt,
    onPrimaryContainer = AccentBlue,
    secondary = AccentGold,
    onSecondary = Color(0xFF241C04),
    secondaryContainer = AccentGoldDim,
    onSecondaryContainer = AccentGold,
    tertiary = AccentBlueSoft,
    tertiaryContainer = SurfaceRaised,
    onTertiaryContainer = AccentBlueSoft,
    background = BgBase,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextOnCard,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = StrokeSubtle,
    outlineVariant = StrokeSubtle,
    error = AccentRed,
    onError = Color(0xFF2B0704),
)

// Skema terang tetap disediakan (fallback), tapi referensi desain fokus ke dark.
private val AetherXLightScheme = lightColorScheme(
    primary = Color(0xFF3D5FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E7FF),
    onPrimaryContainer = Color(0xFF10265E),
    secondary = Color(0xFFB8862B),
    secondaryContainer = Color(0xFFFFE9BE),
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF0F6),
    onBackground = Color(0xFF15161C),
    onSurface = Color(0xFF15161C),
    onSurfaceVariant = Color(0xFF54566A),
    outline = Color(0xFFDADCE6),
)

private val AetherXShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema utama AetherX.
 *
 * UI direwrite total mengikuti referensi desain: dasar gelap pekat + aksen
 * biru pucat, tipografi tebal, kartu besar dengan sudut membulat. Dynamic
 * color (Material You) dimatikan secara default supaya tampilan tidak lagi
 * mengikuti wallpaper sistem dan konsisten dengan identitas AetherX.
 */
@Composable
fun AetherXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor && dynamicAvailable && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && dynamicAvailable && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> AetherXDarkScheme
        else -> AetherXLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherXTypography,
        shapes = AetherXShapes,
        content = content,
    )
}
