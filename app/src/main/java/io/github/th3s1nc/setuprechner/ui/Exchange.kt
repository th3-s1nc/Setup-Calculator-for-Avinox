package io.github.th3s1nc.setuprechner.ui

import io.github.th3s1nc.setuprechner.calc.Adjust
import io.github.th3s1nc.setuprechner.calc.Battery
import io.github.th3s1nc.setuprechner.calc.Motor
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.Scope
import io.github.th3s1nc.setuprechner.calc.SetupCalculator
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.floor

/** Ein Profil in einer Sicherung: Name, Eingaben, Anpassungen von Hand und Haken der Checkliste. */
data class BackupProfile(
    val name: String,
    val values: ProfileValues,
    val adjust: Map<Scope, Map<String, Adjust>>,
    val checks: Map<Scope, Map<String, String>>
)

/**
 * Austausch mit der Web-Version: Sicherung der Profile als JSON-Datei und Link zum Teilen.
 * Beide Formate sind dieselben wie in `docs/index.html`, eine Sicherung aus dem Browser lässt
 * sich also in der App laden und umgekehrt. Reines Kotlin ohne Android-Abhängigkeiten.
 */
object Exchange {
    const val APP_ID = "setup-calculator-for-avinox"
    const val SITE = "https://th3-s1nc.github.io/Setup-Calculator-for-Avinox/"

    /** Alle Modusnamen, die der Rechner kennt. Fremde Namen in einer Datei werden übergangen. */
    private val MODES: Set<String> = Motor.values().flatMap { SetupCalculator.ladderNames(it) }.toSet()

    /** Zahl als Text ohne überflüssige Nachkommastelle und mit Punkt: "23", "21.5". Leer, wenn keine Zahl. */
    fun number(s: String): String {
        val d = s.trim().replace(',', '.').toDoubleOrNull() ?: return ""
        if (d.isNaN() || d.isInfinite()) return ""
        return if (d == floor(d) && abs(d) < 1e15) d.toLong().toString() else d.toString()
    }

    private fun scopeKey(s: Scope): String = if (s == Scope.ALL) "all" else "four"

    // ---------------------------------------------------------------- Sicherung

    fun encode(profiles: List<BackupProfile>, exported: String): String {
        val list = profiles.map { p ->
            val v = p.values
            val data = LinkedHashMap<String, Any?>()
            data["motor"] = v.motor?.name
            data["battery"] = v.battery?.name?.lowercase()
            data["scope"] = scopeKey(v.scope)
            data["profile"] = v.focus.name.lowercase()
            data["bike"] = number(v.bike)
            data["rider"] = number(v.rider)
            data["power"] = number(v.power)
            data["cadence"] = number(v.cadence)
            data["adjust"] = Scope.values().associate { sc ->
                scopeKey(sc) to (p.adjust[sc] ?: emptyMap()).mapValues { (_, a) ->
                    val o = LinkedHashMap<String, Any?>()
                    if (a.al != 0) o["al"] = a.al
                    if (a.alLo != 0) o["alLo"] = a.alLo
                    if (a.alHi != 0) o["alHi"] = a.alHi
                    if (a.watt != 0) o["watt"] = a.watt
                    if (a.nm != 0) o["nm"] = a.nm
                    o
                }
            }
            data["checks"] = Scope.values().associate { sc -> scopeKey(sc) to (p.checks[sc] ?: emptyMap()) }
            linkedMapOf<String, Any?>("name" to p.name, "data" to data)
        }
        return Json.write(linkedMapOf<String, Any?>("app" to APP_ID, "version" to 1, "exported" to exported, "profiles" to list))
    }

    /** Liest eine Sicherung. null, wenn der Text keine Sicherung dieses Rechners ist. */
    fun decode(text: String): List<BackupProfile>? {
        val root = try { Json.parse(text) } catch (e: Exception) { return null }
        if (root !is Map<*, *> || root["app"] != APP_ID) return null
        val list = root["profiles"] as? List<*> ?: return null
        val out = ArrayList<BackupProfile>()
        for (item in list) {
            val o = item as? Map<*, *> ?: continue
            val name = (o["name"] as? String ?: "").replace(Regex("\\s+"), " ").trim()
            if (name.isEmpty()) continue
            val d = o["data"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            out.add(BackupProfile(name, values(d).copy(name = name), adjustOf(d["adjust"]), checksOf(d["checks"])))
        }
        return out
    }

    private fun text(v: Any?): String = when (v) {
        is String -> v
        is Number -> v.toString()
        else -> ""
    }

    private fun values(d: Map<*, *>): ProfileValues = ProfileValues(
        motor = Motor.values().firstOrNull { it.name == d["motor"] },
        battery = Battery.values().firstOrNull { it.name.lowercase() == d["battery"] },
        scope = if (d["scope"] == "all") Scope.ALL else Scope.FOUR,
        focus = Profile.values().firstOrNull { it.name.lowercase() == d["profile"] } ?: Profile.ALLROUND,
        bike = number(text(d["bike"])),
        rider = number(text(d["rider"])),
        power = number(text(d["power"])),
        cadence = number(text(d["cadence"]))
    )

    private fun int(v: Any?): Int {
        val d = (v as? Number)?.toDouble() ?: return 0
        return if (d.isNaN() || abs(d) > 2000.0) 0 else Math.round(d).toInt()
    }

    private fun adjustOf(v: Any?): Map<Scope, Map<String, Adjust>> {
        val o = v as? Map<*, *> ?: return emptyMap()
        val out = HashMap<Scope, Map<String, Adjust>>()
        for (sc in Scope.values()) {
            val src = o[scopeKey(sc)] as? Map<*, *> ?: continue
            val m = LinkedHashMap<String, Adjust>()
            for ((k, a) in src) {
                if (k !is String || k !in MODES || a !is Map<*, *>) continue
                val adj = Adjust(int(a["al"]), int(a["alLo"]), int(a["alHi"]), int(a["watt"]), int(a["nm"]))
                if (!adj.isZero) m[k] = adj
            }
            if (m.isNotEmpty()) out[sc] = m
        }
        return out
    }

    private fun checksOf(v: Any?): Map<Scope, Map<String, String>> {
        val o = v as? Map<*, *> ?: return emptyMap()
        val out = HashMap<Scope, Map<String, String>>()
        for (sc in Scope.values()) {
            val src = o[scopeKey(sc)] as? Map<*, *> ?: continue
            val m = LinkedHashMap<String, String>()
            for ((k, s) in src) if (k is String && k in MODES && s is String && s.length < 40 && isSignature(s)) m[k] = s
            if (m.isNotEmpty()) out[sc] = m
        }
        return out
    }

    /** Kennung eines Hakens: "Level-Level/Watt/Nm", nur Ziffern und die beiden Trennzeichen */
    fun isSignature(s: String): Boolean = s.isNotEmpty() && s.all { it.isDigit() || it == '-' || it == '/' }

    // ---------------------------------------------------------------- Link

    /**
     * Link auf die Web-Version, der die Eingaben und auf Wunsch die Anpassungen von Hand enthält.
     * Nur für vollständige Eingaben gedacht.
     */
    fun link(v: ProfileValues, adjust: Map<String, Adjust>): String {
        val q = ArrayList<String>()
        q.add("m=" + (v.motor?.name ?: ""))
        if (v.motor?.needsBattery == true && v.battery != null) q.add("a=" + v.battery.name.lowercase())
        q.add("b=" + number(v.bike))
        q.add("r=" + number(v.rider))
        q.add("p=" + number(v.power))
        q.add("c=" + number(v.cadence))
        q.add("s=" + scopeKey(v.scope))
        if (v.scope == Scope.FOUR) q.add("f=" + v.focus.name.lowercase())
        val parts = ArrayList<String>()
        for ((name, a) in adjust) {
            if (a.al != 0) parts.add("$name.al.${a.al}")
            if (a.alLo != 0) parts.add("$name.alLo.${a.alLo}")
            if (a.alHi != 0) parts.add("$name.alHi.${a.alHi}")
            if (a.watt != 0) parts.add("$name.watt.${a.watt}")
            if (a.nm != 0) parts.add("$name.nm.${a.nm}")
        }
        if (parts.isNotEmpty()) q.add("x=" + URLEncoder.encode(parts.joinToString(","), "UTF-8"))
        return SITE + "#" + q.joinToString("&")
    }
}

/** Kleiner JSON-Leser und -Schreiber für die Sicherung. Kennt Objekte, Listen, Text, Zahlen, true, false und null. */
internal object Json {

    fun write(value: Any?): String {
        val sb = StringBuilder()
        put(sb, value, 0)
        return sb.toString()
    }

    private fun put(sb: StringBuilder, v: Any?, depth: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Boolean -> sb.append(v.toString())
            is Int, is Long -> sb.append(v.toString())
            is Number -> {
                val d = v.toDouble()
                if (d.isNaN() || d.isInfinite()) sb.append("null")
                else if (d == floor(d) && abs(d) < 1e15) sb.append(d.toLong().toString())
                else sb.append(d.toString())
            }
            is Map<*, *> -> {
                if (v.isEmpty()) { sb.append("{}"); return }
                sb.append("{")
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(",")
                    first = false
                    line(sb, depth + 1)
                    quote(sb, k.toString())
                    sb.append(": ")
                    put(sb, x, depth + 1)
                }
                line(sb, depth)
                sb.append("}")
            }
            is List<*> -> {
                if (v.isEmpty()) { sb.append("[]"); return }
                sb.append("[")
                var first = true
                for (x in v) {
                    if (!first) sb.append(",")
                    first = false
                    line(sb, depth + 1)
                    put(sb, x, depth + 1)
                }
                line(sb, depth)
                sb.append("]")
            }
            else -> quote(sb, v.toString())
        }
    }

    private fun line(sb: StringBuilder, depth: Int) {
        sb.append("\n")
        for (i in 0 until depth) sb.append(" ")
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append(String.format("\\u%04x", ch.code)) else sb.append(ch)
            }
        }
        sb.append('"')
    }

    /** Liest einen JSON-Text. Wirft eine [IllegalArgumentException], wenn er nicht gültig ist. */
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.space()
        val v = p.value(0)
        p.space()
        if (p.pos != text.length) p.fail()
        return v
    }

    private class Parser(val s: String) {
        var pos = 0

        fun fail(): Nothing = throw IllegalArgumentException("JSON, Stelle $pos")

        fun space() {
            while (pos < s.length && (s[pos] == ' ' || s[pos] == '\n' || s[pos] == '\r' || s[pos] == '\t')) pos++
        }

        private fun expect(word: String) {
            if (!s.startsWith(word, pos)) fail()
            pos += word.length
        }

        fun value(depth: Int): Any? {
            if (depth > 32 || pos >= s.length) fail()
            return when (s[pos]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> str()
                't' -> { expect("true"); true }
                'f' -> { expect("false"); false }
                'n' -> { expect("null"); null }
                else -> num()
            }
        }

        private fun obj(depth: Int): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            pos++
            space()
            if (pos < s.length && s[pos] == '}') { pos++; return out }
            while (true) {
                space()
                if (pos >= s.length || s[pos] != '"') fail()
                val key = str()
                space()
                if (pos >= s.length || s[pos] != ':') fail()
                pos++
                space()
                out[key] = value(depth + 1)
                space()
                if (pos >= s.length) fail()
                if (s[pos] == ',') { pos++; continue }
                if (s[pos] == '}') { pos++; return out }
                fail()
            }
        }

        private fun arr(depth: Int): List<Any?> {
            val out = ArrayList<Any?>()
            pos++
            space()
            if (pos < s.length && s[pos] == ']') { pos++; return out }
            while (true) {
                space()
                out.add(value(depth + 1))
                space()
                if (pos >= s.length) fail()
                if (s[pos] == ',') { pos++; continue }
                if (s[pos] == ']') { pos++; return out }
                fail()
            }
        }

        private fun str(): String {
            val sb = StringBuilder()
            pos++
            while (true) {
                if (pos >= s.length) fail()
                val ch = s[pos++]
                if (ch == '"') return sb.toString()
                if (ch != '\\') { sb.append(ch); continue }
                if (pos >= s.length) fail()
                when (val e = s[pos++]) {
                    '"', '\\', '/' -> sb.append(e)
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pos + 4 > s.length) fail()
                        val code = s.substring(pos, pos + 4).toIntOrNull(16) ?: fail()
                        sb.append(code.toChar())
                        pos += 4
                    }
                    else -> fail()
                }
            }
        }

        private fun num(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '-' || s[pos] == '+' || s[pos] == '.' || s[pos] == 'e' || s[pos] == 'E')) pos++
            if (pos == start) fail()
            return s.substring(start, pos).toDoubleOrNull() ?: fail()
        }
    }
}
