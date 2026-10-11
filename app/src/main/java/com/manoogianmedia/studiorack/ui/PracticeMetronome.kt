package com.manoogianmedia.studiorack.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

internal data class PracticeMetronomeConfig(
    val tempo: Int = 120,
    val meter: Int = 4,
    val subdivision: Int = 1,
    val accents: List<Int> = listOf(2, 1, 1, 1),
    val polyrhythm: Boolean = false,
    val pulseA: Int = 3,
    val pulseB: Int = 2,
    val trainer: Boolean = false,
    val trainerGoal: Int = 140,
    val trainerStep: Int = 2,
    val trainerBars: Int = 4,
    val silentBars: Boolean = false,
    val soundBars: Int = 2,
    val mutedBars: Int = 1,
)

internal data class PracticeMetronomeState(
    val running: Boolean = false,
    val effectiveTempo: Int = 120,
    val beat: Int = 0,
    val subBeat: Int = 0,
    val bar: Int = 0,
    val silent: Boolean = false,
    val pulseA: Int = -1,
    val pulseB: Int = -1,
)

internal fun practiceGrid(config: PracticeMetronomeConfig): Int {
    val base = config.meter.coerceIn(1, 12) * config.subdivision.coerceIn(1, 4)
    return if (!config.polyrhythm) base else lcmPractice(lcmPractice(base, config.pulseA.coerceIn(2, 12)), config.pulseB.coerceIn(2, 12))
}

private fun lcmPractice(a: Int, b: Int): Int {
    fun gcd(x: Int, y: Int): Int = if (y == 0) x else gcd(y, x % y)
    return a / gcd(a, b) * b
}

internal class PracticeMetronomeEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(PracticeMetronomeState())
    val state = mutableState.asStateFlow()
    @Volatile private var config = PracticeMetronomeConfig()
    private var job: Job? = null
    private var player: PracticeClickPlayer? = null

    fun configure(value: PracticeMetronomeConfig) { config = value }
    fun toggle() { if (mutableState.value.running) stop() else start() }

    fun start() {
        if (mutableState.value.running) return
        mutableState.value = PracticeMetronomeState(running = true, effectiveTempo = config.tempo)
        job = scope.launch {
            try {
                val clickPlayer = player ?: PracticeClickPlayer().also { player = it }
                var step = 0
                var bar = 0
                var next = System.nanoTime()
                while (isActive && mutableState.value.running) {
                    val current = config
                    val grid = practiceGrid(current)
                    val remaining = next - System.nanoTime()
                    if (remaining > 1_000_000L) delay(remaining / 1_000_000L)
                    if (!isActive || !mutableState.value.running) break
                    val beatEvery = grid / current.meter.coerceIn(1, 12)
                    val normalEvery = grid / (current.meter.coerceIn(1, 12) * current.subdivision.coerceIn(1, 4))
                    val beat = (step / beatEvery).coerceAtMost(current.meter - 1)
                    val subBeat = (step % beatEvery) / normalEvery
                    val silent = current.silentBars && bar % (current.soundBars + current.mutedBars).coerceAtLeast(1) >= current.soundBars
                    val polyA = if (current.polyrhythm && step % (grid / current.pulseA) == 0) step / (grid / current.pulseA) else -1
                    val polyB = if (current.polyrhythm && step % (grid / current.pulseB) == 0) step / (grid / current.pulseB) else -1
                    if (!silent && step % normalEvery == 0) {
                        val level = if (subBeat == 0) current.accents.getOrElse(beat) { 1 } else 1
                        if (level > 0) clickPlayer.play(if (level == 2) ClickVoice.ACCENT else ClickVoice.NORMAL)
                    }
                    if (!silent && current.polyrhythm) {
                        if (polyA >= 0) clickPlayer.play(ClickVoice.POLY_A)
                        if (polyB >= 0) clickPlayer.play(ClickVoice.POLY_B)
                    }
                    mutableState.value = PracticeMetronomeState(
                        running = true, effectiveTempo = current.tempo, beat = beat, subBeat = subBeat,
                        bar = bar, silent = silent, pulseA = polyA, pulseB = polyB,
                    )
                    val interval = 60_000_000_000L * current.meter.coerceIn(1, 12) / current.tempo.coerceIn(30, 300) / grid
                    next += interval
                    step += 1
                    if (step >= grid) {
                        step = 0
                        bar += 1
                        if (current.trainer && bar % current.trainerBars.coerceAtLeast(1) == 0 && current.tempo != current.trainerGoal) {
                            val direction = if (current.trainerGoal > current.tempo) 1 else -1
                            config = current.copy(tempo = (current.tempo + direction * current.trainerStep).let {
                                if (direction > 0) it.coerceAtMost(current.trainerGoal) else it.coerceAtLeast(current.trainerGoal)
                            })
                        }
                    }
                    if (System.nanoTime() - next > interval) next = System.nanoTime() + interval
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutableState.value = PracticeMetronomeState()
            }
        }
    }

    fun stop() { mutableState.value = PracticeMetronomeState(); job?.cancel(); job = null; player?.stop() }
    fun close() { stop(); player?.close(); player = null; scope.cancel() }
}

private class PracticeMetronomeUiState {
    val tempo = mutableIntStateOf(120)
    val meter = mutableIntStateOf(4)
    val subdivision = mutableIntStateOf(1)
    val accents = mutableStateListOf(2, 1, 1, 1)
    val poly = mutableStateOf(false)
    val pulseA = mutableIntStateOf(3)
    val pulseB = mutableIntStateOf(2)
    val trainer = mutableStateOf(false)
    val goal = mutableIntStateOf(140)
    val step = mutableIntStateOf(2)
    val every = mutableIntStateOf(4)
    val silent = mutableStateOf(false)
    val soundBars = mutableIntStateOf(2)
    val mutedBars = mutableIntStateOf(1)
    val mode = mutableStateOf("Beat")
    val lastTap = mutableLongStateOf(0L)
    val taps = mutableStateListOf<Long>()
}

private enum class ClickVoice { NORMAL, ACCENT, POLY_A, POLY_B }

private class PracticeClickPlayer {
    private val tracks = mapOf(
        ClickVoice.NORMAL to trackFor(920.0, .045, .55), ClickVoice.ACCENT to trackFor(1480.0, .065, .82),
        ClickVoice.POLY_A to trackFor(1850.0, .035, .42), ClickVoice.POLY_B to trackFor(520.0, .055, .48),
    )
    fun play(voice: ClickVoice) { tracks[voice]?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause(); it.setPlaybackHeadPosition(0); it.play() } } }
    fun stop() { tracks.values.forEach { runCatching { it.pause(); it.setPlaybackHeadPosition(0) } } }
    fun close() { stop(); tracks.values.forEach(AudioTrack::release) }
    private fun trackFor(frequency: Double, duration: Double, strength: Double): AudioTrack {
        val count = (44_100 * duration).toInt()
        val samples = ShortArray(count) { index ->
            val decay = 1.0 - index.toDouble() / count
            (sin(2.0 * PI * frequency * index / 44_100) * decay * 22_000 * strength).toInt().toShort()
        }
        return AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44_100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build().also { it.write(samples, 0, samples.size) }
    }
}

@Composable
internal fun PracticeMetronomePanel() {
    var fullScreen by remember { mutableStateOf(false) }
    val engine = remember { PracticeMetronomeEngine() }
    val session = remember { PracticeMetronomeUiState() }
    DisposableEffect(engine) { onDispose { engine.close() } }
    MetronomeSurface(engine, session, fullScreen = false, openFullScreen = { fullScreen = true })
    if (fullScreen) Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MetronomeSurface(engine, session, fullScreen = true, openFullScreen = { fullScreen = false }) }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetronomeSurface(engine: PracticeMetronomeEngine, session: PracticeMetronomeUiState, fullScreen: Boolean, openFullScreen: () -> Unit) {
    val state by engine.state.collectAsState()
    var tempo by session.tempo; var meter by session.meter; var subdivision by session.subdivision
    val accents = session.accents
    var poly by session.poly; var pulseA by session.pulseA; var pulseB by session.pulseB
    var trainer by session.trainer; var goal by session.goal; var step by session.step; var every by session.every
    var silent by session.silent; var soundBars by session.soundBars; var mutedBars by session.mutedBars
    var mode by session.mode; var lastTap by session.lastTap; val taps = session.taps
    fun configuration() = PracticeMetronomeConfig(tempo, meter, subdivision, accents.toList(), poly, pulseA, pulseB, trainer, goal, step, every, silent, soundBars, mutedBars)
    engine.configure(configuration())
    val contentModifier = if (fullScreen) {
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    } else {
        Modifier.fillMaxWidth().padding(2.dp)
    }
    Column(contentModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("PRACTICE TOOL", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black); Text("Metronome", fontSize = 24.sp, fontWeight = FontWeight.Black) }
            OutlinedButton(onClick = openFullScreen) { Text(if (fullScreen) "Close Full Screen" else "Full Screen") }
        }
        Surface(color = Color(0xFF080D16), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TempoButton("-5") { tempo = (tempo - 5).coerceAtLeast(30) }; TempoButton("-") { tempo = (tempo - 1).coerceAtLeast(30) }
                    OutlinedTextField(tempo.toString(), { tempo = it.toIntOrNull()?.coerceIn(30, 300) ?: tempo }, label = { Text("BPM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(105.dp), singleLine = true)
                    TempoButton("+") { tempo = (tempo + 1).coerceAtMost(300) }; TempoButton("+5") { tempo = (tempo + 5).coerceAtMost(300) }
                }
                Slider(tempo.toFloat(), { tempo = it.toInt() }, valueRange = 30f..300f)
                BeatDisplay(state, meter, poly, pulseA, pulseB)
                Text(if (state.silent) "SILENT BAR - HOLD THE PULSE" else if (state.running) "${state.effectiveTempo} BPM  •  BAR ${state.bar + 1}  •  BEAT ${state.beat + 1}" else "READY", color = if (state.silent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = { engine.configure(configuration()); engine.toggle() }, modifier = Modifier.weight(1f)) { Text(if (state.running) "Stop" else "Start", fontWeight = FontWeight.Black) }
            OutlinedButton(onClick = {
                val now = System.currentTimeMillis(); if (lastTap == 0L || now - lastTap > 2200) taps.clear(); taps.add(now); lastTap = now
                if (taps.size >= 3) tempo = (60_000.0 / taps.zipWithNext { a, b -> b - a }.average()).toInt().coerceIn(30, 300)
                if (taps.size > 8) taps.removeAt(0)
            }, modifier = Modifier.weight(1f)) { Text("Tap Tempo") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Beat", "Poly", "Practice").forEach { choice ->
            if (choice == mode) Button({ mode = choice }, Modifier.weight(1f)) { Text(choice) } else OutlinedButton({ mode = choice }, Modifier.weight(1f)) { Text(choice) }
        } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("QUICK STARTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterVertically))
            OutlinedButton({ tempo = 120; meter = 4; subdivision = 2; poly = false }) { Text("Straight 4") }
            OutlinedButton({ tempo = 88; meter = 6; subdivision = 3; poly = false; accents.clear(); repeat(6) { accents.add(if (it == 0 || it == 3) 2 else 1) } }) { Text("Triplet 6/8") }
            OutlinedButton({ tempo = 72; meter = 4; subdivision = 1; poly = true; pulseA = 3; pulseB = 2; mode = "Poly" }) { Text("3 : 2") }
        }
        when (mode) {
            "Beat" -> BeatControls(meter, { newMeter -> meter = newMeter; accents.clear(); repeat(newMeter) { accents.add(if (it == 0) 2 else 1) } }, subdivision, { subdivision = it }, accents)
            "Poly" -> PolyControls(poly, { poly = it }, pulseA, { pulseA = it }, pulseB, { pulseB = it }, state)
            else -> PracticeControls(trainer, { trainer = it }, goal, { goal = it }, step, { step = it }, every, { every = it }, silent, { silent = it }, soundBars, { soundBars = it }, mutedBars, { mutedBars = it }, tempo)
        }
    }
}

@Composable private fun TempoButton(label: String, action: () -> Unit) { OutlinedButton(action, contentPadding = ButtonDefaults.ContentPadding, modifier = Modifier.size(48.dp)) { Text(label, fontWeight = FontWeight.Black) } }

@Composable
private fun BeatDisplay(state: PracticeMetronomeState, meter: Int, poly: Boolean, pulseA: Int, pulseB: Int) {
    Canvas(Modifier.fillMaxWidth().height(86.dp)) {
        val center = size.width / 2; val bottom = size.height - 5; val side = if (state.running && state.beat % 2 == 0) -1 else 1
        drawLine(Color(0xFF50D5F9), Offset(center, bottom), Offset(center + side * size.width * .22f, 12f), 5f, StrokeCap.Round)
        drawCircle(Color(0xFFFFA000), 13f, Offset(center + side * size.width * .22f, 12f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(meter) { index -> Box(Modifier.size(if (index == state.beat && state.running) 18.dp else 13.dp).background(if (index == state.beat && state.running) MaterialTheme.colorScheme.primary else Color(0xFF344057), CircleShape)) } }
    if (poly) Text("$pulseA : $pulseB", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BeatControls(meter: Int, setMeter: (Int) -> Unit, subdivision: Int, setSubdivision: (Int) -> Unit, accents: MutableList<Int>) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f), shape = RoundedCornerShape(8.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Stepper("Meter", meter, { setMeter((meter - 1).coerceAtLeast(1)) }, { setMeter((meter + 1).coerceAtMost(12)) })
        Text("SUBDIVISION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf(1 to "Quarter", 2 to "Eighth", 3 to "Triplet", 4 to "Sixteenth").forEach { (value, label) -> ChoiceButton(label, value == subdivision) { setSubdivision(value) } } }
        Text("BEAT NOTATION AND ACCENTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { accents.forEachIndexed { index, level ->
            Surface(Modifier.width(58.dp).clickable { accents[index] = (level + 2) % 3 }, color = if (level == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(7.dp)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (subdivision == 3) "3" else "♪", fontSize = 25.sp, color = if (level == 2) Color.Black else MaterialTheme.colorScheme.onSurface); Text("${index + 1} ${if (level == 2) "ACC" else if (level == 0) "MUTE" else ""}", fontSize = 8.sp, color = if (level == 2) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } }
    } }
}

@Composable private fun PolyControls(enabled: Boolean, setEnabled: (Boolean) -> Unit, a: Int, setA: (Int) -> Unit, b: Int, setB: (Int) -> Unit, state: PracticeMetronomeState) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f), shape = RoundedCornerShape(8.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleLine("Polyrhythm", "Two independent pulses share one cycle", enabled, setEnabled)
        Stepper("Pulse A", a, { setA((a - 1).coerceAtLeast(2)) }, { setA((a + 1).coerceAtMost(12)) }); PulseRow(a, state.pulseA, MaterialTheme.colorScheme.secondary)
        Stepper("Pulse B", b, { setB((b - 1).coerceAtLeast(2)) }, { setB((b + 1).coerceAtMost(12)) }); PulseRow(b, state.pulseB, MaterialTheme.colorScheme.primary)
    } }
}

@Composable private fun PracticeControls(trainer: Boolean, setTrainer: (Boolean) -> Unit, goal: Int, setGoal: (Int) -> Unit, step: Int, setStep: (Int) -> Unit, every: Int, setEvery: (Int) -> Unit, silent: Boolean, setSilent: (Boolean) -> Unit, soundBars: Int, setSound: (Int) -> Unit, mutedBars: Int, setMuted: (Int) -> Unit, tempo: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f), shape = RoundedCornerShape(8.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ToggleLine("Tempo Trainer", "Move toward a goal automatically", trainer, setTrainer)
        Stepper("Goal BPM", goal, { setGoal((goal - 1).coerceAtLeast(30)) }, { setGoal((goal + 1).coerceAtMost(300)) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Stepper("Change", step, { setStep((step - 1).coerceAtLeast(1)) }, { setStep((step + 1).coerceAtMost(20)) }) }; Box(Modifier.weight(1f)) { Stepper("Every bars", every, { setEvery((every - 1).coerceAtLeast(1)) }, { setEvery((every + 1).coerceAtMost(32)) }) } }
        LinearProgressIndicator(progress = { if (goal == tempo) 1f else (tempo.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        ToggleLine("Silent Bars", "Keep the display moving while the click drops out", silent, setSilent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Stepper("Sound bars", soundBars, { setSound((soundBars - 1).coerceAtLeast(1)) }, { setSound((soundBars + 1).coerceAtMost(16)) }) }; Box(Modifier.weight(1f)) { Stepper("Silent bars", mutedBars, { setMuted((mutedBars - 1).coerceAtLeast(1)) }, { setMuted((mutedBars + 1).coerceAtMost(16)) }) } }
    } }
}

@Composable private fun ToggleLine(title: String, detail: String, checked: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; Switch(checked, change) } }
@Composable private fun Stepper(label: String, value: Int, minus: () -> Unit, plus: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold); OutlinedButton(minus, contentPadding = ButtonDefaults.ContentPadding) { Text("-") }; Text(value.toString(), Modifier.width(42.dp), fontWeight = FontWeight.Black, fontSize = 18.sp); OutlinedButton(plus, contentPadding = ButtonDefaults.ContentPadding) { Text("+") } } }
@Composable private fun PulseRow(count: Int, active: Int, color: Color) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(count) { index -> Box(Modifier.weight(1f).height(11.dp).background(if (index == active) color else Color(0xFF344057), RoundedCornerShape(99.dp))) } } }
@Composable private fun ChoiceButton(label: String, selected: Boolean, action: () -> Unit) { if (selected) Button(action) { Text(label) } else OutlinedButton(action) { Text(label) } }
