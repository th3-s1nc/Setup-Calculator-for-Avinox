package io.github.th3s1nc.setuprechner.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val input = state.saved.toInput()

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
    val scale = result.motor.boostW.toDouble()
    // Es ist immer nur ein Modus aufgeklappt.
    var open by rememberSaveable { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.setup_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = cs.onBackground,
            modifier = Modifier.weight(1f))
        TextButton(onClick = { copyText(context, text.share(result, input, state.saved.name)) }) { Text(stringResource(R.string.copy)) }
        TextButton(onClick = { shareText(context, text.share(result, input, state.saved.name)) }) { Text(stringResource(R.string.share)) }
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

    PanelCard {
        Column(Modifier.animateContentSize()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Label(stringResource(R.string.col_mode), Modifier.weight(COL_MODE))
                Label(stringResource(R.string.col_level), Modifier.weight(COL_LEVEL), TextAlign.End)
                Label(stringResource(R.string.col_watt), Modifier.weight(COL_WATT), TextAlign.End)
                Label(stringResource(R.string.col_nm), Modifier.weight(COL_NM), TextAlign.End)
            }
            var zone: Zone? = null
            for (m in result.modes) {
                if (result.ladder && m.zone != null && m.zone != zone) {
                    zone = m.zone
                    ZoneRow(text.zone(m.zone))
                } else {
                    HorizontalDivider(color = cs.outline)
                }
                ModeRow(
                    name = m.name,
                    color = modeColor(m.name),
                    tag = if (result.ladder && m.custom) stringResource(R.string.tag_new) else null,
                    level = text.assistLevel(m),
                    watt = text.watt(m.watt),
                    nm = m.nm.toString(),
                    flat = (m.flat / scale).toFloat(),
                    climb = (m.climb / scale).toFloat(),
                    isOpen = open == m.name,
                    onToggle = { open = if (open == m.name) "" else m.name }
                ) {
                    Details(
                        line = text.detailLine(m, input),
                        notes = text.notes(m, input),
                        rows = m.rows,
                        twoColumns = m.isRange
                    )
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
                flat = (b.atCadence / scale).toFloat(),
                climb = (b.atCadence / scale).toFloat(),
                isOpen = open == BOOST,
                onToggle = { open = if (open == BOOST) "" else BOOST }
            ) {
                Details(
                    line = text.detailLine(b, input),
                    notes = listOf(SetupText.Note(text.boostNote(b))),
                    rows = b.rows,
                    twoColumns = false
                )
            }
        }
    }

    Hint(stringResource(R.string.tap_hint, input.cadence.toString(), text.watt(scale)))
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
    flat: Float,
    climb: Float,
    isOpen: Boolean,
    onToggle: () -> Unit,
    details: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
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
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.onSurface, maxLines = 1)
                    if (tag != null) Tag(tag)
                }
                Value(level, COL_LEVEL)
                Value(watt, COL_WATT)
                Value(nm, COL_NM)
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
private fun androidx.compose.foundation.layout.RowScope.Value(text: String, weight: Float) {
    Text(
        text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1,
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(weight)
    )
}

/** Aufgeklappter Bereich unter einer Zeile: Kennzahlen, Hinweise, Tabelle nach Trittfrequenz. */
@Composable
private fun Details(line: String, notes: List<SetupText.Note>, rows: List<CadenceRow>, twoColumns: Boolean) {
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
        HorizontalDivider(color = cs.outline)
        CadenceTable(rows, twoColumns)
    }
}
