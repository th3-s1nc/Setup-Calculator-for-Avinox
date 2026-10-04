package io.github.th3s1nc.setuprechner.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.BoostResult
import io.github.th3s1nc.setuprechner.calc.ModeResult
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.SetupInput
import io.github.th3s1nc.setuprechner.calc.SetupResult
import io.github.th3s1nc.setuprechner.calc.Zone
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Texte, Hinweise und Zahlenformate in der Sprache der geladenen Sprachdatei.
 *
 * Das Zahlenformat richtet sich nach der Sprachdatei (Eintrag `locale_tag`), nicht nach der
 * Systemsprache. So passen Text und Zahlen auch dann zusammen, wenn das Handy auf einer Sprache
 * steht, für die es keine Übersetzung gibt.
 */
class SetupText(private val res: Resources) {

    data class Note(val text: String, val warning: Boolean = false)

    private val locale: Locale = Locale.forLanguageTag(res.getString(R.string.locale_tag))
    private val decimalSeparator: Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    private fun s(id: Int, vararg args: Any): String = res.getString(id, *args)

    /** Watt gerundet mit Tausendertrennzeichen: 1.000 (de) oder 1,000 (en) */
    fun watt(x: Double): String = String.format(locale, "%,d", x.roundToLong())
    fun watt(x: Int): String = String.format(locale, "%,d", x)

    /** W/kg mit zwei Nachkommastellen */
    fun wkg(x: Double): String = String.format(locale, "%.2f", x)

    /** Gewicht oder Zielwert ohne überflüssige Nachkommastelle: 110 oder 110,5 */
    fun plain(x: Double): String =
        if (x == Math.floor(x)) x.toLong().toString()
        else x.toString().replace('.', decimalSeparator)

    fun profile(p: Profile): String = when (p) {
        Profile.ALLROUND -> s(R.string.profile_allround)
        Profile.LONG -> s(R.string.profile_long)
        Profile.POWER -> s(R.string.profile_power)
    }

    fun profileHint(p: Profile): String = when (p) {
        Profile.ALLROUND -> s(R.string.profile_allround_hint)
        Profile.LONG -> s(R.string.profile_long_hint)
        Profile.POWER -> s(R.string.profile_power_hint)
    }

    fun zone(z: Zone): String = when (z) {
        Zone.LOW -> s(R.string.zone_low)
        Zone.MID -> s(R.string.zone_mid)
        Zone.HIGH -> s(R.string.zone_high)
    }

    fun assistLevel(m: ModeResult): String =
        if (m.isRange) s(R.string.range_dash, m.alLo.toString(), m.alHi.toString()) else m.alLo.toString()

    fun percent(m: ModeResult): String =
        if (m.isRange) s(R.string.pct_range, m.pctLo.toString(), m.pctHi.toString())
        else s(R.string.pct_single, m.pctHi.toString())

    fun percent(pct: Int): String = s(R.string.pct_single, pct.toString())

    fun power(m: ModeResult): String =
        if (m.flat.roundToLong() == m.climb.roundToLong()) s(R.string.power_single, watt(m.climb))
        else s(R.string.power_range, watt(m.flat), watt(m.climb))

    /** Erste Zeile der Eckdaten: Motor und Umfang. */
    fun summaryMain(r: SetupResult, inp: SetupInput): String =
        s(R.string.summary_main, r.motor.label, if (r.ladder) s(R.string.all_modes_lower) else profile(inp.profile))

    /** Zweite Zeile der Eckdaten: Gewicht, Trittfrequenz, Eigenleistung. */
    fun summaryDetail(r: SetupResult, inp: SetupInput): String =
        s(R.string.summary_detail, plain(r.totalKg), inp.cadence.toString(), watt(inp.powerW))

    fun detailLine(m: ModeResult, inp: SetupInput): String =
        s(R.string.detail_line, percent(m), power(m), wkg(m.wkg), inp.cadence.toString())

    fun detailLine(b: BoostResult, inp: SetupInput): String =
        s(R.string.detail_line, percent(b.pct), s(R.string.power_single, watt(b.atCadence)), wkg(b.wkg), inp.cadence.toString())

    fun notes(m: ModeResult, inp: SetupInput): List<Note> {
        val out = ArrayList<Note>()
        val eps = 0.5
        val reach = minOf(m.watt.toDouble(), m.alWatt)
        if (m.nmWatt < reach - eps) {
            out.add(Note(s(R.string.note_nm, inp.cadence.toString(), watt(m.nmWatt), watt(m.watt), m.rpmForMax.toString()), true))
        }
        if (m.alWatt < m.watt - eps) {
            out.add(Note(s(R.string.note_al, watt(inp.powerW), watt(m.alWatt), watt(m.watt))))
        }
        val band = m.band
        if (band != null) {
            if (m.wkg < band.first - 0.005) {
                out.add(Note(s(R.string.note_band_below, plain(band.first), plain(band.second)), true))
            } else {
                out.add(Note(s(R.string.note_band_ok, plain(band.first), plain(band.second))))
            }
        }
        if (m.raisedToMinimum) out.add(Note(s(R.string.note_raised)))
        if (m.nmSpread) out.add(Note(s(R.string.note_spread)))
        if (out.isEmpty()) out.add(Note(s(R.string.note_full)))
        return out
    }

    fun boostNote(b: BoostResult): String = s(R.string.note_boost, watt(b.watt), b.rpmForMax.toString())

    /** Text zum Teilen oder Kopieren, eine Zeile je Modus. Ein Profilname steht als erste Zeile darüber. */
    fun share(r: SetupResult, inp: SetupInput, profileName: String = ""): String {
        val sb = StringBuilder()
        if (profileName.isNotBlank()) sb.append(profileName.trim()).append("\n")
        sb.append(s(R.string.share_header, r.motor.label, plain(r.totalKg), inp.cadence.toString(), watt(inp.powerW)))
        for (m in r.modes) {
            val al = if (m.isRange) "${m.alLo}–${m.alHi}" else "${m.alLo}"
            sb.append("\n").append(s(R.string.share_line, m.name, al, watt(m.watt), m.nm.toString()))
        }
        return sb.toString()
    }

    fun rules(r: SetupResult, inp: SetupInput): List<String> {
        val out = arrayListOf(
            s(R.string.rule_1, plain(r.totalKg)),
            s(R.string.rule_2, inp.cadence.toString()),
            s(R.string.rule_3, watt(inp.powerW)),
            s(R.string.rule_4)
        )
        if (r.ladder) out.add(s(R.string.rule_5, r.modes.take(2).joinToString(", ") { it.name }))
        return out
    }

    fun tips(r: SetupResult): List<String> =
        if (r.ladder) listOf(s(R.string.tip_ladder_1), s(R.string.tip_ladder_2), s(R.string.tip_ladder_3))
        else listOf(s(R.string.tip_four_1), s(R.string.tip_four_2))
}

/** Texte für die aktuell geladene Sprache. */
@Composable
internal fun rememberSetupText(): SetupText {
    val res = LocalContext.current.resources
    return remember(res) { SetupText(res) }
}
