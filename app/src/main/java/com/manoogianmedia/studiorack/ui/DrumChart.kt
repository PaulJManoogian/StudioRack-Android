package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
private val drumSectionPattern = Regex("^\\[([^\\]]+)\\](?:\\s*(?:x|bars?\\s*=?)?\\s*(\\d+))?(?:\\s*bars?)?$", RegexOption.IGNORE_CASE)
private val drumMetadataPattern = Regex("^\\{([^}:]+):\\s*(.*)\\}$")

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
internal fun DrumChartDocumentView(source: String, format: String, performanceMode: Boolean = false) {
    val document = remember(source) { parseDrumChart(source) }
    val sections = remember(document, format) {
        if (document.sections.isNotEmpty()) document.sections
        else listOf(DrumChartSection(if (format == "drum_tab") "Drum Tab" else "Chart", rows = document.rows, notes = document.notes))
    }
    if (performanceMode && format == "drum_staff") {
        PerformanceDrumStaffChart(document, sections)
        return
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section.name.uppercase(), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        if (section.bars > 0) Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = .14f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .55f)), shape = RoundedCornerShape(50)) {
                            Text("${section.bars} ${if (section.bars == 1) "bar" else "bars"}", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                    DrumPatternGrid(section.rows)
                    if (section.notes.isNotEmpty()) Text(section.notes.joinToString("  •  "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PerformanceDrumStaffChart(document: DrumChartDocument, sections: List<DrumChartSection>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = (maxWidth.value / 92f).toInt().coerceIn(1, 6)
        val formRows = ((sections.size + columns - 1) / columns).coerceAtLeast(1)
        val uniqueRows = sections.flatMap { it.rows }.distinctBy { it.label.uppercase() }
        val keyLines = ((uniqueRows.size + 3) / 4).coerceAtLeast(1).coerceAtMost(2)
        val overhead = 18f + (formRows * 40f) + (keyLines * 16f) + 12f
        val sectionSlot = performanceDrumSectionSlot(maxHeight.value, overhead, sections.size)
        val staffHeight = (sectionSlot - 42f).coerceIn(64f, 126f).dp

        Column(
            Modifier.fillMaxSize().padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            DrumSongForm(sections, compact = true)
            DrumStaffKey(uniqueRows, compact = true)
            sections.forEach { section ->
                DrumStaffSection(
                    section = section,
                    timeSignature = document.metadata["time"].orEmpty().ifBlank { "4/4" },
                    compact = true,
                    showKey = false,
                    staffHeight = staffHeight,
                    sectionHeight = sectionSlot.dp,
                )
            }
        }
    }
}

internal fun performanceDrumSectionSlot(availableHeight: Float, overhead: Float, sectionCount: Int): Float {
    if (sectionCount <= 0) return 0f
    return ((availableHeight - overhead).coerceAtLeast(0f) / sectionCount).coerceAtLeast(76f)
}

@Composable
private fun DrumSongForm(sections: List<DrumChartSection>, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("SONG FORM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (compact) 9.sp else 11.sp, fontWeight = FontWeight.Black)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = (maxWidth.value / if (compact) 92f else 104f).toInt().coerceIn(1, if (compact) 6 else 4)
            val cellWidth = maxWidth / columns
            Column {
                sections.chunked(columns).forEach { rowSections ->
                    Row(Modifier.fillMaxWidth()) {
                        rowSections.forEach { section ->
                            Column(
                                Modifier.width(cellWidth).height(if (compact) 40.dp else 64.dp).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .28f)).padding(if (compact) 3.dp else 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(section.name, color = MaterialTheme.colorScheme.onSurface, fontSize = if (compact) 10.sp else 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (section.bars > 0) Text("${section.bars} ${if (section.bars == 1) "bar" else "bars"}", color = MaterialTheme.colorScheme.secondary, fontSize = if (compact) 9.sp else 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrumStaffSection(
    section: DrumChartSection,
    timeSignature: String,
    compact: Boolean = false,
    showKey: Boolean = true,
    staffHeight: Dp = 154.dp,
    sectionHeight: Dp? = null,
) {
    val sectionModifier = if (sectionHeight == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(sectionHeight)
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .55f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .28f)), shape = RoundedCornerShape(7.dp), modifier = sectionModifier) {
        Column(Modifier.padding(if (compact) 5.dp else 9.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(section.name.uppercase(), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = if (compact) 13.sp else 17.sp, maxLines = 1)
                if (section.bars > 0) Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = .14f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .55f)), shape = RoundedCornerShape(50)) {
                    Text("${section.bars} ${if (section.bars == 1) "bar" else "bars"}", Modifier.padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 2.dp else 4.dp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = if (compact) 10.sp else 14.sp)
                }
            }
            if (!compact) Text("Percussion staff  •  $timeSignature", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (showKey) DrumStaffKey(section.rows)
            DrumStaffNotation(section.rows, timeSignature, staffHeight, compact)
            if (section.notes.isNotEmpty()) Text(
                section.notes.joinToString("  •  "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 9.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val drumStaffNames = mapOf(
    "CR" to "Crash", "CC" to "Crash", "RC" to "Ride crash",
    "HH" to "Hi-hat", "OH" to "Open hi-hat", "RD" to "Ride",
    "T1" to "High tom", "HT" to "High tom", "T2" to "Mid tom", "MT" to "Mid tom",
    "SD" to "Snare", "SN" to "Snare", "RS" to "Rimshot",
    "T3" to "Low tom", "LT" to "Low tom", "FT" to "Floor tom",
    "BD" to "Bass drum", "KD" to "Kick", "K" to "Kick", "HF" to "Hi-hat foot",
)

@Composable
private fun DrumStaffKey(rows: List<DrumPatternRow>, compact: Boolean = false) {
    val labels = rows.map { it.label.uppercase() }.distinct()
    if (labels.isEmpty()) return
    if (compact) {
        Text(
            labels.joinToString("  •  ") { "$it ${drumStaffNames[it] ?: it.lowercase().replaceFirstChar(Char::uppercase)}" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 360.dp) 2 else 3
        val cellWidth = maxWidth / columns
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.chunked(columns).forEach { line ->
                Row(Modifier.fillMaxWidth()) {
                    line.forEach { label ->
                        Surface(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .045f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)), shape = RoundedCornerShape(4.dp), modifier = Modifier.width(cellWidth).padding(end = 4.dp)) {
                            Text("$label  ${drumStaffNames[label] ?: label.lowercase().replaceFirstChar(Char::uppercase)}", Modifier.padding(horizontal = 6.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
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
private fun DrumStaffNotation(rows: List<DrumPatternRow>, timeSignature: String, height: Dp = 154.dp, compact: Boolean = false) {
    if (rows.isEmpty()) return
    val notationColor = MaterialTheme.colorScheme.onSurface
    val softColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f)
    val accentColor = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(height)) {
    Canvas(Modifier.fillMaxSize()) {
        fun fy(value: Float) = size.height * value / 154f
        val left = 64.dp.toPx().coerceAtMost(size.width * .28f)
        val right = size.width - 6.dp.toPx()
        listOf(57f, 69f, 81f, 93f, 105f).forEach { y ->
            drawLine(softColor, Offset(left, fy(y)), Offset(right, fy(y)), 1.2.dp.toPx())
        }
        drawLine(notationColor, Offset(left - 22.dp.toPx(), fy(65f)), Offset(left - 22.dp.toPx(), fy(99f)), 4.dp.toPx())
        drawLine(notationColor, Offset(left - 14.dp.toPx(), fy(65f)), Offset(left - 14.dp.toPx(), fy(99f)), 4.dp.toPx())

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
        rows.groupBy { drumStaffY(it.label) }.toSortedMap().forEach { (position, positionRows) ->
            Text(
                positionRows.map { it.label.uppercase() }.distinct().joinToString("/"),
                modifier = Modifier.width(39.dp).offset(x = 0.dp, y = ((position - 7f) * (height.value / 154f)).dp),
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
        val signature = timeSignature.split("/", limit = 2).let { it.firstOrNull().orEmpty().ifBlank { "4" } to it.getOrNull(1).orEmpty().ifBlank { "4" } }
        Column(Modifier.offset(x = 47.dp, y = (52f * (height.value / 154f)).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(signature.first, color = notationColor, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Black, lineHeight = if (compact) 12.sp else 16.sp)
            Text(signature.second, color = notationColor, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Black, lineHeight = if (compact) 12.sp else 16.sp)
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

