package io.github.th3s1nc.setuprechner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Farbschema: dunkel mit orangem Akzent, auf Wunsch hell (Info > Design).
 * Die App folgt nicht der hell/dunkel-Einstellung des Systems.
 */
private val DarkColors = darkColorScheme(
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

private val LightColors = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F2F4),
    onBackground = Color(0xFF16181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFE6E8EC),
    onSurfaceVariant = Color(0xFF5B6470),
    outline = Color(0xFFD5D9DF),
    outlineVariant = Color(0xFFD5D9DF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

/** Hintergrund des Fensters je Design, als ARGB-Wert für die Activity */
const val DARK_BACKGROUND: Int = 0xFF0E0E10.toInt()
const val LIGHT_BACKGROUND: Int = 0xFFF1F2F4.toInt()

// Vom gewählten Design gesetzt. Beim Umschalten startet die Activity neu, deshalb genügt ein einfacher Wert.
private var lightDesign = false

/** Farbe für BOOST: neutral, weil die Stufe fest ist und keine eigene Modusfarbe braucht. */
val BoostColor: Color get() = if (lightDesign) Color(0xFF3A4049) else Color(0xFFD9D9DE)

/** Hinweise, die auf eine Grenze aufmerksam machen. */
val WarningColor: Color get() = if (lightDesign) Color(0xFF9A5B00) else Color(0xFFF0B458)

/**
 * Farben der Modi. ECO, AUTO, TRAIL und TURBO wie in der Avinox Ride App,
 * die Zusatzmodi nach den Farbcodes der Blätter "Generelles Setup".
 */
private val ModeColors: Map<String, Long> = mapOf(
    "ECO" to 0xFF2FCB7A,
    "AUTO" to 0xFF3D9BF5,
    "TRAIL" to 0xFFF2A93B,
    "TURBO" to 0xFFFF5A2B,
    "LOW" to 0xFFF4E04D,
    "FLAT" to 0xFFA8E05F,
    "MTWOS" to 0xFFA8E05F,
    "ROAD" to 0xFF4CC9F0,
    "POWER" to 0xFFF06AC0,
    "ALL IN" to 0xFFF06AC0,
    "BEAST" to 0xFFA97BE8
)
private const val DEFAULT_MODE_COLOR: Long = 0xFF3D9BF5

fun modeColor(name: String): Color = Color(ModeColors[name] ?: DEFAULT_MODE_COLOR)

/** Farbe eines Modus als "#RRGGBB" für die Druckansicht */
fun modeHex(name: String): String = String.format("#%06X", (ModeColors[name] ?: DEFAULT_MODE_COLOR) and 0xFFFFFF)

@Composable
fun SetupTheme(light: Boolean = false, content: @Composable () -> Unit) {
    lightDesign = light
    MaterialTheme(colorScheme = if (light) LightColors else DarkColors, content = content)
}
