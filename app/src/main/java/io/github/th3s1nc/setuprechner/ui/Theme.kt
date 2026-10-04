package io.github.th3s1nc.setuprechner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Farbschema: durchgehend dunkel mit orangem Akzent.
 * Die App folgt nicht mehr der hell/dunkel-Einstellung des Systems.
 */
private val AppColors = darkColorScheme(
    primary = Color(0xFFFF5A1F),
    onPrimary = Color(0xFF1A0A02),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF18181B),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF242428),
    onSurfaceVariant = Color(0xFF9A9AA2),
    outline = Color(0xFF2E2E33),
    outlineVariant = Color(0xFF2E2E33),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF1A0A02)
)

/** Farbe für BOOST: hell und neutral, weil die Stufe fest ist und keine eigene Modusfarbe braucht. */
val BoostColor = Color(0xFFD9D9DE)

/** Hinweise, die auf eine Grenze aufmerksam machen. */
val WarningColor = Color(0xFFF0B458)

/**
 * Farben der Modi. ECO, AUTO, TRAIL und TURBO wie in der Avinox Ride App,
 * die Zusatzmodi nach den Farbcodes der Blätter "Generelles Setup".
 */
fun modeColor(name: String): Color = when (name) {
    "ECO" -> Color(0xFF2FCB7A)
    "AUTO" -> Color(0xFF3D9BF5)
    "TRAIL" -> Color(0xFFF2A93B)
    "TURBO" -> Color(0xFFFF5A2B)
    "LOW" -> Color(0xFFF4E04D)
    "FLAT", "MTWOS" -> Color(0xFFA8E05F)
    "ROAD" -> Color(0xFF4CC9F0)
    "POWER", "ALL IN" -> Color(0xFFF06AC0)
    "BEAST" -> Color(0xFFA97BE8)
    else -> Color(0xFF3D9BF5)
}

@Composable
fun SetupTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
