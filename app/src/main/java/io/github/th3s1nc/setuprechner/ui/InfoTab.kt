package io.github.th3s1nc.setuprechner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.AppLanguage
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.SetupCalculator

/** Info: Rechenweg, Tipps zum Übertragen, Assist-Level-Tabelle und Quelle. */
@Composable
internal fun InfoTab(state: SetupState) {
    val input = state.saved.toInput()
    val text = rememberSetupText()
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTitle(stringResource(R.string.info_title))

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.language)) {
                    Segmented(
                        options = listOf(AppLanguage.SYSTEM to stringResource(R.string.lang_system)) + AppLanguage.CHOICES,
                        selected = AppLanguage.current(context),
                        onSelect = { AppLanguage.choose(context, it) }
                    )
                    Hint(stringResource(R.string.lang_hint))
                }
            }
        }

        if (input != null) {
            val result = remember(input) { SetupCalculator.compute(input) }
            PanelCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Group(stringResource(R.string.how_title)) {
                        text.rules(result, input).forEachIndexed { i, rule -> Bullet("${i + 1}.", rule) }
                    }
                }
            }
            PanelCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Group(stringResource(R.string.transfer_title)) {
                        for (tip in text.tips(result)) Bullet("•", tip)
                    }
                }
            }
        }

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.al_title)) {
                    for (i in 1..8) {
                        Row {
                            AlCell(stringResource(R.string.level_n, i.toString()), text.percent(SetupCalculator.AL[i]))
                            if (i + 8 <= 15) {
                                AlCell(stringResource(R.string.level_n, (i + 8).toString()), text.percent(SetupCalculator.AL[i + 8]))
                            } else {
                                Box(Modifier.weight(1f))
                            }
                        }
                    }
                    Hint(stringResource(R.string.al_hint))
                }
            }
        }

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.source_title)) {
                    Hint(stringResource(R.string.source_text))
                    Hint(stringResource(R.string.source_consent))
                    Hint(stringResource(R.string.source_tune))
                }
            }
        }

        PanelCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Group(stringResource(R.string.about_title)) {
                    Hint(
                        if (version != null) stringResource(R.string.about_version, version)
                        else stringResource(R.string.about_noversion)
                    )
                    Hint(stringResource(R.string.about_brand))
                    Hint(stringResource(R.string.about_privacy))
                }
            }
        }
    }
}

@Composable
private fun RowScope.AlCell(level: String, pct: String) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.weight(1f).padding(end = 16.dp)) {
        Text(level, fontSize = 14.sp, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(pct, fontSize = 14.sp, color = cs.onSurface, textAlign = TextAlign.End)
    }
}
