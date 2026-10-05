package io.github.th3s1nc.setuprechner.ui

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.th3s1nc.setuprechner.calc.Adjust
import io.github.th3s1nc.setuprechner.calc.Battery
import io.github.th3s1nc.setuprechner.calc.ModeResult
import io.github.th3s1nc.setuprechner.calc.Motor
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.Scope
import io.github.th3s1nc.setuprechner.calc.SetupInput
import kotlin.math.roundToInt

/** Ein gespeichertes Fahrerprofil. Ein leerer Name wird in der Oberfläche als "Profil N" gezeigt. */
data class RiderProfile(val id: Int, val name: String)

/** Ein vollständiger Satz Eingaben, so wie er im Formular steht oder gespeichert ist. */
data class ProfileValues(
    val name: String = "",
    val motor: Motor? = null,
    val scope: Scope = Scope.FOUR,
    val focus: Profile = Profile.ALLROUND,
    val bike: String = "",
    val rider: String = "",
    val power: String = "",
    val cadence: String = "",
    /** Akku, nur beim M2S von Belang */
    val battery: Battery? = null
) {
    // "NaN" und "Infinity" liest Kotlin als Zahl. Hier gelten sie wie jeder andere unbrauchbare Text.
    private fun number(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    val bikeKg: Double? get() = number(bike)
    val riderKg: Double? get() = number(rider)
    val powerW: Double? get() = number(power)
    val cadenceRpm: Double? get() = number(cadence)

    val weightOk: Boolean get() = (bikeKg ?: -1.0) in 10.0..45.0 && (riderKg ?: -1.0) in 40.0..180.0
    val powerOk: Boolean get() = (powerW ?: -1.0) in 50.0..500.0
    val cadenceOk: Boolean get() = (cadenceRpm ?: -1.0) in 40.0..120.0
    /** true, wenn der Motor eine Akku-Wahl braucht und noch keine getroffen ist */
    val batteryMissing: Boolean get() = motor?.needsBattery == true && battery == null

    /**
     * true, wenn sich aus den Werten ein Setup rechnen lässt. Ein fehlender Akku hindert daran nicht:
     * Profile aus Versionen vor der Akku-Wahl rechnen weiter mit dem höheren Boost-Wert.
     */
    val usable: Boolean get() = motor != null && weightOk && powerOk && cadenceOk

    /** true, wenn alles ausgefüllt ist, was zum Speichern nötig ist, beim M2S also auch der Akku */
    val complete: Boolean get() = usable && !batteryMissing

    // Ein leeres Feld gilt nicht als Fehler, nur ein ausgefülltes mit unpassendem Wert.
    val bikeBad: Boolean get() = bike.isNotBlank() && (bikeKg ?: -1.0) !in 10.0..45.0
    val riderBad: Boolean get() = rider.isNotBlank() && (riderKg ?: -1.0) !in 40.0..180.0
    val powerBad: Boolean get() = power.isNotBlank() && !powerOk
    val cadenceBad: Boolean get() = cadence.isNotBlank() && !cadenceOk

    val totalKg: Double? get() {
        val b = bikeKg
        val r = riderKg
        return if (b != null && r != null) b + r else null
    }

    fun toInput(adjust: Map<String, Adjust> = emptyMap()): SetupInput? {
        if (!usable) return null
        return SetupInput(
            motor = motor ?: return null,
            bikeKg = bikeKg ?: return null,
            riderKg = riderKg ?: return null,
            powerW = powerW ?: return null,
            cadence = (cadenceRpm ?: return null).roundToInt(),
            scope = scope,
            profile = focus,
            battery = battery,
            adjust = adjust
        )
    }
}

/**
 * Eingaben des Formulars, getrennt nach Fahrerprofilen.
 *
 * Die Felder dieser Klasse sind der Entwurf des aktiven Profils. Erst [save] schreibt ihn ins
 * Profil; das Setup rechnet immer mit dem gespeicherten Stand ([saved]). Neue Profile beginnen leer.
 *
 * Speicherform: "profile_ids" = "1,2,3", "active_profile" = "2", die gespeicherten Werte je Profil
 * unter "p<id>_<feld>". Ein ungespeicherter Entwurf liegt unter "draft_<feld>" mit "draft_id",
 * damit er Drehen des Bildschirms und einen Sprachwechsel übersteht. Stände aus Versionen ohne
 * Profile (Felder ohne Präfix) werden beim ersten Start als Profil 1 übernommen.
 *
 * Anpassungen von Hand und die Haken der Checkliste gehören zum Profil, gelten aber sofort und
 * ohne Speichern. Sie liegen unter "p<id>_adjust_four", "p<id>_adjust_all", "p<id>_checks_four"
 * und "p<id>_checks_all". Die Einstellungen der Ansicht ("advanced", "unit", "speed") gelten für
 * alle Profile.
 */
class SetupState(private val prefs: SharedPreferences) {

    var profiles by mutableStateOf(listOf<RiderProfile>())
        private set
    var activeId by mutableStateOf(1)
        private set

    /** Gespeicherter Stand des aktiven Profils. Damit rechnet das Setup. */
    var saved by mutableStateOf(ProfileValues())
        private set

    // Entwurf: was gerade im Formular steht
    var name by mutableStateOf("")
        private set
    var motor by mutableStateOf<Motor?>(null)
    var scope by mutableStateOf(Scope.FOUR)

    /** Ausrichtung des Setups mit vier Modi (Alleskönner, Langstrecke, Power) */
    var profile by mutableStateOf(Profile.ALLROUND)
    var bike by mutableStateOf("")
    var rider by mutableStateOf("")
    var power by mutableStateOf("")
    var cadence by mutableStateOf("")
    var battery by mutableStateOf<Battery?>(null)

    /** Anpassungen von Hand des aktiven Profils, getrennt für "4 Modi" und "Alle Modi" */
    var adjust by mutableStateOf(mapOf<Scope, Map<String, Adjust>>())
        private set

    /** Abgehakte Stufen des aktiven Profils: Modusname -> Kennung der Werte, die eingetragen wurden */
    var checks by mutableStateOf(mapOf<Scope, Map<String, String>>())
        private set

    /** Schalter "Erweitert": aus = nur der Standardrechner */
    var advanced by mutableStateOf(false)
        private set

    /** Letzte Spalte als geschätzte Steigung statt W/kg */
    var gradeUnit by mutableStateOf(false)
        private set

    /** Tempo in km/h für die Steigungsschätzung */
    var speed by mutableStateOf(DEFAULT_SPEED)
        private set

    init {
        var ids = parseIds(prefs.getString(KEY_IDS, null))
        if (ids.isEmpty()) {
            // Erster Start oder Update von einer Version ohne Profile
            val e = prefs.edit()
            for (f in FIELDS) {
                val old = prefs.getString(f, null)
                if (old != null) {
                    e.putString(PROFILE_PREFIX + 1 + "_" + f, old)
                    e.remove(f)
                }
            }
            e.putString(KEY_IDS, "1").putString(KEY_ACTIVE, "1").apply()
            ids = listOf(1)
        }
        val active = prefs.getString(KEY_ACTIVE, null)?.toIntOrNull()
        activeId = if (active != null && active in ids) active else ids.first()
        profiles = ids.map { RiderProfile(it, prefs.getString(profileKey(it, F_NAME), null) ?: "") }
        saved = read(PROFILE_PREFIX + activeId + "_")
        // Ungespeicherten Entwurf wiederherstellen, wenn er zum aktiven Profil gehört
        if (prefs.getString(KEY_DRAFT_ID, null)?.toIntOrNull() == activeId) show(read(DRAFT_PREFIX))
        else { clearDraft(); show(saved) }
        loadExtras()
        advanced = prefs.getString(KEY_ADVANCED, null) == "1"
        gradeUnit = prefs.getString(KEY_UNIT, null) == UNIT_GRADE
        speed = (prefs.getString(KEY_SPEED, null)?.toIntOrNull() ?: DEFAULT_SPEED).coerceIn(MIN_SPEED, MAX_SPEED)
    }

    /** Der Entwurf als Wertesatz */
    val draft: ProfileValues get() = ProfileValues(name, motor, scope, profile, bike, rider, power, cadence, battery)

    // ---------------------------------------------------------------- Erweitert

    /** true, wenn die Steigung angezeigt wird. Ohne "Erweitert" bleibt es bei W/kg. */
    val showGrade: Boolean get() = advanced && gradeUnit

    /** Anpassungen, mit denen das Setup gerade rechnet. Ohne "Erweitert" ruhen sie. */
    val activeAdjust: Map<String, Adjust> get() = if (advanced) adjust[saved.scope] ?: emptyMap() else emptyMap()

    /** Eingaben für die Rechnung aus dem gespeicherten Stand, mit den Anpassungen von Hand. */
    fun savedInput(): SetupInput? = saved.toInput(activeAdjust)

    /** Gespeicherter Stand eines anderen Profils als Eingaben für den Vergleich, mit dessen Anpassungen. null, wenn unvollständig. */
    fun inputOf(id: Int): SetupInput? {
        val prefix = PROFILE_PREFIX + id + "_"
        val v = read(prefix)
        return v.toInput(readAdjust(prefix)[v.scope] ?: emptyMap())
    }

    fun showAdvanced(on: Boolean) {
        advanced = on
        prefs.edit().putString(KEY_ADVANCED, if (on) "1" else "0").apply()
    }

    fun useGrade(on: Boolean) {
        gradeUnit = on
        prefs.edit().putString(KEY_UNIT, if (on) UNIT_GRADE else UNIT_WKG).apply()
    }

    val canSlower: Boolean get() = speed > MIN_SPEED
    val canFaster: Boolean get() = speed < MAX_SPEED

    fun stepSpeed(delta: Int) {
        speed = (speed + delta).coerceIn(MIN_SPEED, MAX_SPEED)
        prefs.edit().putString(KEY_SPEED, speed.toString()).apply()
    }

    /** true, wenn der Wert nach dem Schritt noch im Einstellbereich des Modus liegt */
    fun canStep(m: ModeResult, field: AdjustField, delta: Int): Boolean = when (field) {
        AdjustField.AL -> m.alHi + delta in m.alMin..m.alMax
        AdjustField.AL_LO -> m.alLo + delta in m.alMin..m.alHi
        AdjustField.AL_HI -> m.alHi + delta in m.alLo..m.alMax
        AdjustField.WATT -> m.watt + delta in m.wattMin..m.wattMax
        AdjustField.NM -> m.nm + delta in m.nmMin..m.nmMax
    }

    /**
     * Stellt einen Wert eines Modus um einen Schritt nach. Ausgangspunkt ist die wirksame Abweichung,
     * nicht eine, die an der Grenze des Modus abgeschnitten wurde.
     */
    fun step(m: ModeResult, field: AdjustField, delta: Int) {
        if (!canStep(m, field, delta)) return
        var a = if (m.twoLevels) Adjust(alLo = m.dAlLo, alHi = m.dAlHi, watt = m.dWatt, nm = m.dNm)
        else Adjust(al = m.dAlHi, watt = m.dWatt, nm = m.dNm)
        a = when (field) {
            AdjustField.AL -> a.copy(al = a.al + delta)
            AdjustField.AL_LO -> a.copy(alLo = a.alLo + delta)
            AdjustField.AL_HI -> a.copy(alHi = a.alHi + delta)
            AdjustField.WATT -> a.copy(watt = a.watt + delta)
            AdjustField.NM -> a.copy(nm = a.nm + delta)
        }
        putAdjust(m.name, a)
    }

    fun resetAdjust(mode: String) = putAdjust(mode, Adjust())

    private fun putAdjust(mode: String, a: Adjust) {
        val scope = saved.scope
        val now = adjust[scope] ?: emptyMap()
        val next = if (a.isZero) now - mode else now + (mode to a)
        adjust = adjust + (scope to next)
        prefs.edit().putString(profileKey(activeId, adjustField(scope)), encodeAdjust(next)).apply()
    }

    fun isChecked(m: ModeResult): Boolean = checks[saved.scope]?.get(m.name) == m.signature

    fun setChecked(m: ModeResult, on: Boolean) {
        val now = checks[saved.scope] ?: emptyMap()
        putChecks(if (on) now + (m.name to m.signature) else now - m.name)
    }

    fun clearChecks() = putChecks(emptyMap())

    private fun putChecks(next: Map<String, String>) {
        val scope = saved.scope
        checks = checks + (scope to next)
        prefs.edit().putString(profileKey(activeId, checksField(scope)), encodeChecks(next)).apply()
    }

    // ---------------------------------------------------------------- Sicherung

    /** Alle Profile als Sicherung im Format der Web-Version. [displayName] liefert den Namen für Profile ohne eigenen. */
    fun backup(exported: String, displayName: (Int) -> String): String =
        Exchange.encode(profiles.mapIndexed { i, p ->
            val prefix = PROFILE_PREFIX + p.id + "_"
            BackupProfile(p.name.ifBlank { displayName(i) }, read(prefix), readAdjust(prefix), readChecks(prefix))
        }, exported)

    /**
     * Lädt Profile aus einer Sicherung. Ein Profil mit gleichem Namen wird ersetzt, sonst kommt es
     * dazu. [displayName] liefert wie bei [backup] den Namen für Profile ohne eigenen. Gibt (geladen, wegen der Obergrenze übersprungen) zurück oder null, wenn der Text keine
     * Sicherung dieses Rechners ist.
     */
    fun restore(text: String, displayName: (Int) -> String): Pair<Int, Int>? {
        val list = Exchange.decode(text) ?: return null
        var loaded = 0
        var skipped = 0
        var reload = false
        for (b in list) {
            val name = b.name.take(MAX_NAME).trim()
            if (name.isEmpty()) continue
            val slot = slotFor(name, displayName)
            if (slot == null) { skipped++; continue }
            val prefix = PROFILE_PREFIX + slot.id + "_"
            val e = write(prefs.edit(), prefix, b.values.copy(name = name))
            for (sc in Scope.values()) {
                e.putString(prefix + adjustField(sc), encodeAdjust(b.adjust[sc] ?: emptyMap()))
                e.putString(prefix + checksField(sc), encodeChecks(b.checks[sc] ?: emptyMap()))
            }
            e.apply()
            profiles = if (profiles.any { it.id == slot.id }) profiles.map { if (it.id == slot.id) slot else it } else profiles + slot
            if (slot.id == activeId) reload = true
            loaded++
        }
        if (loaded == 0 && skipped == 0) return null
        prefs.edit().putString(KEY_IDS, profiles.joinToString(",") { it.id.toString() }).apply()
        if (reload) activate(activeId)
        return loaded to skipped
    }

    /**
     * Platz für ein geladenes Profil: gleicher Name, sonst ein noch leeres Profil, sonst ein neues.
     * Verglichen wird der Name, wie er in der App steht. Ein Profil ohne eigenen Namen trägt den
     * von [displayName] gelieferten und wird so auch beim Zurückladen seiner eigenen Sicherung gefunden.
     */
    private fun slotFor(name: String, displayName: (Int) -> String): RiderProfile? {
        val same = profiles.withIndex().firstOrNull { (i, p) -> p.name.ifBlank { displayName(i) }.equals(name, ignoreCase = true) }?.value
        if (same != null) return same.copy(name = name)
        val empty = profiles.firstOrNull { it.name.isBlank() && read(PROFILE_PREFIX + it.id + "_") == ProfileValues() }
        if (empty != null) return empty.copy(name = name)
        if (!canAdd) return null
        return RiderProfile((profiles.maxOfOrNull { it.id } ?: 0) + 1, name)
    }

    // ---------------------------------------------------------------- Formular

    /** true, wenn im Formular etwas anderes steht als gespeichert ist */
    val dirty: Boolean get() = draft != saved
    val canSave: Boolean get() = dirty && draft.complete
    val canAdd: Boolean get() = profiles.size < MAX_PROFILES
    val canDelete: Boolean get() = profiles.size > 1

    /** Nach jeder Änderung im Formular aufrufen: merkt sich den Entwurf. */
    fun edit() {
        if (!dirty) { clearDraft(); return }
        write(prefs.edit(), DRAFT_PREFIX, draft).putString(KEY_DRAFT_ID, activeId.toString()).apply()
    }

    fun rename(newName: String) {
        name = newName.take(MAX_NAME)
        edit()
    }

    /** Schreibt den Entwurf ins aktive Profil. Nur möglich, wenn alle Felder gültig sind. */
    fun save(): Boolean {
        if (!draft.complete) return false
        name = name.trim()
        val values = draft
        write(prefs.edit(), PROFILE_PREFIX + activeId + "_", values).apply()
        saved = values
        profiles = profiles.map { if (it.id == activeId) it.copy(name = values.name) else it }
        clearDraft()
        return true
    }

    /** Setzt das Formular auf den gespeicherten Stand zurück. */
    fun discard() {
        clearDraft()
        show(saved)
    }

    /** Wechselt zu einem anderen Profil. Ein ungespeicherter Entwurf geht dabei verloren. */
    fun select(id: Int) {
        if (id == activeId || profiles.none { it.id == id }) return
        activate(id)
    }

    /** Legt ein neues, leeres Profil an und macht es aktiv. */
    fun add() {
        if (!canAdd) return
        val id = (profiles.maxOfOrNull { it.id } ?: 0) + 1
        profiles = profiles + RiderProfile(id, "")
        prefs.edit().putString(KEY_IDS, profiles.joinToString(",") { it.id.toString() }).apply()
        activate(id)
    }

    /** Löscht das aktive Profil und wechselt zum Nachbarn. Das letzte Profil bleibt immer erhalten. */
    fun deleteActive() {
        if (!canDelete) return
        val index = profiles.indexOfFirst { it.id == activeId }
        val removed = activeId
        profiles = profiles.filter { it.id != removed }
        val e = prefs.edit()
        for (f in FIELDS + F_NAME + EXTRAS) e.remove(profileKey(removed, f))
        e.putString(KEY_IDS, profiles.joinToString(",") { it.id.toString() }).apply()
        activate(profiles[(index - 1).coerceAtLeast(0)].id)
    }

    /** Plus/Minus-Tasten: Eigenleistung in 10-Watt-Schritten. Ein leeres Feld beginnt bei 200 W. */
    fun stepPower(delta: Int) {
        val now = draft.powerW
        power = (if (now == null) 200 else (now.roundToInt() + delta).coerceIn(50, 500)).toString()
        edit()
    }

    /** Plus/Minus-Tasten: Trittfrequenz in 5er-Schritten. Ein leeres Feld beginnt bei 75 U/min. */
    fun stepCadence(delta: Int) {
        val now = draft.cadenceRpm
        cadence = (if (now == null) 75 else (now.roundToInt() + delta).coerceIn(40, 120)).toString()
        edit()
    }

    private fun activate(id: Int) {
        activeId = id
        prefs.edit().putString(KEY_ACTIVE, id.toString()).apply()
        clearDraft()
        saved = read(PROFILE_PREFIX + id + "_")
        show(saved)
        loadExtras()
    }

    private fun loadExtras() {
        val prefix = PROFILE_PREFIX + activeId + "_"
        adjust = readAdjust(prefix)
        checks = readChecks(prefix)
    }

    private fun readAdjust(prefix: String): Map<Scope, Map<String, Adjust>> =
        Scope.values().associate { it to decodeAdjust(prefs.getString(prefix + adjustField(it), null)) }

    private fun readChecks(prefix: String): Map<Scope, Map<String, String>> =
        Scope.values().associate { it to decodeChecks(prefs.getString(prefix + checksField(it), null)) }

    private fun show(v: ProfileValues) {
        name = v.name
        motor = v.motor
        scope = v.scope
        profile = v.focus
        bike = v.bike
        rider = v.rider
        power = v.power
        cadence = v.cadence
        battery = v.battery
    }

    private fun read(prefix: String): ProfileValues = ProfileValues(
        name = prefs.getString(prefix + F_NAME, null) ?: "",
        motor = enumOrNull<Motor>(prefs.getString(prefix + F_MOTOR, null)),
        scope = enumOrNull<Scope>(prefs.getString(prefix + F_SCOPE, null)) ?: Scope.FOUR,
        focus = enumOrNull<Profile>(prefs.getString(prefix + F_FOCUS, null)) ?: Profile.ALLROUND,
        bike = prefs.getString(prefix + F_BIKE, null) ?: "",
        rider = prefs.getString(prefix + F_RIDER, null) ?: "",
        power = prefs.getString(prefix + F_POWER, null) ?: "",
        cadence = prefs.getString(prefix + F_CADENCE, null) ?: "",
        battery = enumOrNull<Battery>(prefs.getString(prefix + F_BATTERY, null))
    )

    private fun write(e: SharedPreferences.Editor, prefix: String, v: ProfileValues): SharedPreferences.Editor {
        e.putString(prefix + F_NAME, v.name)
        if (v.motor != null) e.putString(prefix + F_MOTOR, v.motor.name) else e.remove(prefix + F_MOTOR)
        if (v.battery != null) e.putString(prefix + F_BATTERY, v.battery.name) else e.remove(prefix + F_BATTERY)
        return e.putString(prefix + F_SCOPE, v.scope.name)
            .putString(prefix + F_FOCUS, v.focus.name)
            .putString(prefix + F_BIKE, v.bike)
            .putString(prefix + F_RIDER, v.rider)
            .putString(prefix + F_POWER, v.power)
            .putString(prefix + F_CADENCE, v.cadence)
    }

    private fun clearDraft() {
        if (!prefs.contains(KEY_DRAFT_ID)) return
        val e = prefs.edit().remove(KEY_DRAFT_ID)
        for (f in FIELDS + F_NAME) e.remove(DRAFT_PREFIX + f)
        e.apply()
    }

    private fun profileKey(id: Int, field: String): String = PROFILE_PREFIX + id + "_" + field

    private fun parseIds(s: String?): List<Int> =
        s?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.distinct() ?: emptyList()

    private companion object {
        const val MAX_PROFILES = 10
        const val MAX_NAME = 24

        const val KEY_IDS = "profile_ids"
        const val KEY_ACTIVE = "active_profile"
        const val KEY_DRAFT_ID = "draft_id"
        const val PROFILE_PREFIX = "p"
        const val DRAFT_PREFIX = "draft_"

        const val F_NAME = "name"
        const val F_MOTOR = "motor"
        const val F_SCOPE = "scope"
        const val F_FOCUS = "profile"
        const val F_BIKE = "bike"
        const val F_RIDER = "rider"
        const val F_POWER = "power"
        const val F_CADENCE = "cadence"
        const val F_BATTERY = "battery"

        const val KEY_ADVANCED = "advanced"
        const val KEY_UNIT = "unit"
        const val KEY_SPEED = "speed"
        const val UNIT_GRADE = "grade"
        const val UNIT_WKG = "wkg"
        const val DEFAULT_SPEED = 15
        const val MIN_SPEED = 5
        const val MAX_SPEED = 24

        fun adjustField(s: Scope): String = if (s == Scope.ALL) "adjust_all" else "adjust_four"
        fun checksField(s: Scope): String = if (s == Scope.ALL) "checks_all" else "checks_four"

        /** Felder je Profil, die nicht zum Formular gehören */
        val EXTRAS = listOf("adjust_four", "adjust_all", "checks_four", "checks_all")

        // Speicherform der Anpassungen: "AUTO:0,0,1,0,-5;ECO:0,0,0,50,0" (al, alLo, alHi, Watt, Nm)
        fun encodeAdjust(m: Map<String, Adjust>): String =
            m.entries.joinToString(";") { (k, a) -> "$k:${a.al},${a.alLo},${a.alHi},${a.watt},${a.nm}" }

        fun decodeAdjust(s: String?): Map<String, Adjust> {
            val out = LinkedHashMap<String, Adjust>()
            if (s.isNullOrEmpty()) return out
            for (part in s.split(';')) {
                val name = part.substringBefore(':', "")
                val n = part.substringAfter(':', "").split(',').mapNotNull { it.toIntOrNull() }
                if (name.isEmpty() || n.size != 5) continue
                val a = Adjust(n[0], n[1], n[2], n[3], n[4])
                if (!a.isZero) out[name] = a
            }
            return out
        }

        // Speicherform der Haken: "ECO=3-3/150/20;AUTO=4-6/300/40"
        fun encodeChecks(m: Map<String, String>): String = m.entries.joinToString(";") { "${it.key}=${it.value}" }

        fun decodeChecks(s: String?): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            if (s.isNullOrEmpty()) return out
            for (part in s.split(';')) {
                val name = part.substringBefore('=', "")
                val sig = part.substringAfter('=', "")
                if (name.isNotEmpty() && Exchange.isSignature(sig)) out[name] = sig
            }
            return out
        }

        /** Eingabefelder ohne den Namen. Unter diesen Schlüsseln lagen die Werte vor der Einführung der Profile. */
        val FIELDS = listOf(F_MOTOR, F_SCOPE, F_FOCUS, F_BIKE, F_RIDER, F_POWER, F_CADENCE, F_BATTERY)
    }
}

/** Die Werte, die sich im Bereich "Anpassen" nachstellen lassen */
enum class AdjustField { AL, AL_LO, AL_HI, WATT, NM }

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
