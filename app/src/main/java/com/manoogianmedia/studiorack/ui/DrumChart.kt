package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val drumMetadata = setOf("title", "artist", "tempo", "time", "feel", "subdivision")
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
        val note = line.replace(Regex("^(cue|fill|instruction|note|sticking|dynamics)\\s*:\\s*", RegexOption.IGNORE_CASE)) {
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
        if (format == "drum_staff") {
            DrumSongForm(sections)
            sections.forEach { section ->
                DrumStaffSection(section, document.metadata["time"].orEmpty().ifBlank { "4/4" })
            }
        } else sections.forEach { section ->
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
private fun DrumSongForm(sections: List<DrumChartSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("SONG FORM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            sections.forEach { section ->
                Column(
                    Modifier.width(116.dp).height(68.dp).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .28f)).padding(7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(section.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    if (section.bars > 0) Text("${section.bars} bars", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DrumStaffSection(section: DrumChartSection, timeSignature: String) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .55f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .28f)), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(section.name.uppercase(), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                if (section.bars > 0) Text("${section.bars} bars", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Text("Percussion staff  •  $timeSignature", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                DrumStaffNotation(section.rows, timeSignature)
            }
            if (section.notes.isNotEmpty()) Text(section.notes.joinToString("  •  "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun drumStaffY(label: String): Float = when (label.uppercase()) {
    "CR", "CC", "RC" -> 24f
    "HH", "OH" -> 36f
    "RD" -> 44f
    "T1", "HT" -> 57f
    "T2", "MT" -> 69f
    "SD", "SN", "RS" -> 81f
    "T3", "LT" -> 93f
    "FT" -> 105f
    "BD", "KD", "K" -> 117f
    "HF" -> 125f
    else -> 81f
}

@Composable
private fun DrumStaffNotation(rows: List<DrumPatternRow>, timeSignature: String) {
    if (rows.isEmpty()) return
    val notationColor = MaterialTheme.colorScheme.onSurface
    val softColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f)
    val accentColor = MaterialTheme.colorScheme.primary
    val staffWidth = 740.dp
    Box(Modifier.width(staffWidth).height(154.dp)) {
    Canvas(Modifier.fillMaxSize()) {
        fun fy(value: Float) = size.height * value / 154f
        val left = 86.dp.toPx()
        val right = size.width - 10.dp.toPx()
        listOf(57f, 69f, 81f, 93f, 105f).forEach { y ->
            drawLine(softColor, Offset(left, fy(y)), Offset(right, fy(y)), 1.2.dp.toPx())
        }
        drawLine(notationColor, Offset(63.dp.toPx(), fy(65f)), Offset(63.dp.toPx(), fy(99f)), 4.dp.toPx())
        drawLine(notationColor, Offset(72.dp.toPx(), fy(65f)), Offset(72.dp.toPx(), fy(99f)), 4.dp.toPx())

        val steps = rows.maxOfOrNull { it.pattern.length }?.coerceIn(4, 32) ?: 8
        val spacing = (right - left) / (steps - 1).coerceAtLeast(1)
        val groupSize = (steps / 4).coerceAtLeast(1)
        val eventGrid = List(steps) { index ->
            rows.mapNotNull { row -> row.pattern.getOrNull(index)?.takeUnless { it in "-_. " }?.let { row.label.uppercase() to it } }
        }
        val active = MutableList<Float?>(steps) { null }
        repeat(steps) { index ->
            val x = left + spacing * index
            val events = eventGrid[index]
            val groupStart = (index / groupSize) * groupSize
            val groupEvents = eventGrid.subList(groupStart, (groupStart + groupSize).coerceAtMost(steps))
            val groupActive = groupEvents.indices.filter { groupEvents[it].isNotEmpty() }
            if (events.isEmpty()) {
                if (index == groupStart && (groupActive.isEmpty() || groupActive.first() > 0)) {
                    val rest = Path().apply {
                        moveTo(x - 4.dp.toPx(), fy(74f)); quadraticTo(x + 6.dp.toPx(), fy(79f), x, fy(87f)); quadraticTo(x - 6.dp.toPx(), fy(93f), x + 2.dp.toPx(), fy(100f))
                    }
                    drawPath(rest, softColor, style = Stroke(1.8.dp.toPx()))
                }
            } else {
                active[index] = x
                val positions = events.map { drumStaffY(it.first) }
                val grouped = groupActive.size > 1
                val beamY = fy(if (grouped) 20f else ((positions.minOrNull() ?: 57f) - 26f).coerceAtLeast(18f))
                drawLine(notationColor, Offset(x + 6.dp.toPx(), fy(positions.maxOrNull() ?: 81f)), Offset(x + 6.dp.toPx(), beamY), 1.7.dp.toPx())
                events.forEach { (label, mark) ->
                    val y = fy(drumStaffY(label))
                    val cymbal = label in setOf("CR", "CC", "RC", "HH", "OH", "RD") || mark in "xX"
                    if (cymbal) {
                        drawLine(notationColor, Offset(x - 5.dp.toPx(), y - 4.dp.toPx()), Offset(x + 5.dp.toPx(), y + 4.dp.toPx()), 1.8.dp.toPx())
                        drawLine(notationColor, Offset(x + 5.dp.toPx(), y - 4.dp.toPx()), Offset(x - 5.dp.toPx(), y + 4.dp.toPx()), 1.8.dp.toPx())
                    } else {
                        drawOval(if (mark in "gG") Color.Transparent else notationColor, Offset(x - 6.dp.toPx(), y - 4.dp.toPx()), Size(12.dp.toPx(), 8.dp.toPx()), style = if (mark in "gG") Stroke(1.5.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill)
                    }
                    if (mark in ">A") {
                        val accent = Path().apply { moveTo(x - 7.dp.toPx(), beamY - 8.dp.toPx()); lineTo(x, beamY - 12.dp.toPx()); lineTo(x + 7.dp.toPx(), beamY - 8.dp.toPx()) }
                        drawPath(accent, accentColor, style = Stroke(2.dp.toPx()))
                    }
                    if (mark in "fF") drawCircle(notationColor, 2.5.dp.toPx(), Offset(x - 9.dp.toPx(), y - 7.dp.toPx()))
                }
                if (!grouped && index > groupStart) {
                    val flag = Path().apply { moveTo(x + 6.dp.toPx(), beamY); quadraticTo(x + 18.dp.toPx(), beamY + 5.dp.toPx(), x + 12.dp.toPx(), beamY + 17.dp.toPx()) }
                    drawPath(flag, notationColor, style = Stroke(1.8.dp.toPx()))
                }
            }
        }
        val beamCount = if (steps >= 16) 2 else 1
        for (start in 0 until steps step groupSize) {
            val group = active.subList(start, (start + groupSize).coerceAtMost(steps)).filterNotNull()
            if (group.size > 1) repeat(beamCount) { beam ->
                drawLine(notationColor, Offset(group.first() + 5.dp.toPx(), fy(18f + beam * 5f)), Offset(group.last() + 7.dp.toPx(), fy(18f + beam * 5f)), 4.dp.toPx())
            }
        }
    }
        val signature = timeSignature.split("/", limit = 2).let { it.firstOrNull().orEmpty().ifBlank { "4" } to it.getOrNull(1).orEmpty().ifBlank { "4" } }
        Column(Modifier.offset(x = 75.dp, y = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(signature.first, color = notationColor, fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 16.sp)
            Text(signature.second, color = notationColor, fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 16.sp)
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

