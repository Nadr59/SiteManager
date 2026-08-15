package com.nadr59.sitemanager.ui.theme

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
    primary = Color(0xFFD4A853),
    onPrimary = Color(0xFF07070C),
    primaryContainer = Color(0xFF2A2210),
    secondary = Color(0xFF5B8DD9),
    tertiary = Color(0xFF5BD9A8),
    background = Color(0xFF07070C),
    surface = Color(0xFF0E0E16),
    surfaceVariant = Color(0xFF14142A),
    onBackground = Color(0xFFE8E6E1),
    onSurface = Color(0xFFE8E6E1),
    onSurfaceVariant = Color(0xFF6B6980),
    outline = Color(0xFF1E1E35)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B6914),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF3D6),
    secondary = Color(0xFF3D6BB0),
    tertiary = Color(0xFF2D8B64),
    background = Color(0xFFFBF8F4),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3F0EB),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFE0DDD8)
)

@Composable
fun SiteManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
