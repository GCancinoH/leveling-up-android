package com.gcancino.levelingup.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val purpleBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF6650A4),
        Color(0xFF4A69BD)
    )
)

val Blue20 = Color(0xFF4A69BD)
val Blue40 = Color(0xFF1E88E5)

val BackgroundColor = Color(0xFF030303)

val LevelingUpDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50), // A shade of green that might represent the positive income/good financial health
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E88E5), // A shade of blue for general containers
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00ACC1), // A teal shade, perhaps for secondary actions or emphasis
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF26A69A), // Another teal/greenish shade
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFFA726), // An orange shade, potentially for warnings or highlights
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFCC80),
    onTertiaryContainer = Color.Black,
    error = Color(0xFFEF5350), // Standard red for errors
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color.Black,
    background = Color(0xFF1A262C), // The very dark background from your image
    onBackground = Color(0xFFE0E0E0), // Light grey text on the dark background
    surface = Color(0xFF263238), // Slightly lighter dark for cards/surfaces (like the balance and financial health cards)
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF424242), // Another surface variant
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF616161), // Outline color
    inverseOnSurface = Color(0xFF263238),
    inverseSurface = Color(0xFFE0E0E0),
    inversePrimary = Color(0xFF81C784),
    surfaceTint = Color(0xFF4CAF50),
    scrim = Color(0x99000000), // Standard scrim for modal barriers
)

val LevelingUpLightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF00ACC1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFFFA726),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFECB3),
    onTertiaryContainer = Color(0xFFE65100),
    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD),
    inverseOnSurface = Color(0xFFF5F5F5),
    inverseSurface = Color(0xFF303030),
    inversePrimary = Color(0xFF4CAF50),
    surfaceTint = Color(0xFF4CAF50),
    scrim = Color(0x99000000),
)
