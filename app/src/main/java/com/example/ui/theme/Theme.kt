package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BarberDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = ObsidianDark,
    primaryContainer = AmberGoldDark,
    onPrimaryContainer = AmberGoldLight,
    secondary = AmberGoldLight,
    onSecondary = ObsidianDark,
    tertiary = SkyBlue,
    onTertiary = ObsidianDark,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CrimsonRed,
    onError = TextPrimary
)

private val BarberLightColorScheme = lightColorScheme(
    primary = AmberGoldDark,
    onPrimary = TextPrimary,
    primaryContainer = AmberGoldLight,
    onPrimaryContainer = ObsidianDark,
    secondary = AmberGold,
    onSecondary = TextPrimary,
    background = Color(0xFFF9F9FB),
    onBackground = Color(0xFF141316),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141316),
    surfaceVariant = Color(0xFFF0EFF4),
    onSurfaceVariant = Color(0xFF6B6974),
    outline = Color(0xFFE2E0E7),
    error = CrimsonRed,
    onError = TextPrimary
)

@Composable
fun BarberCraftTheme(
    darkTheme: Boolean = true, // Luxury dark by default for barber lounge ambiance
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BarberDarkColorScheme else BarberLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
