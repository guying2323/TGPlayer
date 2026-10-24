package com.tgplayer.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Theme ported from Findroid's ColorDark/ColorLight and Shape definitions
 * (https://github.com/jarnedemeulemeester/findroid, GPL-3.0). Dynamic color is
 * on by default on Android 12+, exactly like FindroidTheme.
 */
private val DarkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA4C9FE),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00315C),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1F4876),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD3E3FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBCC7DB),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF263141),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3C4758),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E3F8),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD9BDE3),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3C2947),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF543F5E),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF5D9FF),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    background = androidx.compose.ui.graphics.Color(0xFF111318),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1E2E8),
    surface = androidx.compose.ui.graphics.Color(0xFF111318),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1E2E8),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC3C6CF),
    outline = androidx.compose.ui.graphics.Color(0xFF8D9199),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFFE1E2E8),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF2E3035),
    inversePrimary = androidx.compose.ui.graphics.Color(0xFF3A608F),
    surfaceDim = androidx.compose.ui.graphics.Color(0xFF111318),
    surfaceBright = androidx.compose.ui.graphics.Color(0xFF37393E),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF0C0E13),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF191C20),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1D2024),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF272A2F),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF32353A),
)

private val LightScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3A608F),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD3E3FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001C39),
    secondary = androidx.compose.ui.graphics.Color(0xFF545F70),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E3F8),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF111C2B),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6D5677),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF5D9FF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF261430),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF191C20),
    surface = androidx.compose.ui.graphics.Color(0xFFF8F9FF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF191C20),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDFE2EB),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    outline = androidx.compose.ui.graphics.Color(0xFF73777F),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFC3C6CF),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFF2E3035),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFFEFF0F7),
    inversePrimary = androidx.compose.ui.graphics.Color(0xFFA4C9FE),
    surfaceDim = androidx.compose.ui.graphics.Color(0xFFD8DAE0),
    surfaceBright = androidx.compose.ui.graphics.Color(0xFFF8F9FF),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF2F3FA),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEDEDF4),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE7E8EE),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE1E2E8),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

object TmSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val page = 24.dp
    val tvPage = 48.dp
}

object TmRadius {
    val sm = 10.dp
    val md = 12.dp
    val lg = 16.dp
    val pill = 999.dp
}

@Composable
fun TmPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colorScheme, shapes = AppShapes, content = content)
}
