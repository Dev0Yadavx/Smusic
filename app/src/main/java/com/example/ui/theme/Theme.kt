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
    primary = PixelPrimary,
    onPrimary = PixelOnPrimary,
    primaryContainer = PixelDarkSurfaceVariant,
    onPrimaryContainer = PixelPrimary,
    secondary = PixelSecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = PixelDarkSurfaceCard,
    onSecondaryContainer = PixelSecondary,
    tertiary = PixelTertiary,
    background = PixelDarkBackground,
    onBackground = PixelTextPrimary,
    surface = PixelDarkSurface,
    onSurface = PixelTextPrimary,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelTextSecondary,
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = PixelLightPrimary,
    onPrimary = PixelLightOnPrimary,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    background = PixelLightBackground,
    onBackground = PixelLightTextPrimary,
    surface = PixelLightSurface,
    onSurface = PixelLightTextPrimary,
    surfaceVariant = PixelLightSurfaceVariant,
    onSurfaceVariant = PixelLightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

@Composable
fun SMusicTheme(
    darkTheme: Boolean = true, // Default to sleek premium dark theme
    dynamicColor: Boolean = true,
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
