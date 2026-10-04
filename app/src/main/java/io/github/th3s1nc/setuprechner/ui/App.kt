package io.github.th3s1nc.setuprechner.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.R

/** Die drei Bereiche der App, in der Reihenfolge der unteren Leiste. */
internal enum class Tab(val labelRes: Int) {
    SETUP(R.string.tab_setup), INPUT(R.string.tab_inputs), INFO(R.string.tab_info)
}

@Composable
fun SetupApp() {
    val context = LocalContext.current
    val state = remember {
        SetupState(context.getSharedPreferences("setup", Context.MODE_PRIVATE))
    }
    // Solange für das aktive Profil nichts gespeichert ist, zuerst die Eingaben zeigen.
    var tabIndex by rememberSaveable { mutableStateOf(if (state.saved.complete) Tab.SETUP.ordinal else Tab.INPUT.ordinal) }
    val tab = Tab.values()[tabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomBar(tab) { tabIndex = it.ordinal } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.SETUP -> SetupTab(state, onEdit = { tabIndex = Tab.INPUT.ordinal })
                Tab.INPUT -> InputTab(state, onDone = { tabIndex = Tab.SETUP.ordinal })
                Tab.INFO -> InfoTab(state)
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surface) {
        Column {
            HorizontalDivider(color = cs.outline)
            Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
                for (t in Tab.values()) {
                    val selected = t == current
                    val tint = if (selected) cs.primary else cs.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(t) })
                            .padding(top = 8.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (selected) cs.primary.copy(alpha = 0.14f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            TabIcon(t, tint)
                        }
                        Text(
                            stringResource(t.labelRes), fontSize = 12.sp, color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** Einfache gezeichnete Symbole, damit keine Icon-Bibliothek nötig ist. */
@Composable
private fun TabIcon(tab: Tab, tint: Color) {
    when (tab) {
        // Aufsteigende Balken wie die Modus-Treppe
        Tab.SETUP -> Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.width(4.dp).height(7.dp).background(tint))
            Box(Modifier.width(4.dp).height(11.dp).background(tint))
            Box(Modifier.width(4.dp).height(15.dp).background(tint))
            Box(Modifier.width(4.dp).height(19.dp).background(tint))
        }
        // Zwei Schieberegler
        Tab.INPUT -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Slider(tint, left = 4, right = 10)
            Slider(tint, left = 11, right = 3)
        }
        // i im Kreis
        Tab.INFO -> Box(
            modifier = Modifier.size(20.dp).border(2.dp, tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("i", fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun Slider(tint: Color, left: Int, right: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(left.dp).height(2.dp).background(tint))
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Box(Modifier.width(right.dp).height(2.dp).background(tint))
    }
}
