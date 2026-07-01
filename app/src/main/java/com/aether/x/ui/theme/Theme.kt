package com.aether.x.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AetherXLightScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    secondaryContainer = BrandSecondaryContainer,
    tertiary = BrandTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
)

private val AetherXDarkScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF1C1B5E),
    primaryContainer = Color(0xFF34339A),
    onPrimaryContainer = BrandPrimaryContainer,
    secondary = Color(0xFFC5C4DD),
    secondaryContainer = Color(0xFF444559),
    tertiary = Color(0xFFE5B8FF),
    tertiaryContainer = Color(0xFF623A7A),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
)

/**
 * Tema utama AetherX.
 *
 * @param useDynamicColor jika true & perangkat Android 12+, palet warna diambil
 *   dari wallpaper sistem (Material You). Jika tidak tersedia/dinonaktifkan,
 *   jatuh ke skema bawaan AetherX yang default-nya putih bersih.
 */
@Composable
fun AetherXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
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
        content = content,
    )
}
