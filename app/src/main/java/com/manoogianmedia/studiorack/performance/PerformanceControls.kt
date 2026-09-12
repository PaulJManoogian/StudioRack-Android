package com.manoogianmedia.studiorack.performance

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.PI
import kotlin.math.sin

data class PerformanceSettings(
    val metronomeAutostart: Boolean = false,
    val metronomeMuted: Boolean = false,
    val metronomeMode: String = "tempo",
    val metronomeSound: String = "tone",
    val pedalEnabled: Boolean = false,
    val pedalMode: String = "hybrid",
    val pedalReverse: Boolean = false,
    val pedalScrollAmount: String = "half",
    val previousKey: String = "ArrowLeft",
    val nextKey: String = "ArrowRight",
    val metronomeKey: String = "ArrowUp",
    val muteKey: String = "ArrowDown",
    val showClock: Boolean = true,
    val showElapsed: Boolean = true,
    val showSetRemaining: Boolean = true,
    val attachmentPreferences: List<String> = listOf("drum_chart", "chart", "sheet_music", "lyrics", "tab"),
) {
    fun toJson(pendingSync: Boolean = false): JSONObject = JSONObject()
        .put("gig_metronome_autostart", if (metronomeAutostart) 1 else 0)
        .put("gig_metronome_muted", if (metronomeMuted) 1 else 0)
        .put("gig_metronome_mode", metronomeMode)
        .put("gig_metronome_sound", metronomeSound)
        .put("gig_pedal_enabled", if (pedalEnabled) 1 else 0)
        .put("gig_pedal_mode", pedalMode)
        .put("gig_pedal_reverse", if (pedalReverse) 1 else 0)
        .put("gig_pedal_scroll_amount", pedalScrollAmount)
        .put("gig_pedal_prev_key", previousKey)
        .put("gig_pedal_next_key", nextKey)
        .put("gig_pedal_metronome_key", metronomeKey)
        .put("gig_pedal_mute_key", muteKey)
        .put("gig_show_clock", if (showClock) 1 else 0)
        .put("gig_show_elapsed", if (showElapsed) 1 else 0)
        .put("gig_show_set_remaining", if (showSetRemaining) 1 else 0)
        .put("gig_attachment_preferences", JSONArray(attachmentPreferences))
        .put("_mobile_pending", if (pendingSync) 1 else 0)

    companion object {
        fun fromJson(value: String): PerformanceSettings {
            val json = runCatching { JSONObject(value) }.getOrDefault(JSONObject())
            return PerformanceSettings(
                metronomeAutostart = json.optInt("gig_metronome_autostart") == 1,
                metronomeMuted = json.optInt("gig_metronome_muted") == 1,
                metronomeMode = json.optString("gig_metronome_mode", "tempo"),
                metronomeSound = json.optString("gig_metronome_sound", "tone"),
                pedalEnabled = json.optInt("gig_pedal_enabled") == 1,
                pedalMode = json.optString("gig_pedal_mode", "hybrid"),
                pedalReverse = json.optInt("gig_pedal_reverse") == 1,
                pedalScrollAmount = json.optString("gig_pedal_scroll_amount", "half"),
                previousKey = json.optString("gig_pedal_prev_key", "ArrowLeft"),
                nextKey = json.optString("gig_pedal_next_key", "ArrowRight"),
                metronomeKey = json.optString("gig_pedal_metronome_key", "ArrowUp"),
                muteKey = json.optString("gig_pedal_mute_key", "ArrowDown"),
                showClock = json.optInt("gig_show_clock", 1) == 1,
                showElapsed = json.optInt("gig_show_elapsed", 1) == 1,
                showSetRemaining = json.optInt("gig_show_set_remaining", 1) == 1,
                attachmentPreferences = json.optJSONArray("gig_attachment_preferences")?.let { values ->
                    (0 until values.length()).mapNotNull { index -> values.optString(index).takeIf(String::isNotBlank) }
                }?.takeIf(List<String>::isNotEmpty) ?: listOf("drum_chart", "chart", "sheet_music", "lyrics", "tab"),
            )
        }
    }
}

enum class PedalAction { PREVIOUS, NEXT, METRONOME, MUTE }

fun mappedPedalAction(keyCode: Int, settings: PerformanceSettings): PedalAction? {
    val mappings = listOf(
        settings.previousKey to PedalAction.PREVIOUS,
        settings.nextKey to PedalAction.NEXT,
        settings.metronomeKey to PedalAction.METRONOME,
        settings.muteKey to PedalAction.MUTE,
    )
    val action = mappings.firstOrNull { keyCodeMatchesName(keyCode, it.first) }?.second ?: return null
    if (!settings.pedalReverse) return action
    return when (action) {
        PedalAction.PREVIOUS -> PedalAction.NEXT
        PedalAction.NEXT -> PedalAction.PREVIOUS
        else -> action
    }
}

internal fun keyCodeForName(name: String): Int = when (name) {
    "ArrowLeft" -> KeyEvent.KEYCODE_DPAD_LEFT
    "ArrowRight" -> KeyEvent.KEYCODE_DPAD_RIGHT
    "ArrowUp" -> KeyEvent.KEYCODE_DPAD_UP
    "ArrowDown" -> KeyEvent.KEYCODE_DPAD_DOWN
    "PageUp" -> KeyEvent.KEYCODE_PAGE_UP
    "PageDown" -> KeyEvent.KEYCODE_PAGE_DOWN
    "Space" -> KeyEvent.KEYCODE_SPACE
    "Enter" -> KeyEvent.KEYCODE_ENTER
    else -> KeyEvent.KEYCODE_UNKNOWN
}

private fun keyCodeMatchesName(keyCode: Int, name: String): Boolean =
    keyCodeForName(name) == keyCode || (name == "Enter" && keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)

fun isSupportedPedalKeyCode(keyCode: Int): Boolean = keyCode in setOf(
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_PAGE_UP,
    KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_SPACE,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
)

data class MetronomeState(
    val running: Boolean = false,
    val muted: Boolean = false,
    val pulse: Boolean = false,
    val downbeat: Boolean = false,
    val beat: Int = 0,
)

class NativeMetronome {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = mutableState.asStateFlow()
    private var job: Job? = null
    private var tempo = 120
    private var beatsPerMeasure = 4
    private var mode = "tempo"
    private var sound = "tone"
    private var player = StaticClickPlayer(sound)

    fun configure(tempoText: String?, timeSignature: String?, mode: String, sound: String) {
        tempo = tempoText?.filter(Char::isDigit)?.toIntOrNull()?.coerceIn(30, 260) ?: 120
        beatsPerMeasure = timeSignature?.substringBefore('/')?.trim()?.toIntOrNull()?.coerceIn(1, 12) ?: 4
        this.mode = mode
        if (this.sound != sound) {
            this.sound = sound
            player.close()
            player = StaticClickPlayer(sound)
        }
    }

    fun setMuted(muted: Boolean) {
        mutableState.value = mutableState.value.copy(muted = muted)
    }

    fun toggleMuted() = setMuted(!mutableState.value.muted)

    fun toggle() {
        if (mutableState.value.running) stop() else start()
    }

    fun start() {
        if (mutableState.value.running) return
        mutableState.value = mutableState.value.copy(running = true, pulse = false, beat = 0)
        job = scope.launch {
            try {
                var beatIndex = 0
                var nextBeat = System.nanoTime()
                while (isActive && mutableState.value.running) {
                    val remaining = nextBeat - System.nanoTime()
                    if (remaining > 1_500_000L) delay((remaining - 500_000L) / 1_000_000L)
                    while (isActive && System.nanoTime() < nextBeat) Thread.yield()
                    if (!isActive || !mutableState.value.running) break
                    val isDownbeat = mode == "downbeat" && beatIndex == 0
                    mutableState.value = mutableState.value.copy(pulse = true, downbeat = isDownbeat, beat = beatIndex + 1)
                    if (!mutableState.value.muted) player.play(isDownbeat)
                    scope.launch {
                        delay(70)
                        if (mutableState.value.running) mutableState.value = mutableState.value.copy(pulse = false)
                    }
                    beatIndex = (beatIndex + 1) % beatsPerMeasure
                    val interval = 60_000_000_000L / tempo
                    nextBeat += interval
                    if (System.nanoTime() - nextBeat > interval) nextBeat = System.nanoTime() + interval
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(running = false, pulse = false)
            }
        }
    }

    fun stop() {
        mutableState.value = mutableState.value.copy(running = false, pulse = false, downbeat = false, beat = 0)
        job?.cancel()
        job = null
        player.stop()
    }

    fun close() {
        stop()
        player.close()
        scope.cancel()
    }
}

private class StaticClickPlayer(sound: String) {
    private val normal = staticTrack(clickSamples(SAMPLE_RATE, sound, false))
    private val accent = staticTrack(clickSamples(SAMPLE_RATE, sound, true))

    fun play(downbeat: Boolean) {
        val track = if (downbeat) accent else normal
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            track.setPlaybackHeadPosition(0)
            track.play()
        }
    }

    fun stop() {
        listOf(normal, accent).forEach { track -> runCatching { track.pause(); track.setPlaybackHeadPosition(0) } }
    }

    fun close() {
        stop()
        normal.release()
        accent.release()
    }

    private fun staticTrack(samples: ShortArray): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(samples.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
        .also { it.write(samples, 0, samples.size) }

    companion object { private const val SAMPLE_RATE = 44_100 }
}

private fun clickSamples(sampleRate: Int, sound: String, downbeat: Boolean): ShortArray {
    val profile = when (sound) {
        "clave" -> SoundProfile(if (downbeat) 1850.0 else 1500.0, 0.045, Wave.SQUARE)
        "woodblock" -> SoundProfile(if (downbeat) 1250.0 else 900.0, 0.055, Wave.TRIANGLE)
        "cowbell" -> SoundProfile(if (downbeat) 1080.0 else 760.0, 0.070, Wave.COWBELL)
        else -> SoundProfile(if (downbeat) 1320.0 else 950.0, if (downbeat) 0.105 else 0.075, Wave.SINE)
    }
    val count = (sampleRate * profile.duration).toInt()
    return ShortArray(count) { index ->
        val phase = 2.0 * PI * profile.frequency * index / sampleRate
        val wave = when (profile.wave) {
            Wave.SINE -> sin(phase)
            Wave.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
            Wave.TRIANGLE -> 2.0 / PI * kotlin.math.asin(sin(phase))
            Wave.COWBELL -> (sin(phase) + 0.55 * sin(phase * 1.47)).coerceIn(-1.0, 1.0)
        }
        val decay = 1.0 - index.toDouble() / count
        (wave * decay * if (downbeat) 20_000 else 14_000).toInt().toShort()
    }
}

private data class SoundProfile(val frequency: Double, val duration: Double, val wave: Wave)
private enum class Wave { SINE, SQUARE, TRIANGLE, COWBELL }
