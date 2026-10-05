package io.github.th3s1nc.setuprechner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.th3s1nc.setuprechner.AppLanguage
import io.github.th3s1nc.setuprechner.R
import io.github.th3s1nc.setuprechner.calc.CadenceRow
import java.io.ByteArrayOutputStream

internal val CardShape = RoundedCornerShape(14.dp)

private val TriangleDown = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}
private val TriangleUp = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    close()
}

/** Kleines Dreieck als Zeichen für "aufklappbar": Spitze nach unten, aufgeklappt nach oben. */
@Composable
internal fun ExpandTriangle(open: Boolean) {
    Box(
        Modifier
            .size(width = 11.dp, height = 7.dp)
            .clip(if (open) TriangleUp else TriangleDown)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}
internal val FieldShape = RoundedCornerShape(8.dp)

/** Seitentitel mit optionaler Unterzeile. */
@Composable
internal fun ScreenTitle(title: String, subtitle: String? = null) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
internal fun PanelCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outline),
        content = content
    )
}

/** Überschrift plus Inhalt innerhalb einer Karte. */
@Composable
internal fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

/** Kleine Beschriftung in Versalien, z. B. Spaltenköpfe. */
@Composable
internal fun Label(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text.uppercase(), modifier = modifier, textAlign = align,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ErrorHint(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
}

/** Kleines Etikett hinter einem Modusnamen, z. B. "NEU" oder "FEST". */
@Composable
internal fun Tag(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
        color = cs.onSurfaceVariant,
        modifier = Modifier
            .border(1.dp, cs.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
internal fun NumberField(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    isError: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(unit) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        )
    )
}

/** Zahlenfeld mit Minus- und Plus-Taste zum schnellen Durchprobieren. */
@Composable
internal fun StepperField(
    label: String,
    value: String,
    unit: String,
    isError: Boolean,
    onChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        StepButton("−", onMinus)
        NumberField(label, value, unit, modifier = Modifier.weight(1f), isError = isError, onChange = onChange)
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(FieldShape)
            .background(cs.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
    }
}

/** Auswahl aus wenigen Optionen nebeneinander. */
@Composable
internal fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(cs.background)
            .border(1.dp, cs.outline, FieldShape)
    ) {
        for ((value, label) in options) {
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) cs.surfaceVariant else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) cs.primary else cs.onSurfaceVariant
                )
            }
        }
    }
}

/** Schalter mit zwei Stellungen, in den Farben der App gezeichnet. */
@Composable
internal fun ToggleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(CircleShape)
                .background(if (checked) cs.primary else cs.surfaceVariant)
                .border(1.dp, if (checked) cs.primary else cs.onSurfaceVariant, CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(if (checked) cs.onPrimary else cs.onSurfaceVariant))
        }
    }
}

/** Kästchen zum Abhaken einer Stufe. [label] ist die Ansage für Vorlesehilfen. */
@Composable
internal fun CheckBox(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(FieldShape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(shape)
                .background(if (checked) cs.primary else Color.Transparent)
                .border(2.dp, if (checked) cs.primary else cs.onSurfaceVariant, shape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Text("✓", fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
        }
    }
}

/** Kleine Plus- oder Minus-Taste für die Regler unter "Erweitert". */
@Composable
internal fun SmallStep(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(FieldShape)
            .background(cs.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = if (enabled) cs.onSurface else cs.outline)
    }
}

/** Regler mit Minus, Wert und Plus. [changed] hebt einen von Hand geänderten Wert hervor. */
@Composable
internal fun Stepper(value: String, changed: Boolean, canMinus: Boolean, canPlus: Boolean, valueWidth: Dp, onMinus: () -> Unit, onPlus: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallStep("−", canMinus, onMinus)
        Text(
            value, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
            color = if (changed) cs.primary else cs.onSurface, modifier = Modifier.width(valueWidth)
        )
        SmallStep("+", canPlus, onPlus)
    }
}

/** Balken für die Motorleistung: kräftig = im Flachen, hell = am Anstieg. Werte 0..1. */
@Composable
internal fun Meter(flat: Float, climb: Float, color: Color, height: Dp, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(cs.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(climb.coerceIn(0f, 1f))
                .clip(shape)
                .background(color.copy(alpha = 0.45f))
        )
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(flat.coerceIn(0f, 1f))
                .clip(shape)
                .background(color)
        )
    }
}

/** Motorleistung über sechs Trittfrequenzen, die Lieblings-Trittfrequenz hervorgehoben. */
@Composable
internal fun CadenceTable(rows: List<CadenceRow>, twoColumns: Boolean, lastHeader: String, last: (CadenceRow) -> String) {
    val cs = MaterialTheme.colorScheme
    val text = rememberSetupText()
    Column {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Label(stringResource(R.string.th_rpm), Modifier.weight(1f))
            if (twoColumns) {
                Label(stringResource(R.string.th_watt_flat), Modifier.weight(1.2f), TextAlign.End)
                Label(stringResource(R.string.th_watt_climb), Modifier.weight(1.3f), TextAlign.End)
            } else {
                Label(stringResource(R.string.th_watt_motor), Modifier.weight(1.3f), TextAlign.End)
            }
            Label(lastHeader, Modifier.weight(1f), TextAlign.End)
        }
        for (r in rows) {
            val weight = if (r.preferred) FontWeight.SemiBold else FontWeight.Normal
            HorizontalDivider(color = cs.outline)
            Row(
                Modifier
                    .background(if (r.preferred) cs.surfaceVariant else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Cell(r.rpm.toString(), 1f, weight, TextAlign.Start)
                if (twoColumns) {
                    Cell(text.watt(r.flat), 1.2f, weight)
                    Cell(text.watt(r.climb), 1.3f, weight)
                } else {
                    Cell(text.watt(r.climb), 1.3f, weight)
                }
                Cell(last(r), 1f, weight)
            }
        }
    }
}

@Composable
private fun RowScope.Cell(text: String, weight: Float, fontWeight: FontWeight, align: TextAlign = TextAlign.End) {
    Text(
        text, fontSize = 14.sp, fontWeight = fontWeight, textAlign = align,
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(weight)
    )
}

@Composable
internal fun Bullet(mark: String, text: String) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(mark, fontSize = 14.sp, color = cs.onSurfaceVariant)
        Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = cs.onSurface, modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------- Teilen

internal fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}

internal fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.clip_label), text))
    // Ab Android 13 zeigt das System selbst eine Bestätigung.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }
}

// ---------------------------------------------------------------- Dateien und Drucken

/** Schreibt einen Text in eine vom Nutzer gewählte Datei. false, wenn das nicht gelingt. */
internal fun writeText(context: Context, uri: Uri, text: String): Boolean =
    try {
        // "wt" kürzt eine vorhandene Datei. Mit "w" allein blieben ab Android 10 alte Reste am Ende stehen.
        val out = context.contentResolver.openOutputStream(uri, "wt")
        if (out == null) false
        else {
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            true
        }
    } catch (e: Exception) {
        false
    }

/** Liest eine vom Nutzer gewählte Datei als Text. null, wenn das nicht gelingt oder die Datei zu groß ist. */
internal fun readText(context: Context, uri: Uri): String? {
    try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BACKUP_BYTES) return null
            }
            // Manche Editoren setzen ein Kennzeichen für UTF-8 an den Anfang
            return String(out.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF")
        }
    } catch (e: Exception) {
        return null
    }
}

private const val MAX_BACKUP_BYTES = 1_000_000

// Hält die Seite fest, bis der Druckdialog sie übernommen hat
private var printView: WebView? = null

/**
 * Öffnet den Druckdialog des Systems mit einer HTML-Seite. Dort lässt sie sich drucken oder als PDF sichern.
 * false, wenn das Gerät nicht drucken kann.
 */
internal fun printHtml(context: Context, html: String, jobName: String): Boolean {
    val activity = AppLanguage.findActivity(context) ?: return false
    return try {
        val view = WebView(activity)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(page: WebView, url: String?) {
                // Manche WebView-Versionen melden das Laden zweimal. Gedruckt wird nur einmal.
                if (printView !== page) return
                try {
                    val manager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    manager.print(jobName, page.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    Toast.makeText(activity, activity.getString(R.string.print_failed), Toast.LENGTH_SHORT).show()
                }
                printView = null
            }
        }
        printView = view
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        true
    } catch (e: Exception) {
        printView = null
        false
    }
}
