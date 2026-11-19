package com.garaih.appgastos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta propia (verde azulado), para que la app se vea igual en cualquier móvil.
private val Claro = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF475569),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFB45309),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF6F8F8),
    surface = Color(0xFFF6F8F8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF2F2),
    surfaceContainerHigh = Color(0xFFE6ECEC),
    surfaceVariant = Color(0xFFE6ECEC),
    outlineVariant = Color(0xFFD5DEDE),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFCBD5E1),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFCD34D),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B1212),
    surface = Color(0xFF0B1212),
    surfaceContainerLowest = Color(0xFF070C0C),
    surfaceContainerLow = Color(0xFF111A1A),
    surfaceContainer = Color(0xFF162020),
    surfaceContainerHigh = Color(0xFF1C2828),
    surfaceVariant = Color(0xFF1C2828),
    outlineVariant = Color(0xFF2A3838),
)

private val Formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun AppGastosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Oscuro else Claro,
        shapes = Formas,
        content = content,
    )
}
