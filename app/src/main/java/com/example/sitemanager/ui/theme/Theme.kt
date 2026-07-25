package com.example.sitemanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Blue900,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue900,
    secondary = Orange500,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Gray100,
    background = Gray100,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = Gray600
)

private val DarkColors = darkColorScheme(
    primary = Blue200,
    onPrimary = Blue900,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue200,
    secondary = Orange500,
    surface = DarkSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2C),
    background = DarkBackground,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0)
)

@Composable
fun SiteManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
