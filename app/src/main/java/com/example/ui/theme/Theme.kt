package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldOnContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = SkySecondary,
    onSecondary = SkyOnSecondary,
    secondaryContainer = SkyOnContainer,
    onSecondaryContainer = SkyContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberOnContainer,
    onTertiaryContainer = AmberContainer,
    background = SlateBackgroundDark,
    onBackground = SlateTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = SkySecondary,
    onSecondary = SkyOnSecondary,
    secondaryContainer = SkyContainer,
    onSecondaryContainer = SkyOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand colors for cohesive identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
