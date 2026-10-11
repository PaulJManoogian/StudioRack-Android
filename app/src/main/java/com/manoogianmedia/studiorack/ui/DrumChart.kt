package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class DrumPatternRow(val label: String, val pattern: String)
internal data class DrumChartSection(
    val name: String,
    val bars: Int = 0,
    val rows: List<DrumPatternRow> = emptyList(),
    val notes: List<String> = emptyList(),
)
internal data class DrumChartDocument(
    val metadata: Map<String, String>,
    val sections: List<DrumChartSection>,
    val rows: List<DrumPatternRow>,
    val notes: List<String>,
)

private val drumMetadata = setOf("title", "artist", "tempo", "time", "feel")
private val drumRowPattern = Regex("(?:^|\\s)([A-Za-z][A-Za-z0-9 +#/-]{0,15})\\|([^|]+)\\|")
private val drumSectionPattern = Regex("^\\[([^]]+)](?:\\s*(?:x|bars?\\s*=?)?\\s*(\\d+))?(?:\\s*bars?)?$", RegexOption.IGNORE_CASE)
private val drumMetadataPattern = Regex("^\\{([^}:]+):\\s*(.*)}$")

internal fun parseDrumChart(source: String): DrumChartDocument {
    val metadata = linkedMapOf<String, String>()
    val sections = mutableListOf<MutableDrumSection>()
    val looseRows = mutableListOf<DrumPatternRow>()
    val looseNotes = mutableListOf<String>()
    var current: MutableDrumSection? = null
    source.replace("\r\n", "\n").lines().forEach { raw ->
        val line = raw.trim()
        if (line.isBlank() || line.startsWith("#")) return@forEach
        drumMetadataPattern.matchEntire(line)?.let { match ->
            val key = match.groupValues[1].trim().lowercase()
            if (key in drumMetadata) {
                metadata[key] = match.groupValues[2].trim()
                return@forEach
            }
        }
        drumSectionPattern.matchEntire(line)?.let { match ->
            current = MutableDrumSection(match.groupValues[1].trim(), match.groupValues[2].toIntOrNull() ?: 0)
            sections += requireNotNull(current)
            return@forEach
        }
        val patternSource = line.replace(Regex("^(?:pattern|groove)\\s*:?\\s*", RegexOption.IGNORE_CASE), "")
        val rows = drumRowPattern.findAll(patternSource).mapNotNull { match ->
            val label = match.groupValues[1].trim()
            val pattern = match.groupValues[2].filterNot(Char::isWhitespace).take(64)
            if (label.isBlank() || pattern.isBlank()) null else DrumPatternRow(label, pattern)
        }.toList()
        if (rows.isNotEmpty()) {
            (current?.rows ?: looseRows).addAll(rows)
            return@forEach
        }
        val note = line.replace(Regex("^(cue|fill|instruction|note)\\s*:\\s*", RegexOption.IGNORE_CASE)) {
            "${it.groupValues[1].replaceFirstChar(Char::uppercase)}: "
        }.take(240)
        (current?.notes ?: looseNotes).add(note)
    }
    return DrumChartDocument(metadata, sections.map(MutableDrumSection::freeze), looseRows, looseNotes)
}

private data class MutableDrumSection(
    val name: String,
    val bars: Int,
    val rows: MutableList<DrumPatternRow> = mutableListOf(),
    val notes: MutableList<String> = mutableListOf(),
) {
    fun freeze() = DrumChartSection(name, bars, rows.toList(), notes.toList())
}

@Composable
internal fun DrumChartDocumentView(source: String, format: String) {
    val document = remember(source) { parseDrumChart(source) }
    val sections = remember(document, format) {
        if (document.sections.isNotEmpty()) document.sections
        else listOf(DrumChartSection(if (format == "drum_tab") "Drum Tab" else "Chart", rows = document.rows, notes = document.notes))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        document.metadata["title"]?.takeIf(String::isNotBlank)?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        val facts = listOf("tempo" to "BPM", "time" to "Time", "feel" to "Feel").mapNotNull { (key, label) ->
            document.metadata[key]?.takeIf(String::isNotBlank)?.let { "$label  $it" }
        }
        if (facts.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            facts.forEach { fact ->
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = .1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .35f)), shape = RoundedCornerShape(6.dp)) {
                    Text(fact, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        sections.forEach { section ->
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .55f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .18f)), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(section.name.uppercase(), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        if (section.bars > 0) Text("${section.bars} bars", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    DrumPatternGrid(section.rows)
                    if (section.notes.isNotEmpty()) Text(section.notes.joinToString("  •  "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DrumPatternGrid(rows: List<DrumPatternRow>) {
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .26f), RoundedCornerShape(5.dp))) {
        rows.forEachIndexed { rowIndex, row ->
            Row(Modifier.background(Color(0x6604090F)), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(44.dp).height(34.dp).border(0.5.dp, Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                    Text(row.label, color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                row.pattern.forEachIndexed { index, mark ->
                    val color = when (mark) {
                        '>', 'A' -> MaterialTheme.colorScheme.primary
                        'x', 'X' -> MaterialTheme.colorScheme.secondary
                        '-', '_', '.' -> MaterialTheme.colorScheme.onSurface.copy(alpha = .25f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(Modifier.size(34.dp).border(if ((index + 1) % 4 == 0) 1.dp else .5.dp, if ((index + 1) % 4 == 0) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                        Text(mark.toString(), color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = if (mark == '>') 18.sp else 15.sp)
                    }
                }
            }
            if (rowIndex < rows.lastIndex) Spacer(Modifier.height(1.dp))
        }
    }
}

