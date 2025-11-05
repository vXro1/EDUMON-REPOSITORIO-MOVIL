package com.example.edumonjetcompose.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ☀️ Esquema de color claro - Fresco y moderno
private val LightColorScheme = lightColorScheme(
    // Colores primarios
    primary = AzulCielo,
    onPrimary = Blanco,
    primaryContainer = AzulCieloClaro,
    onPrimaryContainer = AzulOscuro,

    // Colores secundarios
    secondary = Fucsia,
    onSecondary = Blanco,
    secondaryContainer = FucsiaClaro,
    onSecondaryContainer = FucsiaOscuro,

    // Colores terciarios
    tertiary = VerdeLima,
    onTertiary = Blanco,
    tertiaryContainer = VerdeLimaClaro,
    onTertiaryContainer = VerdeOscuro,

    // Fondos y superficies
    background = FondoClaro,
    onBackground = GrisOscuro,
    surface = FondoCard,
    onSurface = GrisOscuro,
    surfaceVariant = FondoSecundario,
    onSurfaceVariant = GrisNeutral,
    surfaceTint = AzulCielo,

    // Inversiones
    inverseSurface = GrisMuyOscuro,
    inverseOnSurface = GrisExtraClaro,
    inversePrimary = AzulCieloClaro,

    // Estados y bordes
    error = Error,
    onError = Blanco,
    errorContainer = ErrorClaro,
    onErrorContainer = ErrorOscuro,
    outline = GrisMedio,
    outlineVariant = GrisClaro,

    // Scrim
    scrim = FondoOverlay
)

// 🌙 Esquema de color oscuro - Elegante y vibrante
private val DarkColorScheme = darkColorScheme(
    // Colores primarios
    primary = AzulCieloClaro,
    onPrimary = FondoOscuroPrimario,
    primaryContainer = AzulOscuro,
    onPrimaryContainer = AzulCieloClaro,

    // Colores secundarios
    secondary = FucsiaClaro,
    onSecondary = FondoOscuroPrimario,
    secondaryContainer = FucsiaOscuro,
    onSecondaryContainer = FucsiaClaro,

    // Colores terciarios
    tertiary = VerdeLimaClaro,
    onTertiary = FondoOscuroPrimario,
    tertiaryContainer = VerdeOscuro,
    onTertiaryContainer = VerdeLimaClaro,

    // Fondos y superficies
    background = FondoOscuroPrimario,
    onBackground = GrisExtraClaro,
    surface = FondoOscuroSecundario,
    onSurface = GrisExtraClaro,
    surfaceVariant = FondoOscuroTerciario,
    onSurfaceVariant = GrisClaro,
    surfaceTint = AzulCieloClaro,

    // Inversiones
    inverseSurface = GrisExtraClaro,
    inverseOnSurface = FondoOscuroPrimario,
    inversePrimary = AzulCielo,

    // Estados y bordes
    error = ErrorClaro,
    onError = FondoOscuroPrimario,
    errorContainer = ErrorOscuro,
    onErrorContainer = ErrorClaro,
    outline = GrisNeutral,
    outlineVariant = GrisOscuro,

    // Scrim
    scrim = FondoOverlay
)

@Composable
fun EDUMONTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color disponible en Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Configuración de insets para edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Configuración de barras del sistema
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // Colores transparentes para efecto moderno
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}