package com.manoogianmedia.studiorack.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.manoogianmedia.studiorack.data.SupportingRecord
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private data class TuningPreset(val id: String, val name: String, val notes: List<String>)
private data class DrumStandard(val id: String, val name: String, val note: String, val wholeHz: Double, val batterHz: Double = wholeHz, val resonantHz: Double = wholeHz, val lugs: Int = 8, val fundamentalRange: ClosedFloatingPointRange<Double>, val batterRange: ClosedFloatingPointRange<Double>? = null, val resonantRange: ClosedFloatingPointRange<Double>? = null)
private data class TunerEnvironment(val id: String, val name: String, val description: String, val gate: Float, val confidence: Double, val smoothing: Int, val toleranceHz: Double)
private data class DrumVisualRange(val family: String, val name: String, val diameter: Int, val lowHz: Double, val highHz: Double)
private data class NoteTarget(val label: String, val frequency: Double)

private val instrumentTunings = listOf(
    TuningPreset("chromatic", "Chromatic", emptyList()),
    TuningPreset("guitar_standard", "Guitar - Standard", listOf("E2", "A2", "D3", "G3", "B3", "E4")),
    TuningPreset("guitar_drop_d", "Guitar - Drop D", listOf("D2", "A2", "D3", "G3", "B3", "E4")),
    TuningPreset("guitar_dadgad", "Guitar - DADGAD", listOf("D2", "A2", "D3", "G3", "A3", "D4")),
    TuningPreset("guitar_open_g", "Guitar - Open G", listOf("D2", "G2", "D3", "G3", "B3", "D4")),
    TuningPreset("bass_4", "Bass - 4 String", listOf("E1", "A1", "D2", "G2")),
    TuningPreset("bass_5", "Bass - 5 String", listOf("B0", "E1", "A1", "D2", "G2")),
    TuningPreset("ukulele", "Ukulele - Standard", listOf("G4", "C4", "E4", "A4")),
    TuningPreset("violin", "Violin / Mandolin", listOf("G3", "D4", "A4", "E5")),
    TuningPreset("viola_cello", "Viola / Cello", listOf("C3", "G3", "D4", "A4")),
)

private val drumStandards = listOf(
    DrumStandard("snare_14", "14 in Snare", "G3", 196.0, 266.0, 398.0, 10, 164.81..233.08, 229.0..356.0, 343.0..400.0),
    DrumStandard("tom_8", "8 in Rack Tom", "E3", 164.81, 288.0, 305.0, 6, 146.83..207.65, 250.0..350.0, 265.0..380.0),
    DrumStandard("tom_10", "10 in Rack Tom", "D3", 146.83, 221.0, 272.0, 6, 130.81..164.81, 195.0..275.0, 210.0..310.0),
    DrumStandard("tom_12", "12 in Rack Tom", "B2", 123.47, 186.0, 229.0, 6, 98.0..130.81, 149.0..220.0, 175.0..285.0),
    DrumStandard("tom_13", "13 in Rack Tom", "G2", 98.0, 150.0, 181.0, 6, 87.31..116.54, 135.0..205.0, 155.0..230.0),
    DrumStandard("floor_tom_14", "14 in Floor Tom", "G2", 98.0, 147.0, 181.0, 8, 82.41..103.83, 118.0..175.0, 130.0..225.0),
    DrumStandard("floor_tom_16", "16 in Floor Tom", "D2", 73.42, 110.0, 136.0, 8, 60.0..82.41, 88.0..130.0, 103.0..170.0),
    DrumStandard("floor_tom_18", "18 in Floor Tom", "A1", 55.0, 82.0, 103.0, 8, 49.0..71.0, 75.0..105.0, 85.0..140.0),
    DrumStandard("kick_18", "18 in Bass Drum", "G1", 49.0, 71.0, 107.0, 8, 43.65..73.42, 60.0..90.0, 80.0..120.0),
    DrumStandard("kick_20", "20 in Bass Drum", "F1", 43.65, 64.0, 96.0, 8, 38.89..65.41, 55.0..82.0, 72.0..105.0),
    DrumStandard("kick_22", "22 in Bass Drum", "D1", 36.71, 58.0, 87.0, 8, 36.0..61.74, 55.0..92.0, 65.0..110.0),
    DrumStandard("kick_24", "24 in Bass Drum", "C1", 32.70, 49.0, 74.0, 10, 30.87..55.0, 45.0..75.0, 55.0..95.0),
    DrumStandard("timpani_32", "32 in Timpani - D2 to A2", "D2", 73.42, lugs = 8, fundamentalRange = 73.42..110.0),
    DrumStandard("timpani_29", "29 in Timpani - F2 to C3", "F2", 87.31, lugs = 8, fundamentalRange = 87.31..130.81),
    DrumStandard("timpani_26", "26 in Timpani - Bb2 to F3", "Bb2", 116.54, lugs = 8, fundamentalRange = 116.54..174.61),
    DrumStandard("timpani_23", "23 in Timpani - D3 to A3", "D3", 146.83, lugs = 8, fundamentalRange = 146.83..220.0),
)

private val tunerEnvironments = listOf(
    TunerEnvironment("studio", "Studio", "Maximum sensitivity for a controlled room", .004f, .60, 7, .5),
    TunerEnvironment("quiet", "Quiet", "Practice room or quiet stage", .008f, .64, 5, 1.0),
    TunerEnvironment("noisy", "Noisy", "Nearby conversation or instruments", .018f, .71, 4, 2.0),
    TunerEnvironment("gig", "Gig", "High ambient stage level", .035f, .78, 3, 4.0),
)

private val drumVisualRanges = listOf(
    DrumVisualRange("snare", "10 in Snare", 10, 196.0, 293.5),
    DrumVisualRange("snare", "12 in Snare", 12, 174.5, 261.5),
    DrumVisualRange("snare", "13 in Snare", 13, 165.0, 247.0),
    DrumVisualRange("snare", "14 in Snare", 14, 147.0, 247.0),
    DrumVisualRange("snare", "15 in Snare", 15, 131.0, 196.0),
    DrumVisualRange("tom", "8 in Tom", 8, 146.83, 207.65),
    DrumVisualRange("tom", "10 in Tom", 10, 130.81, 164.81),
    DrumVisualRange("tom", "12 in Tom", 12, 98.0, 130.81),
    DrumVisualRange("tom", "13 in Tom", 13, 87.31, 116.54),
    DrumVisualRange("tom", "14 in Floor Tom", 14, 82.41, 103.83),
    DrumVisualRange("tom", "16 in Floor Tom", 16, 60.0, 82.41),
    DrumVisualRange("tom", "18 in Floor Tom", 18, 49.0, 71.0),
    DrumVisualRange("kick", "18 in Bass Drum", 18, 43.65, 73.42),
    DrumVisualRange("kick", "20 in Bass Drum", 20, 38.89, 65.41),
    DrumVisualRange("kick", "22 in Bass Drum", 22, 36.0, 61.74),
    DrumVisualRange("kick", "24 in Bass Drum", 24, 30.87, 55.0),
    DrumVisualRange("timpani", "23 in Timpani", 23, 146.83, 220.0),
    DrumVisualRange("timpani", "26 in Timpani", 26, 116.54, 174.61),
    DrumVisualRange("timpani", "29 in Timpani", 29, 87.31, 130.81),
    DrumVisualRange("timpani", "32 in Timpani", 32, 73.42, 110.0),
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TunerPanel(model: StudioRackViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by model.items.collectAsState()
    val categories by model.categories.collectAsState()
    val itemTypes by model.itemTypes.collectAsState()
    val brands by model.brands.collectAsState()
    val tunings by model.tunings.collectAsState()
    val engine = remember { AndroidTunerEngine() }
    var mode by remember { mutableStateOf("Tuned Instruments") }
    var instrumentPreset by remember { mutableStateOf(instrumentTunings[1]) }
    var stringIndex by remember { mutableStateOf(0) }
    var drumStandard by remember { mutableStateOf(drumStandards[0]) }
    var drumZone by remember { mutableStateOf("Whole drum") }
    var lug by remember { mutableStateOf(1) }
    var environment by remember { mutableStateOf(tunerEnvironments[1]) }
    var lugPattern by remember { mutableStateOf("Star") }
    var lugReadings by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var lockedTargetHz by remember { mutableStateOf<Double?>(null) }
    val recentReadings = remember { mutableListOf<Double>() }
    var concertPitch by remember { mutableStateOf(440f) }
    var listening by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(PitchReading()) }
    var selectedItem by remember { mutableStateOf<SupportingRecord?>(null) }
    var notes by remember { mutableStateOf("") }

    fun startListening() {
        listening = true
        engine.start(scope, environment.gate, environment.confidence) { value -> scope.launch {
            reading = if (value.frequencyHz > 0) {
                recentReadings += value.frequencyHz
                while (recentReadings.size > environment.smoothing) recentReadings.removeAt(0)
                value.copy(frequencyHz = recentReadings.sorted()[recentReadings.size / 2])
            } else value
        } }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else reading = PitchReading(state = PitchState.Uncertain)
    }
    DisposableEffect(Unit) { onDispose { engine.stop(); TunerReferenceTone.stop() } }

    val categoryNames = categories.associate { it.entityId to JSONObject(it.json).optString("name") }
    val typeNames = itemTypes.associate { it.entityId to JSONObject(it.json).optString("name") }
    val brandNames = brands.associate { it.entityId to JSONObject(it.json).optString("name") }
    fun itemWords(item: SupportingRecord): String {
        val data = JSONObject(item.json)
        return listOf(data.optString("display_name"), categoryNames[data.optString("category_id")], typeNames[data.optString("type_id")]).joinToString(" ").lowercase()
    }
    val percussionWords = listOf("drum", "percussion", "snare", "tom", "timpani", "cymbal")
    val visibleItems = items.filter { item ->
        val percussion = percussionWords.any { it in itemWords(item) }
        if (mode == "Percussion") percussion else !percussion
    }

    val nearest = nearestNote(reading.frequencyHz, concertPitch.toDouble())
    val baseTarget = if (mode == "Tuned Instruments") {
        instrumentPreset.notes.getOrNull(stringIndex)?.let { NoteTarget(it, noteFrequency(it, concertPitch.toDouble())) }
            ?: nearest
    } else {
        val hz = when (drumZone) { "Batter head", "Lug" -> drumStandard.batterHz; "Resonant head" -> drumStandard.resonantHz; else -> drumStandard.wholeHz }
        NoteTarget(if (drumZone.startsWith("Lug")) "Lug $lug" else drumStandard.note, hz)
    }
    val fixedTarget = if (mode == "Percussion" && lockedTargetHz != null) NoteTarget(if (drumZone == "Lug") "Lug $lug" else drumStandard.note, lockedTargetHz!!) else baseTarget
    val cents = if (reading.frequencyHz > 0 && fixedTarget.frequency > 0) 1200.0 * log2(reading.frequencyHz / fixedTarget.frequency) else 0.0
    val existing = selectedItem?.let { item -> tunings.firstOrNull { it.entityId == item.entityId } }?.let { JSONObject(it.json) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TUNERS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Tune on this device", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Pitch analysis stays on the device. Choose a standard, tune, then save the result with an existing gear item.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TunerChoices(listOf("Tuned Instruments", "Percussion"), mode) {
            engine.stop(); listening = false; reading = PitchReading(); selectedItem = null; recentReadings.clear(); mode = it
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (mode == "Tuned Instruments") {
                    TunerDropdown("Standard tuning", instrumentPreset.name, instrumentTunings, text = { it.name }, choose = { instrumentPreset = it; stringIndex = 0 })
                    if (instrumentPreset.notes.isNotEmpty()) TunerDropdown("String / target", instrumentPreset.notes[stringIndex], instrumentPreset.notes, text = { it }, choose = { stringIndex = instrumentPreset.notes.indexOf(it) })
                    Text("Concert pitch: A = ${concertPitch.roundToInt()} Hz", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Slider(value = concertPitch, onValueChange = { concertPitch = it }, valueRange = 430f..450f, steps = 19)
                } else {
                    TunerDropdown("Drum standard", drumStandard.name, drumStandards, text = { it.name }, choose = { drumStandard = it; lug = 1; lugReadings = emptyMap(); lockedTargetHz = null })
                    TunerDropdown("Measurement", drumZone, listOf("Whole drum", "Batter head", "Resonant head", "Lug"), text = { it }, choose = { drumZone = it; lockedTargetHz = null })
                    Text("Environment sensitivity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TunerChoices(tunerEnvironments.map { it.name }, environment.name) { name ->
                        environment = tunerEnvironments.first { it.name == name }; recentReadings.clear()
                        if (listening) { engine.stop(); listening = false }
                    }
                    Text("${environment.description}. ±${"%.1f".format(environment.toleranceHz)} Hz comparison tolerance.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    if (drumZone == "Lug") TunerChoices(listOf("Star", "Clockwise"), lugPattern) { lugPattern = it }
                    StudioButton(onClick = { if (reading.frequencyHz > 0) lockedTargetHz = reading.frequencyHz }, enabled = reading.frequencyHz > 0, kind = StudioButtonKind.Secondary) { Text("Lock Current Reading", fontWeight = FontWeight.Bold) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioButton(onClick = {
                        if (listening) { engine.stop(); listening = false }
                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Text(if (listening) "Stop Tuner" else "Start Tuner", fontWeight = FontWeight.Black) }
                    StudioButton(onClick = { TunerReferenceTone.play(fixedTarget.frequency) }, kind = StudioButtonKind.Secondary) { Text("Play Reference", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                }
                Text(tunerStatus(listening, reading.state), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        TunerDisplay(fixedTarget, nearest, reading, cents, if (mode == "Percussion") environment.toleranceHz else null)

        if (mode == "Percussion") {
            DrumTuningConsole(drumStandard, drumZone, lug, lugReadings, fixedTarget.frequency, reading.frequencyHz, environment.toleranceHz, lugPattern,
                selectLug = { lug = it },
                previous = { lug = adjacentLug(drumStandard.lugs, lug, lugPattern, -1) },
                next = { lug = adjacentLug(drumStandard.lugs, lug, lugPattern, 1) },
                capture = {
                    if (reading.frequencyHz > 0) {
                        lugReadings = lugReadings + (lug to reading.frequencyHz)
                        lug = adjacentLug(drumStandard.lugs, lug, lugPattern, 1)
                    }
                },
                clear = { lugReadings = emptyMap() },
            )
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save tuning to...", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("Store this setup and reading with gear that is already in your library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TunerDropdown("Gear item", selectedItem?.let { itemLabel(it, brandNames) } ?: "Choose gear...", visibleItems, text = { itemLabel(it, brandNames) }, choose = { selectedItem = it })
                existing?.optString("target_note")?.takeIf(String::isNotBlank)?.let { Text("Saved target: $it", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Tuning notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                StudioButton(
                    onClick = {
                        val item = selectedItem ?: return@StudioButton
                        val measured = reading.frequencyHz.takeIf { it > 0 }?.let { "%.2f Hz".format(it) } ?: "not measured"
                        val lugs = lugReadings.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}=${"%.2f".format(it.value)} Hz" }
                        val percussionDetails = if (mode == "Percussion") "; ${environment.name.lowercase()} environment; ${lugPattern.lowercase()} lug pattern${lugs.takeIf(String::isNotBlank)?.let { "; lugs $it" } ?: ""}" else ""
                        val summary = "${if (mode == "Percussion") drumStandard.name else instrumentPreset.name}; $drumZone; target ${fixedTarget.label} (${"%.2f".format(fixedTarget.frequency)} Hz); measured $measured; ${"%+.1f".format(cents)} cents$percussionDetails${notes.trim().takeIf(String::isNotBlank)?.let { "; $it" } ?: ""}"
                        val oldNotes = existing?.optString("notes").orEmpty()
                        val readingText = "${fixedTarget.label}: $measured (${"%+.1f".format(cents)} cents)"
                        model.saveTuning(item.entityId, JSONObject()
                            .put("enabled", 1)
                            .put("target_note", fixedTarget.label)
                            .put("batter", if (mode == "Percussion" && drumZone == "Batter head") readingText else existing?.optString("batter"))
                            .put("reso", if (mode == "Percussion" && drumZone == "Resonant head") readingText else existing?.optString("reso"))
                            .put("notes", listOf(oldNotes, summary).filter(String::isNotBlank).joinToString("\n").takeLast(8000)))
                    },
                    enabled = selectedItem != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Tuning", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun TunerDisplay(target: NoteTarget, detected: NoteTarget, reading: PitchReading, cents: Double, drumToleranceHz: Double? = null) {
    val position = cents.coerceIn(-50.0, 50.0).toFloat()
    val outlineColor = MaterialTheme.colorScheme.outline
    val needleColor = MaterialTheme.colorScheme.primary
    Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Target  ${target.label}  |  ${"%.2f".format(target.frequency)} Hz", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(if (reading.frequencyHz > 0) detected.label else "--", color = MaterialTheme.colorScheme.onSurface, fontSize = 68.sp, fontWeight = FontWeight.Normal)
            Text(if (reading.frequencyHz > 0) "%.2f Hz".format(reading.frequencyHz) else "-- Hz", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val y = size.height * .68f
                drawLine(outlineColor, Offset(0f, y), Offset(size.width, y), 6f, StrokeCap.Round)
                drawLine(Color(0xFF55E69B), Offset(size.width / 2, y - 22), Offset(size.width / 2, y + 13), 5f, StrokeCap.Round)
                val x = size.width * ((position + 50f) / 100f)
                drawLine(needleColor, Offset(x, y - 34), Offset(x, y + 13), 14f, StrokeCap.Round)
            }
            val tuned = reading.state == PitchState.Clear && if (drumToleranceHz != null) abs(reading.frequencyHz - target.frequency) <= drumToleranceHz else abs(cents) <= 3
            val variance = if (drumToleranceHz != null) "${"%.2f".format(abs(reading.frequencyHz - target.frequency))} Hz ${if (reading.frequencyHz < target.frequency) "low" else "high"}" else "${abs(cents).let { "%.1f".format(it) }} cents ${if (cents < 0) "flat" else "sharp"}"
            Text(if (reading.frequencyHz <= 0) "Waiting for a clear note" else if (tuned) "In tune" else variance, color = if (tuned) Color(0xFF55E69B) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confidence", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                LinearProgressIndicator(progress = { reading.confidence }, modifier = Modifier.weight(1f))
            }
            if (drumToleranceHz != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ambient input", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { (reading.inputLevel / .08f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DrumTuningConsole(
    standard: DrumStandard,
    zone: String,
    selectedLug: Int,
    readings: Map<Int, Double>,
    targetHz: Double,
    detectedHz: Double,
    toleranceHz: Double,
    pattern: String,
    selectLug: (Int) -> Unit,
    previous: () -> Unit,
    next: () -> Unit,
    capture: () -> Unit,
    clear: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${standard.name} · $zone", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(280.dp)) {
                    val radius = size.minDimension * .39f
                    drawCircle(Color(0xFF111A29), radius)
                    drawCircle(Color(0xFF59616E), radius, style = Stroke(width = 12f))
                    drawCircle(outline.copy(alpha = .35f), radius * .72f, style = Stroke(width = 2f))
                    repeat(standard.lugs) { index ->
                        val angle = -Math.PI / 2 + Math.PI * 2 * index / standard.lugs
                        val center = Offset(size.width / 2 + kotlin.math.cos(angle).toFloat() * radius, size.height / 2 + kotlin.math.sin(angle).toFloat() * radius)
                        val value = readings[index + 1]
                        val color = when {
                            index + 1 == selectedLug && zone == "Lug" -> accent
                            value == null -> Color(0xFF737B88)
                            abs(value - targetHz) <= toleranceHz -> Color(0xFF55E69B)
                            value < targetHz -> Color(0xFF4ECFF5)
                            else -> Color(0xFFFF936D)
                        }
                        drawCircle(color, 17f, center)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (zone == "Lug") "LUG $selectedLug" else zone.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (detectedHz > 0) "%.2f Hz".format(detectedHz) else "--", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Target %.2f Hz".format(targetHz), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            if (zone == "Lug") {
                TunerChoices((1..standard.lugs).map(Int::toString), selectedLug.toString()) { selectLug(it.toInt()) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioButton(previous, kind = StudioButtonKind.Secondary) { Text("Previous") }
                    StudioButton(capture, enabled = detectedHz > 0) { Text("Capture Lug", fontWeight = FontWeight.Black) }
                    StudioButton(next, kind = StudioButtonKind.Secondary) { Text("Next") }
                    StudioButton(clear, kind = StudioButtonKind.Secondary) { Text("Clear") }
                }
                Text("$pattern order · ${readings.size}/${standard.lugs} lugs captured", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            TypicalRangePanel(standard, zone, if (detectedHz > 0) detectedHz else targetHz)
        }
    }
}

@Composable
private fun TypicalRangePanel(standard: DrumStandard, zone: String, frequency: Double) {
    val range = when (zone) { "Batter head", "Lug" -> standard.batterRange; "Resonant head" -> standard.resonantRange; else -> standard.fundamentalRange }
    val state = when { range == null -> "No comparison range"; frequency < range.start -> "Below typical range"; frequency > range.endInclusive -> "Above typical range"; else -> "Within typical range" }
    val rangeText: (ClosedFloatingPointRange<Double>?) -> String = { it?.let { value -> "${pitchName(value.start)}–${pitchName(value.endInclusive)} · ${formatHz(value.start)}–${formatHz(value.endInclusive)} Hz" } ?: "Not applicable" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PRACTICAL COMPARISON", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(state, color = if (state.startsWith("Within")) Color(0xFF55E69B) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
        Text("Fundamental ${rangeText(standard.fundamentalRange)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Batter lug ${rangeText(standard.batterRange)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Resonant lug ${rangeText(standard.resonantRange)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        DrumFamilyRangeMap(standard)
        Text("Comparison bands are starting points; shell, heads, sustain, intervals, and the music determine the final sound.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun DrumFamilyRangeMap(standard: DrumStandard) {
    val family = when {
        standard.id.startsWith("snare") -> "snare"
        standard.id.startsWith("kick") -> "kick"
        standard.id.startsWith("timpani") -> "timpani"
        else -> "tom"
    }
    val ranges = drumVisualRanges.filter { it.family == family }.sortedBy { it.diameter }
    if (ranges.isEmpty()) return
    val lowMidi = kotlin.math.floor(ranges.minOf { 69 + 12 * log2(it.lowHz / 440.0) } - 1)
    val highMidi = kotlin.math.ceil(ranges.maxOf { 69 + 12 * log2(it.highHz / 440.0) } + 1)
    val position: (Double) -> Float = { hz -> ((69 + 12 * log2(hz / 440.0) - lowMidi) / (highMidi - lowMidi)).toFloat().coerceIn(0f, 1f) }
    val familyTitle = when (family) { "snare" -> "Snare sizes"; "kick" -> "Bass drum sizes"; "timpani" -> "Timpani sizes"; else -> "Tom sizes" }
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("MUSICAL RANGE MAP", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(familyTitle, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pitchName(440.0 * 2.0.pow((lowMidi - 69) / 12.0)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(pitchName(440.0 * 2.0.pow((((lowMidi + highMidi) / 2) - 69) / 12.0)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(pitchName(440.0 * 2.0.pow((highMidi - 69) / 12.0)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            ranges.forEach { item ->
                val selected = item.diameter == standard.name.substringBefore(" in").toIntOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${item.diameter} in", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.widthIn(min = 38.dp))
                        Canvas(Modifier.weight(1f).height(18.dp)) {
                            drawRoundRect(Color.White.copy(alpha = .07f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                            val left = size.width * position(item.lowHz)
                            val right = size.width * position(item.highHz)
                            drawRoundRect(if (selected) Color(0xFFFFA000) else Color(0xFF4ECFF5), topLeft = Offset(left, 0f), size = androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(4f), size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                        }
                    }
                    Text("${pitchName(item.lowHz)}–${pitchName(item.highHz)} · ${formatHz(item.lowHz)}–${formatHz(item.highHz)} Hz", color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(start = 46.dp))
                }
            }
        }
    }
}

private fun pitchName(frequency: Double): String = nearestNote(frequency, 440.0).label
private fun formatHz(frequency: Double): String = if (frequency % 1.0 == 0.0) "%.0f".format(frequency) else "%.1f".format(frequency)

private fun adjacentLug(count: Int, current: Int, pattern: String, direction: Int): Int {
    val order = if (pattern == "Clockwise" || count % 2 != 0) (1..count).toList() else (1..count / 2).flatMap { listOf(it, it + count / 2) }
    val index = order.indexOf(current).coerceAtLeast(0)
    return order[(index + direction + order.size) % order.size]
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TunerChoices(options: List<String>, selected: String, choose: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            StudioButton(onClick = { choose(option) }, kind = if (option == selected) StudioButtonKind.Primary else StudioButtonKind.Secondary) {
                Text(option, color = if (option == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun <T> TunerDropdown(label: String, selected: String, options: List<T>, text: (T) -> String, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box {
            StudioButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                Text("$selected  ⌄", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.widthIn(min = 260.dp)) {
                options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { choose(option); expanded = false }) }
            }
        }
    }
}

private fun itemLabel(item: SupportingRecord, brands: Map<String, String>): String {
    val data = JSONObject(item.json)
    return listOf(brands[data.optString("brand_id")], data.optString("display_name")).filterNot { it.isNullOrBlank() }.joinToString(" ").ifBlank { "Gear item" }
}

private fun tunerStatus(listening: Boolean, state: PitchState): String = when {
    !listening -> "Microphone is off."
    state == PitchState.Weak -> "Signal is too quiet. Play or strike again."
    state == PitchState.Clipping -> "Signal is clipping. Move the device farther away."
    state == PitchState.Uncertain -> "Pitch is uncertain. Reduce background noise and try again."
    else -> "Listening..."
}

private fun nearestNote(frequency: Double, referenceA: Double): NoteTarget {
    if (frequency <= 0) return NoteTarget("--", 0.0)
    val midi = (69 + 12 * log2(frequency / referenceA)).roundToInt().coerceIn(0, 127)
    val names = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B")
    return NoteTarget("${names[midi % 12]}${midi / 12 - 1}", referenceA * 2.0.pow((midi - 69) / 12.0))
}

private fun noteFrequency(note: String, referenceA: Double): Double {
    val match = Regex("^([A-G])([#b]?)(-?\\d+)$").matchEntire(note) ?: return 0.0
    val base = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)[match.groupValues[1]] ?: 0
    val accidental = when (match.groupValues[2]) { "#" -> 1; "b" -> -1; else -> 0 }
    val midi = (match.groupValues[3].toInt() + 1) * 12 + base + accidental
    return referenceA * 2.0.pow((midi - 69) / 12.0)
}

private fun log2(value: Double): Double = ln(value) / ln(2.0)
