package io.github.th3s1nc.setuprechner.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.Battery
import io.github.th3s1nc.setuprechner.calc.BoostResult
import io.github.th3s1nc.setuprechner.calc.ModeResult
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.SetupCalculator
import io.github.th3s1nc.setuprechner.calc.SetupInput
import io.github.th3s1nc.setuprechner.calc.SetupResult
import io.github.th3s1nc.setuprechner.calc.Zone
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    fun plain(x: Double): String {
        // Auf zwei Stellen runden, damit aus 23,4 + 87,3 nicht 110,69999999999999 wird
        val r = Math.round(x * 100.0) / 100.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString().replace('.', decimalSeparator)
    }

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

    /** Geschätzte Steigung, die sich mit Eigenleistung plus Motorleistung bei einem Tempo in km/h halten lässt. */
    fun grade(motorW: Double, inp: SetupInput, speed: Int): String {
        val g = SetupCalculator.grade(inp.powerW + motorW, inp.totalKg, speed.toDouble())
        if (g.isNaN() || g.isInfinite() || g > 40.0) return s(R.string.grade_max, "40")
        if (g < 1.0) return s(R.string.grade_min, "1")
        return s(R.string.grade_value, Math.round(g).toString())
    }

    /** Letzte Spalte einer Zeile: W/kg oder, wenn [speed] gesetzt ist, die geschätzte Steigung bei diesem Tempo. */
    fun last(motorW: Double, wkgValue: Double, inp: SetupInput, speed: Int?): String =
        if (speed == null) s(R.string.wkg_value, wkg(wkgValue)) else grade(motorW, inp, speed)

    /** Wert der letzten Tabellenspalte ohne Einheit bei W/kg */
    fun lastCell(motorW: Double, wkgValue: Double, inp: SetupInput, speed: Int?): String =
        if (speed == null) wkg(wkgValue) else grade(motorW, inp, speed)

    fun detailLine(m: ModeResult, inp: SetupInput, speed: Int? = null): String =
        if (speed == null) s(R.string.detail_line, percent(m), power(m), wkg(m.wkg), inp.cadence.toString())
        else s(R.string.detail_line_grade, percent(m), power(m), grade(m.climb, inp, speed), inp.cadence.toString())

    fun detailLine(b: BoostResult, inp: SetupInput, speed: Int? = null): String {
        val p = s(R.string.power_single, watt(b.atCadence))
        return if (speed == null) s(R.string.detail_line, percent(b.pct), p, wkg(b.wkg), inp.cadence.toString())
        else s(R.string.detail_line_grade, percent(b.pct), p, grade(b.atCadence, inp, speed), inp.cadence.toString())
    }

    /** [next] ist der nächststärkere Modus. Ist dieser von Hand schwächer eingestellt, gibt es einen Hinweis. */
    fun notes(m: ModeResult, inp: SetupInput, next: ModeResult? = null): List<Note> {
        val out = ArrayList<Note>()
        val eps = 0.5
        val reach = minOf(m.watt.toDouble(), m.alWatt)
        if (next != null && m.watt > next.watt) out.add(Note(s(R.string.note_over, next.name), true))
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
        if (m.nmSpread && m.dNm == 0) out.add(Note(s(R.string.note_spread)))
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
            val line = s(R.string.share_line, m.name, al, watt(m.watt), m.nm.toString())
            sb.append("\n").append(if (m.adjusted) s(R.string.share_line_adjusted, line) else line)
        }
        return sb.toString()
    }

    /** Erste Zeile des Texts zum Teilen, auch als Begleittext für den Link */
    fun shareHeader(r: SetupResult, inp: SetupInput): String =
        s(R.string.share_header, r.motor.label, plain(r.totalKg), inp.cadence.toString(), watt(inp.powerW))

    /** Kurze Kennzeile eines Setups für den Vergleich */
    fun compareLine(r: SetupResult, inp: SetupInput): String =
        s(R.string.cmp_line, "Avinox " + r.motor.label, plain(r.totalKg), inp.cadence.toString(), watt(inp.powerW))

    fun today(): String = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    private fun html(t: String): String =
        t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /**
     * Setup-Karte zum Drucken als eigenständige HTML-Seite: Eckdaten, alle Stufen mit Kästchen zum
     * Abhaken, Quelle. Immer hell wie Papier, unabhängig vom Design der App.
     */
    fun sheetHtml(r: SetupResult, inp: SetupInput, profileName: String, speed: Int?, date: String): String {
        val sb = StringBuilder()
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>")
        sb.append(SHEET_CSS)
        sb.append("</style></head><body><h1>").append(html(s(R.string.sheet_title, r.motor.label))).append("</h1><p class=\"sub\">")
        if (profileName.isNotBlank()) sb.append(html(profileName.trim())).append(" · ")
        sb.append(html(date)).append("</p><dl>")
        val facts = ArrayList<Pair<String, String>>()
        facts.add(s(R.string.sheet_total) to s(R.string.sheet_weight, plain(r.totalKg), plain(inp.bikeKg), plain(inp.riderKg)))
        facts.add(s(R.string.rider_power) to s(R.string.power_single, watt(inp.powerW)))
        facts.add(s(R.string.cadence_title) to inp.cadence.toString() + " " + s(R.string.unit_rpm))
        facts.add(s(R.string.scope) to if (r.ladder) s(R.string.scope_all) else s(R.string.scope_four) + " · " + profile(inp.profile))
        val battery: Battery? = inp.battery
        if (r.motor.needsBattery && battery != null) facts.add(s(R.string.battery) to battery.label)
        for ((k, v) in facts) sb.append("<div><dt>").append(html(k)).append("</dt><dd>").append(html(v)).append("</dd></div>")
        sb.append("</dl><table><thead><tr><th></th><th>").append(html(s(R.string.col_mode))).append("</th><th>").append(html(s(R.string.sheet_al)))
            .append("</th><th>").append(html(s(R.string.sheet_watt))).append("</th><th>").append(html(s(R.string.sheet_nm))).append("</th><th>")
            .append(html(s(R.string.sheet_motor_at, inp.cadence.toString()))).append("</th><th>")
            .append(html(if (speed == null) s(R.string.unit_wkg) else s(R.string.th_grade))).append("</th></tr></thead><tbody>")
        var any = false
        var zone: Zone? = null
        for (m in r.modes) {
            if (m.adjusted) any = true
            val z = m.zone
            if (r.ladder && z != null && z != zone) {
                zone = z
                sb.append("<tr class=\"zone\"><td colspan=\"7\">").append(html(zone(z))).append("</td></tr>")
            }
            sb.append("<tr><td class=\"box\"><i></i></td><td><i class=\"sw\" style=\"background:").append(modeHex(m.name)).append("\"></i>")
                .append(html(m.name)).append(if (m.adjusted) " *" else "").append("</td><td>").append(html(assistLevel(m)))
                .append("<small>").append(html(percent(m))).append("</small></td><td>").append(watt(m.watt)).append("</td><td>").append(m.nm)
                .append("</td><td class=\"soft\">").append(html(power(m))).append("</td><td class=\"soft\">")
                .append(html(last(m.climb, m.wkg, inp, speed))).append("</td></tr>")
        }
        val b = r.boost
        sb.append("<tr><td></td><td><i class=\"sw\" style=\"background:#9aa0a8\"></i>BOOST<small>").append(html(s(R.string.tag_fixed)))
            .append("</small></td><td>15<small>").append(html(percent(b.pct))).append("</small></td><td>").append(watt(b.watt)).append("</td><td>")
            .append(b.nm).append("</td><td class=\"soft\">").append(html(s(R.string.power_single, watt(b.atCadence)))).append("</td><td class=\"soft\">")
            .append(html(last(b.atCadence, b.wkg, inp, speed))).append("</td></tr></tbody></table><div class=\"foot\">")
        if (any) sb.append("<p>* ").append(html(s(R.string.sheet_adjusted))).append("</p>")
        if (speed != null) sb.append("<p>").append(html(s(R.string.grade_short, speed.toString(), watt(inp.powerW), plain(r.totalKg)))).append("</p>")
        sb.append("<p>").append(html(s(R.string.source_tune))).append("</p><p>").append(html(s(R.string.source_text))).append(" ")
            .append(html(s(R.string.source_consent))).append("</p><p>").append(html(Exchange.SITE.removePrefix("https://").removeSuffix("/")))
            .append("</p></div></body></html>")
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

private const val SHEET_CSS = "body{font-family:sans-serif;color:#111418;background:#fff;margin:0;padding:24px;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
    "h1{font-size:22px;margin:0}.sub{color:#555c66;font-size:13px;margin:2px 0 14px}" +
    "dl{display:flex;flex-wrap:wrap;gap:8px 22px;margin:0 0 14px;padding:10px 0;border-top:1px solid #c9ced6;border-bottom:1px solid #c9ced6}" +
    "dt{font-size:10px;font-weight:bold;letter-spacing:.06em;text-transform:uppercase;color:#555c66}dd{margin:2px 0 0;font-size:14px;font-weight:bold}" +
    "table{width:100%;border-collapse:collapse}" +
    "th{font-size:10px;letter-spacing:.05em;text-transform:uppercase;color:#555c66;text-align:right;padding:6px;border-bottom:2px solid #111418;vertical-align:bottom}" +
    "td{text-align:right;padding:8px 6px;border-bottom:1px solid #c9ced6;font-size:16px;font-weight:bold;vertical-align:middle}" +
    "td small{display:block;font-size:11px;font-weight:normal;color:#555c66}" +
    "th:nth-child(2),td:nth-child(2){text-align:left;white-space:nowrap}td.soft{font-weight:normal;font-size:14px;color:#333a44}" +
    "td.box{width:24px;padding-right:0;text-align:left}td.box i{display:inline-block;width:14px;height:14px;border:1.5px solid #111418;border-radius:3px;vertical-align:middle}" +
    ".sw{display:inline-block;width:11px;height:11px;border-radius:3px;margin-right:7px;border:1px solid rgba(0,0,0,.25);box-sizing:border-box}" +
    "tr.zone td{text-align:left;font-size:10px;letter-spacing:.06em;text-transform:uppercase;color:#555c66;background:#eef0f3;padding:4px 6px}" +
    ".foot{font-size:11px;line-height:1.4;color:#555c66;margin-top:12px}.foot p{margin:0 0 4px}" +
    "@page{margin:14mm}@media print{body{padding:0}}"

/** Texte für die aktuell geladene Sprache. */
@Composable
internal fun rememberSetupText(): SetupText {
    val res = LocalContext.current.resources
    return remember(res) { SetupText(res) }
}
