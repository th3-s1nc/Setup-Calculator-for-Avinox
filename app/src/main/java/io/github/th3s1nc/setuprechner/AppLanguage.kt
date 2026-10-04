package io.github.th3s1nc.setuprechner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/**
 * Sprachwahl in der App. Leer = Sprache des Systems, sonst ein Sprachkürzel ("de", "en").
 *
 * Die Wahl wird gespeichert und beim Start der Activity über [wrap] auf die Ressourcen gelegt.
 * Das funktioniert auf allen unterstützten Android-Versionen und braucht keine weitere Bibliothek.
 */
object AppLanguage {
    const val SYSTEM = ""
    private const val PREFS = "setup"
    private const val KEY = "language"

    /** Wählbare Sprachen mit ihrem Eigennamen. Neue Übersetzungen hier ergänzen. */
    val CHOICES: List<Pair<String, String>> = listOf("de" to "Deutsch", "en" to "English")

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    /** Speichert die Wahl und startet die Activity neu, damit alle Texte neu geladen werden. */
    fun choose(context: Context, tag: String) {
        if (tag == current(context)) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
        findActivity(context)?.recreate()
    }

    /** Legt die gewählte Sprache auf den Kontext. Aufruf in `attachBaseContext` der Activity. */
    fun wrap(base: Context): Context {
        val tag = current(base)
        if (tag.isEmpty()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    private fun findActivity(context: Context): Activity? {
        var c: Context? = context
        while (c != null) {
            if (c is Activity) return c
            c = (c as? ContextWrapper)?.baseContext
        }
        return null
    }
}
