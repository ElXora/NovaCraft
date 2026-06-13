package com.novacraft.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Composition Locals ───────────────────────────────────────────────────────
val LocalAppTheme = compositionLocalOf<AppThemeConfig> { AppThemeConfig.DARK_NOVA }

enum class AppThemeConfig { DARK_NOVA, LIGHT_NOVA, ABYSS, CRIMSON, FOREST }

// ─── Dark Nova Scheme (default blue/purple) ───────────────────────────────────
private val DarkNovaColorScheme = darkColorScheme(
    primary = NovaPrimary,
    onPrimary = NovaOnPrimary,
    primaryContainer = NovaPrimaryContainer,
    onPrimaryContainer = NovaOnPrimaryContainer,
    secondary = NovaSecondary,
    onSecondary = NovaOnSecondary,
    secondaryContainer = NovaSecondaryContainer,
    onSecondaryContainer = NovaOnSecondaryContainer,
    tertiary = NovaTertiary,
    onTertiary = NovaOnTertiary,
    background = NovaDarkBackground,
    onBackground = NovaDarkOnBackground,
    surface = NovaDarkSurface,
    onSurface = NovaDarkOnSurface,
    surfaceVariant = NovaDarkSurfaceVariant,
    outline = NovaOutline,
    error = NovaError,
)

// ─── Light Nova Scheme ────────────────────────────────────────────────────────
private val LightNovaColorScheme = lightColorScheme(
    primary = NovaPrimaryLight,
    onPrimary = NovaOnPrimaryLight,
    primaryContainer = NovaPrimaryContainerLight,
    onPrimaryContainer = NovaOnPrimaryContainerLight,
    secondary = NovaSecondaryLight,
    onSecondary = NovaOnSecondaryLight,
    background = NovaLightBackground,
    onBackground = NovaLightOnBackground,
    surface = NovaLightSurface,
    onSurface = NovaLightOnSurface,
)

// ─── Abyss (deepest dark) ────────────────────────────────────────────────────
private val AbyssColorScheme = darkColorScheme(
    primary = AbyssPrimary,
    onPrimary = AbyssOnPrimary,
    primaryContainer = AbyssPrimaryContainer,
    background = AbyssBackground,
    surface = AbyssSurface,
)

@Composable
fun NovaCraftTheme(
    themeConfig: AppThemeConfig = AppThemeConfig.DARK_NOVA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeConfig == AppThemeConfig.LIGHT_NOVA -> LightNovaColorScheme
        themeConfig == AppThemeConfig.ABYSS -> AbyssColorScheme
        else -> DarkNovaColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeConfig == AppThemeConfig.LIGHT_NOVA
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaCraftTypography,
            shapes = NovaCraftShapes,
            content = content
        )
    }
}
