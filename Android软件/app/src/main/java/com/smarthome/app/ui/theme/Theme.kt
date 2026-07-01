package com.smarthome.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DoubaoOrange,
    onPrimary = Color.White,
    primaryContainer = DoubaoOrangeBg,
    onPrimaryContainer = DoubaoOrange,
    secondary = Secondary40,
    onSecondary = Color.White,
    secondaryContainer = DoubaoPeach,
    onSecondaryContainer = Secondary60,
    tertiary = Tertiary40,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF2D2D2D),
    surface = LightSurface,
    onSurface = Color(0xFF2D2D2D),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6B6B6B),
    error = StatusRed,
    onError = Color.White,
    outline = Color(0xFFE0D5CC),
    outlineVariant = Color(0xFFF0E8E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = DoubaoOrangeLight,
    onPrimary = Color(0xFF3D1A00),
    primaryContainer = DoubaoOrange,
    secondary = Secondary80,
    onSecondary = Color(0xFF32263F),
    tertiary = Tertiary80,
    background = DarkBackground,
    onBackground = Color(0xFFE8E0D8),
    surface = DarkSurface,
    onSurface = Color(0xFFE8E0D8),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC0B8B0),
    error = StatusRed,
    onError = Color.White
)

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        typography = SmartHomeTypography,
        shapes = SmartHomeShapes,
        content = content
    )
}
