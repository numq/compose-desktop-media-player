package io.github.numq.cdmp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00696B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4FB3B5),
    onPrimaryContainer = Color(0xFF002021),

    secondary = Color(0xFF47617A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE5FF),
    onSecondaryContainer = Color(0xFF001E30),

    tertiary = Color(0xFF985C00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF311C00),

    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E5),
    onSurfaceVariant = Color(0xFF3F4949),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FB3B5),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F51),
    onPrimaryContainer = Color(0xFFA6EFF0),

    secondary = Color(0xFFB3C8E3),
    onSecondary = Color(0xFF1D3246),
    secondaryContainer = Color(0xFF334A5F),
    onSecondaryContainer = Color(0xFFCFE5FF),

    tertiary = Color(0xFFFFB95E),
    onTertiary = Color(0xFF502D00),
    tertiaryContainer = Color(0xFF724300),
    onTertiaryContainer = Color(0xFFFFDDB3),

    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E3),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E3),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC9C9),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)