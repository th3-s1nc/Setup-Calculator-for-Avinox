package io.github.th3s1nc.setuprechner.calc

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/*
 * Rechenkern des Setup-Rechners. Reines Kotlin ohne Android-Abhängigkeiten.
 *
 * Rechenweg nach dem Setup-Guide von Bernd Hemmersbach (Vorabfassung 11.05.2026)
 * und den Blättern "Generelles Setup" für M1, M2 und M2S.
 */

enum class Motor(val label: String, val nm: Int, val watt: Int, val boostNm: Int, val boostW: Int) {
    M1("M1", 105, 1000, 120, 1000),
    M2("M2", 110, 1000, 125, 1100),
    M2S("M2S", 130, 1300, 150, 1500)
}

/** FOUR = die vier Werksmodi, ALL = alle Stufen wie im Blatt "Generelles Setup". */
enum class Scope { FOUR, ALL }

/** Ausrichtung des Setups mit vier Modi. Die Anzeigenamen stehen in den Sprachdateien. */
enum class Profile { ALLROUND, LONG, POWER }

/** Gruppen der Modus-Leiter. Die Anzeigenamen stehen in den Sprachdateien. */
enum class Zone { LOW, MID, HIGH }

data class SetupInput(
    val motor: Motor,
    val bikeKg: Double,
    val riderKg: Double,
    val powerW: Double,
    val cadence: Int,
    val scope: Scope,
    val profile: Profile
) {
    val totalKg: Double get() = bikeKg + riderKg
}

data class CadenceRow(
    val rpm: Int,
    val flat: Double,
    val climb: Double,
    val wkg: Double,
    val preferred: Boolean
)

data class ModeResult(
    val name: String,
    val zone: Zone?,
    /** true = kein Werksmodus, muss in der Avinox Ride App neu angelegt werden */
    val custom: Boolean,
    val band: Pair<Double, Double>?,
    val watt: Int,
    val nm: Int,
    val alLo: Int,
    val alHi: Int,
    val pctLo: Int,
    val pctHi: Int,
    /** Motorleistung bei Lieblings-Trittfrequenz im Flachen (unterer Assist Level) */
    val flat: Double,
    /** Motorleistung bei Lieblings-Trittfrequenz am Anstieg (oberer Assist Level) */
    val climb: Double,
    val wkg: Double,
    /** Was der Assist Level bei der Eigenleistung hergibt */
    val alWatt: Double,
    /** Was das Drehmoment bei der Lieblings-Trittfrequenz hergibt */
    val nmWatt: Double,
    /** Trittfrequenz, ab der die Wattgrenze erreichbar ist */
    val rpmForMax: Int,
    val rows: List<CadenceRow>,
    val raisedToMinimum: Boolean,
    val nmSpread: Boolean
) {
    val isRange: Boolean get() = alLo != alHi
}

data class BoostResult(
    val nm: Int,
    val watt: Int,
    val pct: Int,
    val atCadence: Double,
    val wkg: Double,
    val rpmForMax: Int,
    val rows: List<CadenceRow>
)

data class SetupResult(
    val motor: Motor,
    val totalKg: Double,
    val modes: List<ModeResult>,
    val boost: BoostResult,
    val ladder: Boolean
)

object SetupCalculator {

    /** Assist Level -> Unterstützung in Prozent (Testfahrten des Guide-Autors mit dem M1). */
    val AL: IntArray = intArrayOf(0, 30, 60, 80, 100, 125, 150, 175, 225, 300, 375, 450, 525, 675, 825, 950)

    /** Watt = Nm x U/min / 9,55 */
    const val K: Double = 9.55

    private val OFFSETS = intArrayOf(-30, -20, -10, 0, 10, 20)

    private class Def(
        val name: String,
        val zone: Zone? = null,
        val target: Double = 0.0,
        val std: String? = null,
        val range: Boolean = false,
        val below: Boolean = false,
        val nmEff: Boolean = false,
        val top: Boolean = false,
        val band: Pair<Double, Double>? = null
    )

    private class Limits(val alMin: Int, val alMax: Int, val nmMin: Int, val nmMax: Int, val wMin: Int, val wMax: Int)

    private class Work(val def: Def, val lim: Limits) {
        var watt = 0
        var nm = 0
        var alLo = 0
        var alHi = 0
        var raised = false
        var spread = false
    }

    /** Zielwerte in Watt Motorleistung je kg Gesamtgewicht (Guide 2.5, 3.5, 3.6). */
    private fun fourModes(profile: Profile): List<Def> {
        val t: List<Triple<String, Double, Pair<Double, Double>>> = when (profile) {
            Profile.ALLROUND -> listOf(
                Triple("ECO", 1.4, 1.0 to 1.5), Triple("AUTO", 2.75, 2.0 to 3.0),
                Triple("TRAIL", 5.5, 4.5 to 6.0), Triple("TURBO", 7.75, 6.0 to 8.0)
            )
            Profile.LONG -> listOf(
                Triple("ECO", 0.9, 0.75 to 1.0), Triple("AUTO", 1.8, 1.5 to 2.0),
                Triple("TRAIL", 2.75, 2.5 to 3.5), Triple("TURBO", 4.1, 3.5 to 5.0)
            )
            Profile.POWER -> listOf(
                Triple("ECO", 1.8, 1.5 to 2.0), Triple("AUTO", 4.1, 3.0 to 4.5),
                Triple("TRAIL", 6.4, 6.0 to 7.5), Triple("TURBO", 9.1, 7.5 to 10.0)
            )
        }
        return t.map { (name, target, band) ->
            Def(name, target = target, std = name, range = name == "AUTO" || name == "TRAIL", band = band)
        }
    }

    /** Modus-Leitern der Blätter "Generelles Setup". */
    private fun ladder(motor: Motor): List<Def> = when (motor) {
        Motor.M1 -> listOf(
            Def("LOW", Zone.LOW, 0.9, below = true, nmEff = true),
            Def("ECO", Zone.LOW, 1.4, std = "ECO", below = true),
            Def("ROAD", Zone.LOW, 2.0, below = true),
            Def("AUTO", Zone.MID, 3.0, std = "AUTO", range = true),
            Def("TRAIL", Zone.MID, 4.2, std = "TRAIL", range = true),
            Def("TURBO", Zone.MID, 5.6, std = "TURBO"),
            Def("BEAST", Zone.HIGH, 7.5),
            Def("ALL IN", Zone.HIGH, top = true)
        )
        Motor.M2 -> listOf(
            Def("LOW", Zone.LOW, 0.9, below = true, nmEff = true),
            Def("FLAT", Zone.LOW, 1.4, below = true),
            Def("ECO", Zone.LOW, 1.8, std = "ECO"),
            Def("ROAD", Zone.MID, 2.2),
            Def("AUTO", Zone.MID, 3.0, std = "AUTO", range = true),
            Def("TRAIL", Zone.MID, 4.2, std = "TRAIL", range = true),
            Def("TURBO", Zone.HIGH, 5.6, std = "TURBO"),
            Def("POWER", Zone.HIGH, 7.5),
            Def("BEAST", Zone.HIGH, top = true)
        )
        Motor.M2S -> listOf(
            Def("LOW", Zone.LOW, 0.9, below = true, nmEff = true),
            Def("ECO", Zone.LOW, 1.4, std = "ECO", below = true),
            Def("ROAD", Zone.LOW, 2.1, below = true),
            Def("AUTO", Zone.MID, 3.0, std = "AUTO", range = true),
            Def("TRAIL", Zone.MID, 4.2, std = "TRAIL", range = true),
            Def("TURBO", Zone.MID, 5.6, std = "TURBO"),
            Def("BEAST", Zone.HIGH, 7.5),
            Def("ALL IN", Zone.HIGH, 9.0),
            Def("MTWOS", Zone.HIGH, top = true)
        )
    }

    fun ladderNames(motor: Motor): List<String> = ladder(motor).map { it.name }

    /** Einstellbereiche der vier Werksmodi (Guide 1.2, M1). Obergrenzen folgen dem Motor. */
    private fun limits(std: String?, m: Motor): Limits = when (std) {
        "ECO" -> Limits(1, 7, 10, 70, 100, 400)
        "AUTO" -> Limits(3, 11, 10, m.nm, 200, m.watt)
        "TRAIL" -> Limits(6, 13, 20, m.nm, 300, m.watt)
        "TURBO" -> Limits(8, 15, 60, m.nm, 400, m.watt)
        else -> Limits(1, 15, 10, m.nm, 100, m.watt)
    }

    private fun ceil5(x: Double): Int = (ceil((x - 1e-9) / 5.0) * 5.0).toInt()
    private fun round50(x: Double): Int = (floor(x / 50.0 + 1e-9 + 0.5) * 50.0).toInt()
    private fun round100(x: Double): Int = (floor(x / 100.0 + 1e-9 + 0.5) * 100.0).toInt()

    /** Nächsthöhere Stufe, die die nötige Unterstützung liefert (2 % Toleranz). */
    private fun alMatch(watt: Int, own: Double): Int {
        val req = watt / own * 100.0
        for (l in 1..15) if (AL[l] >= req * 0.98) return l
        return 15
    }

    /** Höchste Stufe, die unter der Wattgrenze bleibt (Reichweiten-Stufen). */
    private fun alBelow(watt: Int, own: Double): Int {
        val req = watt / own * 100.0
        var r = 1
        for (l in 1..15) if (AL[l] < req - 1e-9) r = l
        return r
    }

    fun compute(inp: SetupInput): SetupResult {
        val motor = inp.motor
        val isLadder = inp.scope == Scope.ALL
        val defs = if (isLadder) ladder(motor) else fourModes(inp.profile)
        val kg = inp.totalKg
        val own = inp.powerW
        val cad = inp.cadence
        val n = defs.size
        val ws = defs.map { Work(it, limits(it.std, motor)) }

        // 1. max. Watt aus Zielwert x Gesamtgewicht
        for (w in ws) {
            if (w.def.top) {
                w.watt = motor.watt
            } else {
                val raw = w.def.target * kg
                val rounded = if (isLadder && raw >= 500.0) round100(raw) else round50(raw)
                val clamped = rounded.coerceIn(w.lim.wMin, w.lim.wMax)
                if (clamped > rounded) w.raised = true
                w.watt = clamped
            }
        }
        for (i in 1 until n) {
            if (!ws[i].def.top && ws[i].watt < ws[i - 1].watt + 50) {
                ws[i].watt = min(ws[i].lim.wMax, ws[i - 1].watt + 50)
            }
        }
        for (i in n - 2 downTo 0) {
            val step = if (isLadder && ws[i + 1].watt > 500) 100 else 50
            val cap = max(ws[i].lim.wMin, ws[i + 1].watt - step)
            if (ws[i].watt > cap) ws[i].watt = cap
        }

        // 2. Assist Level aus Wattgrenze / Eigenleistung
        var prevHi = 0
        var prevRange = false
        for (w in ws) {
            val d = w.def
            val lim = w.lim
            if (d.range) {
                var hi = alMatch(w.watt, own).coerceIn(lim.alMin, lim.alMax)
                var lo = if (prevRange) max(lim.alMin, prevHi) else max(lim.alMin, min(prevHi + 1, hi - 2))
                if (hi <= lo) hi = min(lim.alMax, lo + 1)
                if (hi <= lo) lo = hi - 1
                w.alLo = lo
                w.alHi = hi
            } else {
                var a = if (d.below) alBelow(w.watt, own) else alMatch(w.watt, own)
                if (d.top) a += 1 // Reserve, damit die volle Leistung auch mit müden Beinen anliegt
                a = max(a, if (d.zone == Zone.LOW) prevHi + 1 else prevHi)
                a = a.coerceIn(lim.alMin, lim.alMax)
                w.alLo = a
                w.alHi = a
            }
            prevHi = w.alHi
            prevRange = d.range
        }

        // 3. max. Nm aus Wattgrenze und Lieblings-Trittfrequenz
        for (w in ws) {
            val base = if (w.def.nmEff) min(w.watt.toDouble(), AL[w.alHi] / 100.0 * own) else w.watt.toDouble()
            val nm = ceil5(base * K / cad)
            val clamped = nm.coerceIn(w.lim.nmMin, w.lim.nmMax)
            if (clamped > nm) w.raised = true
            w.nm = clamped
        }
        if (isLadder) {
            for (i in n - 2 downTo 0) {
                if (ws[i].def.zone != Zone.HIGH || ws[i + 1].def.zone != Zone.HIGH) continue
                val cap = max(ws[i].lim.nmMin, ws[i + 1].nm - 15)
                if (ws[i].nm > cap) {
                    ws[i].nm = cap
                    ws[i].spread = true
                }
            }
        }

        // 4. Leistungstabellen
        val modes = ws.map { w ->
            fun eff(al: Int, rpm: Int): Double =
                min(min(AL[al] / 100.0 * own, w.nm * rpm / K), w.watt.toDouble())
            val rows = OFFSETS.map { dx ->
                val rpm = cad + dx
                val climb = eff(w.alHi, rpm)
                CadenceRow(rpm, eff(w.alLo, rpm), climb, climb / kg, dx == 0)
            }
            val climb = eff(w.alHi, cad)
            ModeResult(
                name = w.def.name,
                zone = w.def.zone,
                custom = w.def.std == null,
                band = w.def.band,
                watt = w.watt,
                nm = w.nm,
                alLo = w.alLo,
                alHi = w.alHi,
                pctLo = AL[w.alLo],
                pctHi = AL[w.alHi],
                flat = eff(w.alLo, cad),
                climb = climb,
                wkg = climb / kg,
                alWatt = AL[w.alHi] / 100.0 * own,
                nmWatt = w.nm * cad / K,
                rpmForMax = ceil(w.watt * K / w.nm - 1e-9).toInt(),
                rows = rows,
                raisedToMinimum = w.raised,
                nmSpread = w.spread
            )
        }

        fun boostAt(rpm: Int): Double =
            min(min(motor.boostNm * rpm / K, motor.boostW.toDouble()), AL[15] / 100.0 * own)
        val boost = BoostResult(
            nm = motor.boostNm,
            watt = motor.boostW,
            pct = AL[15],
            atCadence = boostAt(cad),
            wkg = boostAt(cad) / kg,
            rpmForMax = ceil(motor.boostW * K / motor.boostNm - 1e-9).toInt(),
            rows = OFFSETS.map { dx ->
                val p = boostAt(cad + dx)
                CadenceRow(cad + dx, p, p, p / kg, dx == 0)
            }
        )
        return SetupResult(motor, kg, modes, boost, isLadder)
    }
}
