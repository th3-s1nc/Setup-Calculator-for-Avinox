package io.github.th3s1nc.setuprechner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.R

private val ChipShape = RoundedCornerShape(18.dp)

/** Anzeigename eines Profils. Ohne eigenen Namen heißt es "Profil N" nach seiner Position. */
@Composable
internal fun profileName(p: RiderProfile, index: Int): String =
    if (p.name.isBlank()) stringResource(R.string.profile_default, (index + 1).toString()) else p.name

/** Reihe mit einem Feld je Profil zum Wechseln, bei Bedarf seitlich wischbar. */
@Composable
internal fun ProfileChips(state: SetupState, showAdd: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.profiles.forEachIndexed { index, p ->
            Chip(profileName(p, index), selected = p.id == state.activeId) { state.select(p.id) }
        }
        if (showAdd && state.canAdd) {
            Chip(stringResource(R.string.profile_new), selected = false, accent = true) { state.add() }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, accent: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(ChipShape)
            .background(if (selected) cs.surfaceVariant else Color.Transparent)
            .border(1.dp, if (selected) cs.primary else cs.outline, ChipShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected || accent) cs.primary else cs.onSurface
        )
    }
}

/** Karte auf der Eingabeseite: Profil wählen, anlegen, benennen und löschen. */
@Composable
internal fun ProfileCard(state: SetupState) {
    val cs = MaterialTheme.colorScheme
    val index = state.profiles.indexOfFirst { it.id == state.activeId }.coerceAtLeast(0)
    val fallbackName = stringResource(R.string.profile_default, (index + 1).toString())
    PanelCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Group(stringResource(R.string.profile_title)) {
                ProfileChips(state, showAdd = true)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.rename(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_name)) },
                    placeholder = { Text(fallbackName) },
                    singleLine = true
                )
                Hint(stringResource(R.string.profile_hint))
                if (state.canDelete) {
                    // Löschen erst nach einer zweiten Bestätigung
                    var confirm by remember(state.activeId) { mutableStateOf(false) }
                    if (!confirm) {
                        TextButton(onClick = { confirm = true }) {
                            Text(stringResource(R.string.profile_delete), color = cs.error)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.profile_delete_confirm, state.name.ifBlank { fallbackName }),
                                fontSize = 14.sp, color = cs.onSurface, modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { state.deleteActive() }) {
                                Text(stringResource(R.string.delete), color = cs.error)
                            }
                            TextButton(onClick = { confirm = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }
        }
    }
}
