package io.github.th3s1nc.setuprechner.ui

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val cadence: String = ""
) {
    private fun number(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

    val bikeKg: Double? get() = number(bike)
    val riderKg: Double? get() = number(rider)
    val powerW: Double? get() = number(power)
    val cadenceRpm: Double? get() = number(cadence)

    val weightOk: Boolean get() = (bikeKg ?: -1.0) in 10.0..45.0 && (riderKg ?: -1.0) in 40.0..180.0
    val powerOk: Boolean get() = (powerW ?: -1.0) in 50.0..500.0
    val cadenceOk: Boolean get() = (cadenceRpm ?: -1.0) in 40.0..120.0
    val complete: Boolean get() = motor != null && weightOk && powerOk && cadenceOk

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

    fun toInput(): SetupInput? {
        if (!complete) return null
        return SetupInput(
            motor = motor ?: return null,
            bikeKg = bikeKg ?: return null,
            riderKg = riderKg ?: return null,
            powerW = powerW ?: return null,
            cadence = (cadenceRpm ?: return null).roundToInt(),
            scope = scope,
            profile = focus
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
    }

    /** Der Entwurf als Wertesatz */
    val draft: ProfileValues get() = ProfileValues(name, motor, scope, profile, bike, rider, power, cadence)

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
        for (f in FIELDS + F_NAME) e.remove(profileKey(removed, f))
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
    }

    private fun show(v: ProfileValues) {
        name = v.name
        motor = v.motor
        scope = v.scope
        profile = v.focus
        bike = v.bike
        rider = v.rider
        power = v.power
        cadence = v.cadence
    }

    private fun read(prefix: String): ProfileValues = ProfileValues(
        name = prefs.getString(prefix + F_NAME, null) ?: "",
        motor = enumOrNull<Motor>(prefs.getString(prefix + F_MOTOR, null)),
        scope = enumOrNull<Scope>(prefs.getString(prefix + F_SCOPE, null)) ?: Scope.FOUR,
        focus = enumOrNull<Profile>(prefs.getString(prefix + F_FOCUS, null)) ?: Profile.ALLROUND,
        bike = prefs.getString(prefix + F_BIKE, null) ?: "",
        rider = prefs.getString(prefix + F_RIDER, null) ?: "",
        power = prefs.getString(prefix + F_POWER, null) ?: "",
        cadence = prefs.getString(prefix + F_CADENCE, null) ?: ""
    )

    private fun write(e: SharedPreferences.Editor, prefix: String, v: ProfileValues): SharedPreferences.Editor {
        e.putString(prefix + F_NAME, v.name)
        if (v.motor != null) e.putString(prefix + F_MOTOR, v.motor.name) else e.remove(prefix + F_MOTOR)
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

        /** Eingabefelder ohne den Namen. Unter diesen Schlüsseln lagen die Werte vor der Einführung der Profile. */
        val FIELDS = listOf(F_MOTOR, F_SCOPE, F_FOCUS, F_BIKE, F_RIDER, F_POWER, F_CADENCE)
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
