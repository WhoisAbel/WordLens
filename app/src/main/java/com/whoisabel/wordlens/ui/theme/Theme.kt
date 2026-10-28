package com.whoisabel.wordlens.ui.theme

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
    primary = IndigoLight,
    secondary = VioletLight,
    tertiary = PinkLight,
    primaryContainer = Color(0xFF2E2A6B),
    background = Color(0xFF0E0E18),
    surface = Color(0xFF0E0E18),
    surfaceContainerLow = Color(0xFF191928),
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    secondary = Violet,
    tertiary = Pink,
    primaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFFF6F6FB),
    surface = Color(0xFFF6F6FB),
    surfaceContainerLow = Color.White,
)

@Composable
fun WordLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand colors look the same on every phone
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
        typography = Typography,
        content = content
    )
}
