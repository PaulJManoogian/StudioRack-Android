package com.manoogianmedia.studiorack.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.manoogianmedia.studiorack.performance.NativeMetronome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

internal data class TempoReading(
    val bpm: Double = 0.0,
    val confidence: Float = 0f,
    val inputLevel: Float = 0f,
    val status: String = "Waiting for a steady pulse",
)

internal class AndroidTempoEngine {
    private var recorder: AudioRecord? = null
    private var job: Job? = null

    @Suppress("MissingPermission")
    fun start(scope: CoroutineScope, onReading: (TempoReading) -> Unit) {
        stop()
        val sampleRate = 48_000
        val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) { onReading(TempoReading(status = "The microphone could not be started")); return }
        val source = if (android.os.Build.VERSION.SDK_INT >= 24) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.DEFAULT
        val audioRecord = runCatching {
            AudioRecord(source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minimum * 2, 8192))
        }.getOrElse {
            AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minimum * 2, 8192))
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release(); onReading(TempoReading(status = "The microphone could not be started")); return
        }
        recorder = audioRecord
        audioRecord.startRecording()
        job = scope.launch(Dispatchers.Default) {
            val pcm = ShortArray(1024)
            val onsets = ArrayDeque<Long>()
            var noiseFloor = .012
            var previousRms = 0.0
            var lastOnset = 0L
            var lastStatusAt = 0L
            var smoothedBpm = 0.0
            while (isActive) {
                val count = audioRecord.read(pcm, 0, pcm.size)
                if (count <= 0) continue
                var energy = 0.0
                var peak = 0.0
                for (index in 0 until count) {
                    val sample = abs(pcm[index].toDouble() / 32768.0)
                    energy += sample * sample
                    peak = max(peak, sample)
                }
                val rms = sqrt(energy / count)
                noiseFloor = noiseFloor * .985 + minOf(rms, noiseFloor * 1.8) * .015
                val threshold = max(.018, noiseFloor * 1.7)
                val now = System.nanoTime() / 1_000_000
                if (rms > threshold && rms - previousRms > max(.005, noiseFloor * .32) && now - lastOnset > 175) {
                    lastOnset = now
                    onsets.addLast(now)
                    while (onsets.isNotEmpty() && now - onsets.first() > 12_000) onsets.removeFirst()
                    while (onsets.size > 24) onsets.removeFirst()
                    val result = calculateTempo(onsets.toList())
                    if (result.bpm > 0) {
                        smoothedBpm = if (smoothedBpm > 0) smoothedBpm * .68 + result.bpm * .32 else result.bpm
                        onReading(result.copy(bpm = smoothedBpm, inputLevel = rms.toFloat()))
                    }
                } else if (onsets.size < 3 && now - lastStatusAt >= 180) {
                    lastStatusAt = now
                    onReading(TempoReading(inputLevel = rms.toFloat(), status = if (peak > .98) "Input is clipping" else if (rms < threshold) "Waiting for a clear beat" else "Finding the pulse"))
                }
                previousRms = rms
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
    }
}

internal fun calculateTempo(times: List<Long>): TempoReading {
    if (times.size < 3) return TempoReading(status = "Finding the pulse")
    val intervals = times.zipWithNext { first, second -> second - first }.filter { it in 175..2000 }
    if (intervals.size < 2) return TempoReading(status = "Finding the pulse")
    val center = intervals.sorted()[intervals.size / 2].toDouble()
    val accepted = intervals.filter { abs(it - center) <= max(55.0, center * .24) }
    if (accepted.size < 2) return TempoReading(status = "Pulse is unsteady")
    val average = accepted.average()
    var bpm = 60000.0 / average
    while (bpm > 220) bpm /= 2
    while (bpm < 40) bpm *= 2
    val spread = accepted.map { abs(it - average) }.sorted()[accepted.size / 2] / average
    val confidence = (((1 - spread * 4) * .72) + (accepted.size / 8.0).coerceIn(0.0, 1.0) * .28).coerceIn(0.0, 1.0).toFloat()
    return TempoReading(bpm.coerceIn(30.0, 300.0), confidence, status = if (confidence >= .72f) "Strong pulse" else "Pulse settling")
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TempoPanel(compact: Boolean = false, initialTargetBpm: Int = 120) {
    val context = LocalContext.current
    val narrowPractice = !compact && LocalConfiguration.current.screenWidthDp < 600
    val scope = rememberCoroutineScope()
    val engine = remember { AndroidTempoEngine() }
    val click = remember { NativeMetronome() }
    val clickState by click.state.collectAsState()
    var targetText by remember(initialTargetBpm) { mutableStateOf(initialTargetBpm.toString()) }
    val target = targetText.toIntOrNull()?.coerceIn(30, 300) ?: initialTargetBpm
    var reading by remember { mutableStateOf(TempoReading()) }
    var listening by remember { mutableStateOf(false) }
    var factor by remember { mutableStateOf(1.0) }
    var taps by remember { mutableStateOf<List<Long>>(emptyList()) }
    var history by remember { mutableStateOf<List<Double>>(emptyList()) }

    fun accept(value: TempoReading) {
        val corrected = value.copy(bpm = (value.bpm * factor).coerceIn(0.0, 300.0))
        reading = corrected
        if (corrected.bpm > 0) history = (history + corrected.bpm).takeLast(if (compact) 45 else 90)
    }
    fun startListening() {
        listening = true
        engine.start(scope) { value -> scope.launch { accept(value) } }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else reading = TempoReading(status = "Microphone permission was not granted")
    }
    DisposableEffect(Unit) { onDispose { engine.stop(); click.close() } }

    val displayedBpm = reading.bpm
    val delta = if (displayedBpm > 0) displayedBpm - target else 0.0
    val stateColor = when {
        displayedBpm <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        abs(delta) <= 1 -> Color(0xFF55E69B)
        abs(delta) <= 3 -> Color(0xFFFFC247)
        else -> Color(0xFFFF6262)
    }
    val direction = when {
        displayedBpm <= 0 -> reading.status
        abs(delta) <= 1 -> "Locked to target"
        delta > 0 -> "Rushing"
        else -> "Dragging"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .55f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(if (narrowPractice) 7.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(if (narrowPractice) 4.dp else if (compact) 10.dp else 7.dp)) {
            if (!compact) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .62f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = if (narrowPractice) 7.dp else 10.dp, vertical = if (narrowPractice) 3.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.width(if (narrowPractice) 62.dp else 78.dp)) {
                            Text("TARGET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text("$target BPM", color = MaterialTheme.colorScheme.primary, fontSize = if (narrowPractice) 13.sp else 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        Slider(
                            value = target.toFloat(),
                            onValueChange = { targetText = it.toInt().coerceIn(30, 300).toString() },
                            valueRange = 30f..300f,
                            steps = 269,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { value -> targetText = value.filter(Char::isDigit).take(3) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = if (narrowPractice) 15.sp else 18.sp,
                                lineHeight = if (narrowPractice) 18.sp else 22.sp,
                            ),
                            singleLine = true,
                            supportingText = null,
                            modifier = Modifier.width(if (narrowPractice) 64.dp else 74.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(if (compact) .34f else if (narrowPractice) .31f else .42f)) {
                    Text(if (displayedBpm > 0) "%.1f".format(displayedBpm) else "--", color = stateColor, fontSize = if (compact) 36.sp else if (narrowPractice) 38.sp else 54.sp, lineHeight = if (compact) 40.sp else if (narrowPractice) 40.sp else 56.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (narrowPractice) "BPM  •  ${shortTempoStatus(direction)}" else "BPM  •  $direction", color = stateColor, fontSize = if (compact || narrowPractice) 11.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = if (narrowPractice) 2 else 1)
                    if (displayedBpm > 0) Text("${if (delta > 0) "+" else ""}${"%.1f".format(delta)} from $target", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                TempoHistoryGraph(history, target, Modifier.weight(1f).height(if (compact) 88.dp else if (narrowPractice) 78.dp else 112.dp))
            }
            TempoDriftMeter(delta, stateColor)
            LinearProgressIndicator(progress = { reading.confidence }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.outline.copy(alpha = .22f))
            TempoActions(
                compactRow = compact || narrowPractice,
                listening = listening,
                clickRunning = clickState.running,
                showClick = !compact,
                onListen = {
                    if (listening) { engine.stop(); listening = false; reading = reading.copy(status = "Listening stopped") }
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                },
                onTap = {
                    val now = System.currentTimeMillis()
                    val current = if (taps.isNotEmpty() && now - taps.last() > 2500) emptyList() else taps
                    taps = (current + now).takeLast(12)
                    val tapped = calculateTempo(taps)
                    if (tapped.bpm > 0) accept(tapped.copy(confidence = max(.45f, tapped.confidence), status = "Tap tempo is active"))
                    else reading = reading.copy(status = "Tap ${max(0, 4 - taps.size)} more times")
                },
                onClick = {
                    click.configure(target.toString(), "4/4", "tempo", "tone")
                    click.toggle()
                },
                onHalf = { factor *= .5; reading = reading.copy(bpm = (reading.bpm * .5).coerceIn(0.0, 300.0)); history = history.map { it * .5 } },
                onDouble = { factor *= 2; reading = reading.copy(bpm = (reading.bpm * 2).coerceIn(0.0, 300.0)); history = history.map { it * 2 } },
            )
            Text(reading.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun shortTempoStatus(value: String): String = when {
    value.contains("wait", true) -> "Waiting"
    value.contains("find", true) -> "Finding pulse"
    value.contains("steady", true) -> "Steady"
    else -> value
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TempoActions(
    compactRow: Boolean,
    listening: Boolean,
    clickRunning: Boolean,
    showClick: Boolean,
    onListen: () -> Unit,
    onTap: () -> Unit,
    onClick: () -> Unit,
    onHalf: () -> Unit,
    onDouble: () -> Unit,
) {
    @Composable
    fun Action(label: String, action: () -> Unit, primary: Boolean = false, modifier: Modifier = Modifier) {
        StudioButton(onClick = action, kind = if (primary) StudioButtonKind.Primary else StudioButtonKind.Secondary, modifier = modifier) {
            Text(label, color = if (primary) Color(0xFF07090F) else MaterialTheme.colorScheme.onSurface, fontSize = if (compactRow) 10.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
    if (compactRow) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Action(if (listening) "Stop" else "Listen", onListen, true, Modifier.weight(1.18f))
            Action("Tap", onTap, modifier = Modifier.weight(1f))
            if (showClick) Action(if (clickRunning) "Stop" else "Click", onClick, modifier = Modifier.weight(1f))
            Action("÷2", onHalf, modifier = Modifier.weight(1f))
            Action("×2", onDouble, modifier = Modifier.weight(1f))
        }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Action(if (listening) "Stop" else "Listen", onListen, true)
            Action("Tap", onTap)
            if (showClick) Action(if (clickRunning) "Stop Click" else "Click", onClick)
            Action("÷2", onHalf)
            Action("×2", onDouble)
        }
    }
}

@Composable
private fun TempoDriftMeter(delta: Double, stateColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(Modifier.fillMaxWidth().height(32.dp)) {
            val y = size.height / 2
            drawLine(Color(0xFFD94E53), Offset(8f, y), Offset(size.width * .25f, y), 10f, StrokeCap.Round)
            drawLine(Color(0xFFFFC247), Offset(size.width * .25f, y), Offset(size.width * .45f, y), 10f)
            drawLine(Color(0xFF55E69B), Offset(size.width * .45f, y), Offset(size.width * .55f, y), 10f)
            drawLine(Color(0xFFFFC247), Offset(size.width * .55f, y), Offset(size.width * .75f, y), 10f)
            drawLine(Color(0xFFD94E53), Offset(size.width * .75f, y), Offset(size.width - 8f, y), 10f, StrokeCap.Round)
            val x = size.width * (.5f + (delta / 12.0).coerceIn(-1.0, 1.0).toFloat() * .47f)
            drawLine(stateColor, Offset(x, 3f), Offset(x, size.height - 3f), 7f, StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DRAGGING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("RUSHING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TempoHistoryGraph(values: List<Double>, target: Int, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = .2f)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            for (row in 1..3) drawLine(gridColor, Offset(0f, size.height * row / 4), Offset(size.width, size.height * row / 4), 1f)
            val range = max(8.0, values.maxOfOrNull { abs(it - target) } ?: 0.0)
            fun y(value: Double) = (size.height - 8f - (((value - (target - range * 1.18)) / (range * 2.36)).toFloat() * (size.height - 16f))).coerceIn(4f, size.height - 4f)
            drawLine(Color(0xFFFF9D1E), Offset(0f, y(target.toDouble())), Offset(size.width, y(target.toDouble())), 2f)
            if (values.size > 1) {
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = index.toFloat() / (values.size - 1) * size.width
                    if (index == 0) path.moveTo(x, y(value)) else path.lineTo(x, y(value))
                }
                drawPath(path, Color(0xFF42D9FF), style = Stroke(4f, cap = StrokeCap.Round))
            }
        }
    }
}
