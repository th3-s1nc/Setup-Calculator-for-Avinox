package io.github.th3s1nc.setuprechner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.Motor
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.Scope
import io.github.th3s1nc.setuprechner.calc.SetupCalculator

/** Trittfrequenz bei 25 km/h je Gang (Guide 2.3): Kettenblatt -> Gang -> U/min */
private val GearCadence: Map<Int, Map<Int, Int>> = mapOf(
    34 to mapOf(12 to 52, 11 to 63, 10 to 73, 9 to 84, 8 to 94),
    36 to mapOf(12 to 49, 11 to 59, 10 to 69, 9 to 79, 8 to 89)
)

/** Eingaben: Motor, Fahrer und Bike, Umfang des Setups. */
@Composable
internal fun InputTab(state: SetupState, onDone: () -> Unit) {
    val text = rememberSetupText()
    val draft = state.draft
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTitle(
            stringResource(R.string.inputs_title),
            stringResource(R.string.inputs_sub)
        )

        ProfileCard(state)

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.motor)) {
                    val motors: List<Pair<Motor?, String>> = Motor.values().map { it to it.label }
                    Segmented(
                        options = motors,
                        selected = state.motor,
                        onSelect = { state.motor = it; state.edit() }
                    )
                    val m = state.motor
                    if (m != null) {
                        Hint(stringResource(R.string.motor_hint, m.nm.toString(), text.watt(m.watt), m.boostNm.toString(), text.watt(m.boostW)))
                    } else {
                        Hint(stringResource(R.string.motor_choose))
                    }
                }
            }
        }

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Group(stringResource(R.string.weight)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField(stringResource(R.string.bike), state.bike, stringResource(R.string.unit_kg), decimal = true, isError = draft.bikeBad,
                            modifier = Modifier.weight(1f)) { state.bike = it; state.edit() }
                        NumberField(stringResource(R.string.rider), state.rider, stringResource(R.string.unit_kg), decimal = true, isError = draft.riderBad,
                            modifier = Modifier.weight(1f)) { state.rider = it; state.edit() }
                    }
                    if (!draft.bikeBad && !draft.riderBad) {
                        val total = draft.totalKg
                        Hint(stringResource(R.string.weight_hint, if (total != null) text.plain(total) else "–"))
                    } else {
                        ErrorHint(stringResource(R.string.weight_error))
                    }
                }

                Group(stringResource(R.string.rider_power)) {
                    StepperField(
                        label = stringResource(R.string.power_label),
                        value = state.power,
                        unit = stringResource(R.string.unit_watt),
                        isError = draft.powerBad,
                        onChange = { state.power = it; state.edit() },
                        onMinus = { state.stepPower(-10) },
                        onPlus = { state.stepPower(10) }
                    )
                    if (!draft.powerBad) {
                        Hint(stringResource(R.string.power_hint))
                    } else {
                        ErrorHint(stringResource(R.string.power_error))
                    }
                }

                Group(stringResource(R.string.cadence_title)) {
                    StepperField(
                        label = stringResource(R.string.cadence_label),
                        value = state.cadence,
                        unit = stringResource(R.string.unit_rpm),
                        isError = draft.cadenceBad,
                        onChange = { state.cadence = it; state.edit() },
                        onMinus = { state.stepCadence(-5) },
                        onPlus = { state.stepCadence(5) }
                    )
                    if (draft.cadenceBad) ErrorHint(stringResource(R.string.cadence_error))
                    GearHelper { rpm -> state.cadence = rpm.toString(); state.edit() }
                }
            }
        }

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Group(stringResource(R.string.scope)) {
                    Segmented(
                        options = listOf(Scope.FOUR to stringResource(R.string.scope_four), Scope.ALL to stringResource(R.string.scope_all)),
                        selected = state.scope,
                        onSelect = { state.scope = it; state.edit() }
                    )
                    val chosen = state.motor
                    if (state.scope == Scope.ALL && chosen != null) {
                        val names = SetupCalculator.ladderNames(chosen)
                        Hint(stringResource(R.string.scope_all_hint, names.size.toString(), names.joinToString(", ")))
                    } else if (state.scope == Scope.ALL) {
                        Hint(stringResource(R.string.motor_choose))
                    } else {
                        Hint(stringResource(R.string.scope_four_hint))
                    }
                }

                if (state.scope == Scope.FOUR) {
                    Group(stringResource(R.string.focus)) {
                        Segmented(
                            options = Profile.values().map { it to text.profile(it) },
                            selected = state.profile,
                            onSelect = { state.profile = it; state.edit() }
                        )
                        Hint(text.profileHint(state.profile))
                    }
                }
            }
        }

        SaveBar(state, onDone)
    }
}

/** Hilfe für alle, die ihre Trittfrequenz nicht kennen: Gang bei Tempo 25 wählen. */
@Composable
private fun GearHelper(onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var open by rememberSaveable { mutableStateOf(false) }
    var ring by rememberSaveable { mutableStateOf(34) }
    var gear by rememberSaveable { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (open) R.string.gear_hide else R.string.gear_show),
            color = cs.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(FieldShape)
                .clickable { open = !open }
                .padding(vertical = 6.dp)
        )
        if (open) {
            Hint(stringResource(R.string.gear_question))
            Segmented(
                options = listOf(34 to stringResource(R.string.ring_34), 36 to stringResource(R.string.ring_36)),
                selected = ring,
                onSelect = { r ->
                    ring = r
                    GearCadence[r]?.get(gear)?.let(onPick)
                }
            )
            val gearLabels = listOf(12, 11, 10, 9, 8).map { it to stringResource(R.string.gear_option, it.toString()) }
            Segmented(
                options = gearLabels,
                selected = gear,
                onSelect = { g ->
                    gear = g
                    GearCadence[ring]?.get(g)?.let(onPick)
                }
            )
            val rpm = GearCadence[ring]?.get(gear)
            if (rpm != null) Hint(stringResource(R.string.gear_result, gear.toString(), rpm.toString()))
        }
    }
}

/** Speichern, Verwerfen und der Weg zum Setup, mit einer Zeile zum Stand der Eingaben. */
@Composable
private fun SaveBar(state: SetupState, onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dirty = state.dirty
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { state.save() }, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save), fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            OutlinedButton(onClick = onDone, enabled = state.saved.complete, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.show_setup), fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        if (dirty) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (state.canSave) R.string.status_unsaved else R.string.status_incomplete),
                    fontSize = 13.sp, lineHeight = 18.sp, color = WarningColor, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { state.discard() }) { Text(stringResource(R.string.discard)) }
            }
        } else if (state.saved.complete) {
            Text(stringResource(R.string.status_saved), fontSize = 13.sp, color = cs.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.status_incomplete), fontSize = 13.sp, lineHeight = 18.sp, color = cs.onSurfaceVariant)
        }
    }
}
