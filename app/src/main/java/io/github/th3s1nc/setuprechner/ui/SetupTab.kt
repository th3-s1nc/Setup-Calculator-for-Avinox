package io.github.th3s1nc.setuprechner.ui

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.CadenceRow
import io.github.th3s1nc.setuprechner.calc.ModeResult
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.Scope
import io.github.th3s1nc.setuprechner.calc.SetupCalculator
import io.github.th3s1nc.setuprechner.calc.SetupInput
import io.github.th3s1nc.setuprechner.calc.SetupResult
import io.github.th3s1nc.setuprechner.calc.Zone

// Spaltenbreiten der Liste, für Kopfzeile und Zeilen gleich
private const val COL_MODE = 1.7f
private const val COL_LEVEL = 1f
private const val COL_WATT = 1f
private const val COL_NM = 0.8f

private const val BOOST = "BOOST"

/** Startseite: alle Modi als kompakte Liste zum Übertragen in die Avinox Ride App. */
@Composable
internal fun SetupTab(state: SetupState, onEdit: () -> Unit) {
    val input = state.savedInput()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (input == null) {
            InvalidInputs(state, onEdit)
        } else {
            SetupContent(state, input, onEdit)
        }
    }
}

@Composable
private fun InvalidInputs(state: SetupState, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    ScreenTitle(stringResource(R.string.setup_title))
    if (state.profiles.size > 1) ProfileChips(state, showAdd = false)
    PanelCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.setup_empty),
                fontSize = 14.sp, lineHeight = 20.sp, color = cs.onSurface
            )
            Button(onClick = onEdit) { Text(stringResource(R.string.open_inputs)) }
        }
    }
}

@Composable
private fun SetupContent(state: SetupState, input: SetupInput, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val text = rememberSetupText()
    val result = remember(input) { SetupCalculator.compute(input) }
    val scale = result.boost.watt.toDouble()
    val advanced = state.advanced
    // Tempo für die Steigung, oder null, solange W/kg angezeigt wird
    val speed: Int? = if (state.showGrade) state.speed else null
    val index = state.profiles.indexOfFirst { it.id == state.activeId }.coerceAtLeast(0)
    val shownName = if (state.profiles.size > 1 || state.saved.name.isNotBlank()) profileName(state.profiles[index], index) else ""
    // Es ist immer nur ein Modus aufgeklappt.
    var open by rememberSaveable { mutableStateOf("") }

    ScreenTitle(stringResource(R.string.setup_title))

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Teilen verschickt das Setup als Text. Die letzte Zeile ist ein Link, der dieselben Werte im Web-Rechner öffnet.
        TextButton(onClick = {
            val link = Exchange.link(state.saved, state.activeAdjust)
            shareText(context, text.share(result, input, state.saved.name) + "\n" + context.getString(R.string.share_open_web, link))
        }) { Text(stringResource(R.string.share)) }
        TextButton(onClick = {
            val html = text.sheetHtml(result, input, shownName, speed, text.today())
            if (!printHtml(context, html, context.getString(R.string.sheet_job, result.motor.label))) {
                Toast.makeText(context, context.getString(R.string.print_failed), Toast.LENGTH_SHORT).show()
            }
        }) { Text(stringResource(R.string.print)) }
    }

    if (state.profiles.size > 1) ProfileChips(state, showAdd = false)

    // Eckdaten, ein Tipp führt zu den Eingaben
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(cs.surfaceVariant)
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text.summaryMain(result, input),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface
            )
            Text(
                text.summaryDetail(result, input),
                fontSize = 13.sp, color = cs.onSurfaceVariant
            )
        }
        Text(stringResource(R.string.edit), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
    }
    if (state.dirty) {
        Text(stringResource(R.string.setup_unsaved), fontSize = 13.sp, lineHeight = 18.sp, color = WarningColor)
    }
    if (state.saved.batteryMissing) {
        // Profil aus einer Version vor der Akku-Wahl
        Text(stringResource(R.string.battery_missing, text.watt(result.boost.watt)), fontSize = 13.sp, lineHeight = 18.sp, color = WarningColor)
    }

    AdvancedBar(state, input, text)

    PanelCard {
        Column(Modifier.animateContentSize()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Label(stringResource(R.string.col_mode), Modifier.weight(COL_MODE))
                Label(stringResource(R.string.col_level), Modifier.weight(COL_LEVEL), TextAlign.End)
                Label(stringResource(R.string.col_watt), Modifier.weight(COL_WATT), TextAlign.End)
                Label(stringResource(R.string.col_nm), Modifier.weight(COL_NM), TextAlign.End)
            }
            var zone: Zone? = null
            result.modes.forEachIndexed { i, m ->
                if (result.ladder && m.zone != null && m.zone != zone) {
                    zone = m.zone
                    ZoneRow(text.zone(m.zone))
                } else {
                    HorizontalDivider(color = cs.outline)
                }
                val next = result.modes.getOrNull(i + 1)
                ModeRow(
                    name = m.name,
                    color = modeColor(m.name),
                    tag = if (result.ladder && m.custom) stringResource(R.string.tag_new) else null,
                    level = text.assistLevel(m),
                    watt = text.watt(m.watt),
                    nm = m.nm.toString(),
                    levelChanged = m.dAlLo != 0 || m.dAlHi != 0,
                    wattChanged = m.dWatt != 0,
                    nmChanged = m.dNm != 0,
                    flat = (m.flat / scale).toFloat(),
                    climb = (m.climb / scale).toFloat(),
                    checked = if (advanced) state.isChecked(m) else null,
                    checkLabel = stringResource(R.string.check_label, m.name),
                    onCheck = { state.setChecked(m, it) },
                    isOpen = open == m.name,
                    onToggle = { open = if (open == m.name) "" else m.name }
                ) {
                    Details(
                        line = text.detailLine(m, input, speed),
                        notes = text.notes(m, input, next),
                        rows = m.rows,
                        twoColumns = m.isRange,
                        lastHeader = stringResource(if (speed == null) R.string.th_wkg else R.string.th_grade),
                        last = { r -> text.lastCell(r.climb, r.wkg, input, speed) }
                    ) {
                        if (advanced) AdjustPanel(state, m, text)
                    }
                }
            }
            val b = result.boost
            HorizontalDivider(color = cs.outline)
            ModeRow(
                name = BOOST,
                color = BoostColor,
                tag = stringResource(R.string.tag_fixed),
                level = "15",
                watt = text.watt(b.watt),
                nm = b.nm.toString(),
                levelChanged = false,
                wattChanged = false,
                nmChanged = false,
                flat = (b.atCadence / scale).toFloat(),
                climb = (b.atCadence / scale).toFloat(),
                checked = null,
                checkLabel = "",
                onCheck = {},
                isOpen = open == BOOST,
                onToggle = { open = if (open == BOOST) "" else BOOST }
            ) {
                Details(
                    line = text.detailLine(b, input, speed),
                    notes = listOf(SetupText.Note(text.boostNote(b))),
                    rows = b.rows,
                    twoColumns = false,
                    lastHeader = stringResource(if (speed == null) R.string.th_wkg else R.string.th_grade),
                    last = { r -> text.lastCell(r.climb, r.wkg, input, speed) }
                ) {}
            }
        }
    }

    if (advanced) {
        val done = result.modes.count { state.isChecked(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.progress, done.toString(), result.modes.size.toString()),
                fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, modifier = Modifier.weight(1f)
            )
            if (done > 0) TextButton(onClick = { state.clearChecks() }) { Text(stringResource(R.string.clear_checks)) }
        }
        Hint(stringResource(R.string.progress_hint))
    }

    // Hinweis zum Aufklappen, dahinter ein i mit der Erklärung zu Balken und W/kg
    var barsInfo by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Hint(stringResource(R.string.tap_hint), Modifier.weight(1f))
        InfoButton(barsInfo) { barsInfo = !barsInfo }
    }
    if (barsInfo) InfoText(stringResource(R.string.bars_info, input.cadence.toString(), text.watt(input.powerW), text.watt(scale)))

    if (advanced) CompareCard(state, result, input, text, speed)
}

/** Schalter "Erweitert" und, wenn er an ist, die Wahl zwischen W/kg und Steigung mit dem Tempo dazu. */
@Composable
private fun AdvancedBar(state: SetupState, input: SetupInput, text: SetupText) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.advanced), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            if (!state.advanced) Hint(stringResource(R.string.advanced_hint))
        }
        ToggleSwitch(checked = state.advanced, onChange = { state.showAdvanced(it) })
    }
    if (state.advanced) {
        Segmented(
            options = listOf(false to stringResource(R.string.unit_wkg), true to stringResource(R.string.unit_grade)),
            selected = state.gradeUnit,
            onSelect = { state.useGrade(it) }
        )
        if (state.gradeUnit) {
            Stepper(
                value = stringResource(R.string.speed_at, state.speed.toString()),
                changed = false,
                canMinus = state.canSlower,
                canPlus = state.canFaster,
                valueWidth = 120.dp,
                onMinus = { state.stepSpeed(-1) },
                onPlus = { state.stepSpeed(1) }
            )
            Hint(stringResource(R.string.speed_note))
            Hint(stringResource(R.string.grade_short, state.speed.toString(), text.watt(input.powerW), text.plain(input.totalKg)))
        }
    }
}

@Composable
private fun ZoneRow(label: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Label(label)
    }
}

@Composable
private fun ModeRow(
    name: String,
    color: Color,
    tag: String?,
    level: String,
    watt: String,
    nm: String,
    levelChanged: Boolean,
    wattChanged: Boolean,
    nmChanged: Boolean,
    flat: Float,
    climb: Float,
    /** null = kein Kästchen (Standardrechner oder BOOST) */
    checked: Boolean?,
    checkLabel: String,
    onCheck: (Boolean) -> Unit,
    isOpen: Boolean,
    onToggle: () -> Unit,
    details: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    // Abgehakte Stufen treten zurück
    val ink = if (checked == true) cs.onSurfaceVariant else cs.onSurface
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(COL_MODE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BOOST hat kein Kästchen und wird nicht eingerückt, damit sein Etikett in eine Zeile passt
                    if (checked != null) CheckBox(checked, checkLabel, onCheck)
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    // Etikett unter dem Namen, damit auch ein längeres wie "nicht einstellbar" Platz hat
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink, maxLines = 1)
                        if (tag != null) Tag(tag)
                    }
                }
                Value(level, COL_LEVEL, if (levelChanged) cs.primary else ink)
                Value(watt, COL_WATT, if (wattChanged) cs.primary else ink)
                Value(nm, COL_NM, if (nmChanged) cs.primary else ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Meter(flat = flat, climb = climb, color = color, height = 5.dp, modifier = Modifier.weight(1f))
                // Kleines Dreieck: zeigt, dass sich die Zeile aufklappen lässt
                ExpandTriangle(isOpen)
            }
        }
        if (isOpen) details()
    }
}

@Composable
private fun RowScope.Value(text: String, weight: Float, color: Color) {
    Text(
        text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1,
        color = color, modifier = Modifier.weight(weight)
    )
}

/** Aufgeklappter Bereich unter einer Zeile: Kennzahlen, Hinweise, Tabelle nach Trittfrequenz, bei "Erweitert" darunter das Nachstellen. */
@Composable
private fun Details(
    line: String,
    notes: List<SetupText.Note>,
    rows: List<CadenceRow>,
    twoColumns: Boolean,
    lastHeader: String,
    last: (CadenceRow) -> String,
    adjust: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 6.dp)) {
        Column(
            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(line, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
            for (n in notes) {
                Text(
                    n.text, fontSize = 13.sp, lineHeight = 18.sp,
                    color = if (n.warning) WarningColor else cs.onSurfaceVariant
                )
            }
        }
        // Erst die Tabelle wie im Standardrechner, darunter das Nachstellen. So sieht man beim
        // Verstellen direkt, was sich in der Tabelle ändert.
        HorizontalDivider(color = cs.outline)
        CadenceTable(rows, twoColumns, lastHeader, last)
        adjust()
    }
}

/** Werte eines Modus von Hand nachstellen, in den Schritten der Avinox Ride App: Level 1, Watt 50, Nm 5. */
@Composable
private fun AdjustPanel(state: SetupState, m: ModeResult, text: SetupText) {
    val cs = MaterialTheme.colorScheme
    HorizontalDivider(color = cs.outline)
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.adjust_title), Modifier.weight(1f))
            if (m.adjusted) Tag(stringResource(R.string.tag_adjusted))
        }
        if (m.twoLevels) {
            AdjustRow(state, m, AdjustField.AL_LO, 1, stringResource(R.string.adj_al_lo), m.alLo.toString(), m.calcAlLo.toString(), m.dAlLo != 0)
            AdjustRow(state, m, AdjustField.AL_HI, 1, stringResource(R.string.adj_al_hi), m.alHi.toString(), m.calcAlHi.toString(), m.dAlHi != 0)
        } else {
            AdjustRow(state, m, AdjustField.AL, 1, stringResource(R.string.adj_al), m.alHi.toString(), m.calcAlHi.toString(), m.dAlHi != 0)
        }
        AdjustRow(state, m, AdjustField.WATT, 50, stringResource(R.string.adj_watt), text.watt(m.watt), text.watt(m.calcWatt), m.dWatt != 0)
        AdjustRow(state, m, AdjustField.NM, 5, stringResource(R.string.adj_nm), m.nm.toString(), m.calcNm.toString(), m.dNm != 0)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hint(stringResource(R.string.adj_hint), Modifier.weight(1f))
            TextButton(onClick = { state.resetAdjust(m.name) }, enabled = m.adjusted) { Text(stringResource(R.string.adj_reset)) }
        }
    }
}

@Composable
private fun AdjustRow(
    state: SetupState, m: ModeResult, field: AdjustField, step: Int,
    label: String, value: String, calculated: String, changed: Boolean
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = cs.onSurface)
            if (changed) Hint(stringResource(R.string.adj_calc, calculated))
        }
        Stepper(
            value = value,
            changed = changed,
            canMinus = state.canStep(m, field, -step),
            canPlus = state.canStep(m, field, step),
            valueWidth = 64.dp,
            onMinus = { state.step(m, field, -step) },
            onPlus = { state.step(m, field, step) }
        )
    }
}

// ---------------------------------------------------------------- Vergleich

private class CompareOption(val key: String, val label: String, val input: SetupInput)

/** Stellt ein zweites Setup daneben: eine andere Ausrichtung oder den gespeicherten Stand eines anderen Profils. */
@Composable
private fun CompareCard(state: SetupState, result: SetupResult, input: SetupInput, text: SetupText, speed: Int?) {
    val cs = MaterialTheme.colorScheme
    val options = ArrayList<CompareOption>()
    if (input.scope == Scope.FOUR) {
        for (f in Profile.values()) {
            if (f != input.profile) {
                options.add(CompareOption("focus:" + f.name, stringResource(R.string.cmp_focus, text.profile(f)), input.copy(profile = f, adjust = emptyMap())))
            }
        }
    }
    state.profiles.forEachIndexed { i, p ->
        if (p.id != state.activeId) {
            val other = state.inputOf(p.id)
            if (other != null) options.add(CompareOption("profile:" + p.id, profileName(p, i), other))
        }
    }
    var pick by rememberSaveable { mutableStateOf("") }
    val chosen = options.firstOrNull { it.key == pick }
    val index = state.profiles.indexOfFirst { it.id == state.activeId }.coerceAtLeast(0)
    val ownName = profileName(state.profiles[index], index)

    PanelCard {
        Column(Modifier.animateContentSize()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.cmp_title)) {
                    Hint(stringResource(R.string.cmp_sub))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip(stringResource(R.string.cmp_off), selected = chosen == null) { pick = "" }
                        for (o in options) Chip(o.label, selected = chosen != null && chosen.key == o.key) { pick = o.key }
                    }
                    if (state.profiles.size < 2 && chosen == null) Hint(stringResource(R.string.cmp_none))
                }
            }
            if (chosen != null) {
                val other = remember(chosen.input) { SetupCalculator.compute(chosen.input) }
                HorizontalDivider(color = cs.outline)
                Row(Modifier.background(cs.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Label(stringResource(R.string.col_mode), Modifier.weight(COMPARE_MODE))
                    CompareHead(ownName, text.compareLine(result, input))
                    CompareHead(chosen.label, text.compareLine(other, chosen.input))
                }
                val names = ArrayList<String>()
                for (m in result.modes) names.add(m.name)
                for (m in other.modes) if (m.name !in names) names.add(m.name)
                for (n in names) {
                    val a = result.modes.firstOrNull { it.name == n }
                    val b = other.modes.firstOrNull { it.name == n }
                    HorizontalDivider(color = cs.outline)
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                        CompareName(n, modeColor(n))
                        CompareCell(
                            level = if (a == null) null else text.assistLevel(a), watt = a?.watt, nm = a?.nm,
                            sub = if (a == null) "" else text.power(a) + " · " + text.last(a.climb, a.wkg, input, speed),
                            refLevel = null, refWatt = null, refNm = null, text = text
                        )
                        CompareCell(
                            level = if (b == null) null else text.assistLevel(b), watt = b?.watt, nm = b?.nm,
                            sub = if (b == null) "" else text.power(b) + " · " + text.last(b.climb, b.wkg, chosen.input, speed),
                            refLevel = if (a == null) null else text.assistLevel(a), refWatt = a?.watt, refNm = a?.nm, text = text
                        )
                    }
                }
                val ba = result.boost
                val bb = other.boost
                HorizontalDivider(color = cs.outline)
                Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                    CompareName(BOOST, BoostColor)
                    CompareCell(
                        level = "15", watt = ba.watt, nm = ba.nm,
                        sub = stringResource(R.string.power_single, text.watt(ba.atCadence)) + " · " + text.last(ba.atCadence, ba.wkg, input, speed),
                        refLevel = null, refWatt = null, refNm = null, text = text
                    )
                    CompareCell(
                        level = "15", watt = bb.watt, nm = bb.nm,
                        sub = stringResource(R.string.power_single, text.watt(bb.atCadence)) + " · " + text.last(bb.atCadence, bb.wkg, chosen.input, speed),
                        refLevel = "15", refWatt = ba.watt, refNm = ba.nm, text = text
                    )
                }
            }
        }
    }
}

private const val COMPARE_MODE = 0.8f

@Composable
private fun RowScope.CompareHead(title: String, line: String) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.weight(1f).padding(start = 8.dp)) {
        Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface, maxLines = 2)
        Text(line, fontSize = 11.sp, lineHeight = 15.sp, color = cs.onSurfaceVariant, maxLines = 4)
    }
}

@Composable
private fun RowScope.CompareName(name: String, color: Color) {
    Row(
        modifier = Modifier.weight(COMPARE_MODE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

/** Eine Seite des Vergleichs. Mit den Werten der anderen Seite ([refLevel] usw.) werden Abweichungen farbig. */
@Composable
private fun RowScope.CompareCell(
    level: String?, watt: Int?, nm: Int?, sub: String,
    refLevel: String?, refWatt: Int?, refNm: Int?, text: SetupText
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.weight(1f).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (level == null || watt == null || nm == null) {
            Text("–", fontSize = 15.sp, color = cs.onSurfaceVariant)
        } else {
            Text(
                stringResource(R.string.cmp_level, level), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (refLevel != null && refLevel != level) cs.primary else cs.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.cmp_watt, text.watt(watt)), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    color = if (refWatt != null && refWatt != watt) cs.primary else cs.onSurface
                )
                Text(
                    stringResource(R.string.cmp_nm, nm.toString()), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    color = if (refNm != null && refNm != nm) cs.primary else cs.onSurface
                )
            }
            Text(sub, fontSize = 11.sp, lineHeight = 15.sp, color = cs.onSurfaceVariant, maxLines = 3)
        }
    }
}
