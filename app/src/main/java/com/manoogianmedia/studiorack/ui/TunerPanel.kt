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
private data class DrumStandard(val id: String, val name: String, val note: String, val wholeHz: Double, val batterHz: Double = wholeHz, val resonantHz: Double = wholeHz)
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
    DrumStandard("snare_14", "14 in Snare - balanced", "A3", 220.0, 220.0, 329.63),
    DrumStandard("tom_8", "8 in Tom", "G3", 196.0, 196.0, 246.94),
    DrumStandard("tom_10", "10 in Tom", "D3", 146.83, 146.83, 185.0),
    DrumStandard("tom_12", "12 in Tom", "A2", 110.0, 110.0, 138.59),
    DrumStandard("tom_13", "13 in Tom", "G2", 98.0, 98.0, 123.47),
    DrumStandard("floor_tom_14", "14 in Floor Tom", "E2", 82.41, 82.41, 103.83),
    DrumStandard("floor_tom_16", "16 in Floor Tom", "C2", 65.41, 65.41, 82.41),
    DrumStandard("floor_tom_18", "18 in Floor Tom", "A1", 55.0, 55.0, 69.30),
    DrumStandard("kick_22", "22 in Bass Drum", "G1", 49.0, 49.0, 61.74),
    DrumStandard("timpani_32", "32 in Timpani - D2 to A2", "D2", 73.42),
    DrumStandard("timpani_29", "29 in Timpani - F2 to C3", "F2", 87.31),
    DrumStandard("timpani_26", "26 in Timpani - Bb2 to F3", "Bb2", 116.54),
    DrumStandard("timpani_23", "23 in Timpani - D3 to A3", "D3", 146.83),
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
    var concertPitch by remember { mutableStateOf(440f) }
    var listening by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(PitchReading()) }
    var selectedItem by remember { mutableStateOf<SupportingRecord?>(null) }
    var notes by remember { mutableStateOf("") }

    fun startListening() {
        listening = true
        engine.start(scope) { value -> scope.launch { reading = value } }
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
    val fixedTarget = if (mode == "Tuned Instruments") {
        instrumentPreset.notes.getOrNull(stringIndex)?.let { NoteTarget(it, noteFrequency(it, concertPitch.toDouble())) }
            ?: nearest
    } else {
        val hz = when (drumZone) { "Batter head" -> drumStandard.batterHz; "Resonant head" -> drumStandard.resonantHz; else -> drumStandard.wholeHz }
        NoteTarget(if (drumZone.startsWith("Lug")) "Lug $lug" else drumStandard.note, hz)
    }
    val cents = if (reading.frequencyHz > 0 && fixedTarget.frequency > 0) 1200.0 * log2(reading.frequencyHz / fixedTarget.frequency) else 0.0
    val existing = selectedItem?.let { item -> tunings.firstOrNull { it.entityId == item.entityId } }?.let { JSONObject(it.json) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TUNERS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Tune on this device", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Pitch analysis stays on the device. Choose a standard, tune, then save the result with an existing gear item.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TunerChoices(listOf("Tuned Instruments", "Percussion"), mode) {
            engine.stop(); listening = false; reading = PitchReading(); selectedItem = null; mode = it
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (mode == "Tuned Instruments") {
                    TunerDropdown("Standard tuning", instrumentPreset.name, instrumentTunings, text = { it.name }, choose = { instrumentPreset = it; stringIndex = 0 })
                    if (instrumentPreset.notes.isNotEmpty()) TunerDropdown("String / target", instrumentPreset.notes[stringIndex], instrumentPreset.notes, text = { it }, choose = { stringIndex = instrumentPreset.notes.indexOf(it) })
                    Text("Concert pitch: A = ${concertPitch.roundToInt()} Hz", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Slider(value = concertPitch, onValueChange = { concertPitch = it }, valueRange = 430f..450f, steps = 19)
                } else {
                    TunerDropdown("Drum standard", drumStandard.name, drumStandards, text = { it.name }, choose = { drumStandard = it })
                    TunerDropdown("Measurement", drumZone, listOf("Whole drum", "Batter head", "Resonant head", "Lug"), text = { it }, choose = { drumZone = it })
                    if (drumZone == "Lug") {
                        Text("Lug $lug", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Slider(value = lug.toFloat(), onValueChange = { lug = it.roundToInt() }, valueRange = 1f..16f, steps = 14)
                    }
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

        TunerDisplay(fixedTarget, nearest, reading, cents)

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
                        val summary = "${if (mode == "Percussion") drumStandard.name else instrumentPreset.name}; $drumZone; target ${fixedTarget.label} (${"%.2f".format(fixedTarget.frequency)} Hz); measured $measured; ${"%+.1f".format(cents)} cents${notes.trim().takeIf(String::isNotBlank)?.let { "; $it" } ?: ""}"
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
private fun TunerDisplay(target: NoteTarget, detected: NoteTarget, reading: PitchReading, cents: Double) {
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
            val tuned = abs(cents) <= 3 && reading.state == PitchState.Clear
            Text(if (reading.frequencyHz <= 0) "Waiting for a clear note" else if (tuned) "In tune" else "${abs(cents).let { "%.1f".format(it) }} cents ${if (cents < 0) "flat" else "sharp"}", color = if (tuned) Color(0xFF55E69B) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confidence", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                LinearProgressIndicator(progress = { reading.confidence }, modifier = Modifier.weight(1f))
            }
        }
    }
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
