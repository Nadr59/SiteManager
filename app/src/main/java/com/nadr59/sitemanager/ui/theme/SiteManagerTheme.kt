package com.nadr59.sitemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══ الثيمات المتاحة ═══
enum class AppTheme(val displayName: String, val emoji: String) {
    DYNAMIC("ديناميكي", "🎨"),
    BLUE("أزرق", "🔵"),
    GREEN("أخضر", "🟢"),
    PURPLE("بنفسجي", "🟣"),
    ORANGE("برتقالي", "🟠"),
    RED("أحمر", "🔴"),
    TEAL("فيروزي", "🩵"),
    DARK("داكن", "⚫")
}

// ═══ ألوان الثيمات ═══
private val BlueLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF0288D1),
    tertiary = Color(0xFF0097A7),
    background = Color(0xFFF8FAFF),
    surface = Color(0xFFFFFFFF)
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF001F5C),
    primaryContainer = Color(0xFF002F85),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22)
)

private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF388E3C),
    tertiary = Color(0xFF00796B),
    background = Color(0xFFF1F8F1),
    surface = Color(0xFFFFFFFF)
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFA5D6A7),
    background = Color(0xFF0D1F0E),
    surface = Color(0xFF1A2E1B)
)

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    secondary = Color(0xFF8E24AA),
    tertiary = Color(0xFF7B1FA2),
    background = Color(0xFFF9F4FF),
    surface = Color(0xFFFFFFFF)
)

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF4A148C),
    primaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFFE040FB),
    background = Color(0xFF1A0A2E),
    surface = Color(0xFF25103D)
)

private val OrangeLight = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFFF57C00),
    tertiary = Color(0xFFFF8F00),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFFFFF)
)

private val OrangeDark = darkColorScheme(
    primary = Color(0xFFFFCC80),
    onPrimary = Color(0xFF3E1A00),
    primaryContainer = Color(0xFF5C2B00),
    secondary = Color(0xFFFFB74D),
    background = Color(0xFF1F0E00),
    surface = Color(0xFF2E1600)
)

private val RedLight = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    secondary = Color(0xFFD32F2F),
    tertiary = Color(0xFFB71C1C),
    background = Color(0xFFFFF5F5),
    surface = Color(0xFFFFFFFF)
)

private val RedDark = darkColorScheme(
    primary = Color(0xFFEF9A9A),
    onPrimary = Color(0xFF7F0000),
    primaryContainer = Color(0xFF7F0000),
    secondary = Color(0xFFE57373),
    background = Color(0xFF1F0000),
    surface = Color(0xFF2E0000)
)

private val TealLight = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF00796B),
    tertiary = Color(0xFF0097A7),
    background = Color(0xFFF0FAFA),
    surface = Color(0xFFFFFFFF)
)

private val TealDark = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003D36),
    primaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF001A17),
    surface = Color(0xFF002820)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D2137),
    primaryContainer = Color(0xFF1565C0),
    secondary = Color(0xFFB39DDB),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun SiteManagerTheme(
    themePreference: AppTheme = AppTheme.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (themePreference) {
        AppTheme.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) BlueDark else BlueLight
            }
        }
        AppTheme.BLUE -> if (darkTheme) BlueDark else BlueLight
        AppTheme.GREEN -> if (darkTheme) GreenDark else GreenLight
        AppTheme.PURPLE -> if (darkTheme) PurpleDark else PurpleLight
        AppTheme.ORANGE -> if (darkTheme) OrangeDark else OrangeLight
        AppTheme.RED -> if (darkTheme) RedDark else RedLight
        AppTheme.TEAL -> if (darkTheme) TealDark else TealLight
        AppTheme.DARK -> DarkScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
