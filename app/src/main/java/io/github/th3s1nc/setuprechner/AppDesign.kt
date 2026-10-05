package io.github.th3s1nc.setuprechner

import android.content.Context

/**
 * Wahl zwischen dunklem und hellem Design (Info > Design). Dunkel ist der Standard.
 *
 * Die Wahl wird gespeichert. Beim Umschalten startet die Activity neu, damit auch die Symbole
 * in Status- und Navigationsleiste zur neuen Hintergrundfarbe passen.
 */
object AppDesign {
    private const val PREFS = "setup"
    private const val KEY = "design"
    private const val LIGHT = "light"
    private const val DARK = "dark"

    fun isLight(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, DARK) == LIGHT

    fun choose(context: Context, light: Boolean) {
        if (light == isLight(context)) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, if (light) LIGHT else DARK).apply()
        AppLanguage.findActivity(context)?.recreate()
    }
}
