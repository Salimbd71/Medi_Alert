package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.ui.strings.LocalAppStrings
import com.example.ui.strings.stringsFor

private val MediLightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = SkyTint,
    onPrimaryContainer = TealPrimary,
    secondary = MintAccent,
    onSecondary = Color(0xFF062E2B),
    secondaryContainer = Color(0xFFD2F5F0),
    onSecondaryContainer = Color(0xFF06403B),
    tertiary = CoralAlert,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5E0),
    onTertiaryContainer = Color(0xFF7D1E12),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6FBFC),
    surfaceContainer = Color(0xFFEAF5F7),
    surfaceContainerHigh = Color(0xFFE1EFF2),
    surfaceContainerHighest = Color(0xFFD6E9EC),
    outline = Color(0xFFB3D2D6),
    error = CoralAlert,
    onError = Color.White
)

private val MediDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF032E33),
    primaryContainer = Color(0xFF104952),
    onPrimaryContainer = Color(0xFFB8F3F9),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF042D28),
    secondaryContainer = Color(0xFF0E453F),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFF8A78),
    onTertiary = Color(0xFF470D05),
    tertiaryContainer = Color(0xFF632018),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF061013),
    surfaceContainerLow = Color(0xFF0C1B20),
    surfaceContainer = Color(0xFF11252C),
    surfaceContainerHigh = Color(0xFF162E36),
    surfaceContainerHighest = Color(0xFF1C3841),
    outline = Color(0xFF284E57),
    error = Color(0xFFFF8A78),
    onError = Color(0xFF470D05)
)

val MediAlertShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MediAlertTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.ENGLISH,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MediDarkColorScheme
        else -> MediLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppStrings provides stringsFor(language)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildMediAlertTypography(language),
            shapes = MediAlertShapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                content = content
            )
        }
    }
}
