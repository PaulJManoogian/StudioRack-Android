package com.manoogianmedia.studiorack.performance

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.Closeable
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Lightweight onboard percussion voices for chart audition playback. */
class DrumAuditionPlayer : Closeable {
    private val tracks = mutableMapOf<Int, AudioTrack>()
    private var preferredDevice: AudioDeviceInfo? = null

    fun setOutputDevice(device: AudioDeviceInfo?) {
        preferredDevice = device
        tracks.values.forEach { track -> runCatching { track.preferredDevice = device } }
    }

    fun play(note: Int, velocity: Int) {
        val track = tracks.getOrPut(note) { createTrack(note) }
        runCatching {
            track.setVolume((velocity.coerceIn(1, 127) / 127f * 0.82f).coerceIn(0f, 1f))
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            track.setPlaybackHeadPosition(0)
            track.play()
        }
    }

    fun stop() {
        tracks.values.forEach { track ->
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
                track.setPlaybackHeadPosition(0)
            }
        }
    }

    override fun close() {
        stop()
        tracks.values.forEach { track -> runCatching { track.release() } }
        tracks.clear()
    }

    private fun createTrack(note: Int): AudioTrack {
        val samples = percussionSamples(note)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            .also { track ->
                track.write(samples, 0, samples.size)
                preferredDevice?.let { track.preferredDevice = it }
            }
    }
}

internal const val DRUM_AUDITION_SAMPLE_RATE = 44_100
private const val SAMPLE_RATE = DRUM_AUDITION_SAMPLE_RATE

internal fun percussionSamples(note: Int): ShortArray = when (note) {
    35, 36 -> kickSamples(if (note == 35) 92.0 else 108.0)
    37 -> clickSamples(1_450.0)
    38, 39, 40 -> snareSamples(if (note == 40) 225.0 else 185.0)
    41, 43, 45, 47, 48, 50 -> tomSamples(note)
    42, 44 -> metallicNoiseSamples(0.085, 0.62)
    46 -> metallicNoiseSamples(0.36, 0.5)
    49, 52, 55, 57 -> cymbalSamples(0.9, note)
    51, 53, 59 -> cymbalSamples(0.58, note)
    else -> clickSamples(700.0 + note.coerceIn(27, 87) * 9.0)
}

private fun kickSamples(startFrequency: Double): ShortArray = render(0.34) { index, time, progress, noise ->
    val frequency = 46.0 + (startFrequency - 46.0) * exp(-progress * 8.0)
    val body = sin(2.0 * PI * frequency * time) * exp(-progress * 7.2)
    val beater = noise * exp(-progress * 65.0) * 0.3
    (body + beater) * attack(index, 44) * 0.9
}

private fun snareSamples(toneFrequency: Double): ShortArray = render(0.25) { index, time, progress, noise ->
    val wire = noise * exp(-progress * 10.5) * 0.72
    val shell = sin(2.0 * PI * toneFrequency * time) * exp(-progress * 14.0) * 0.38
    (wire + shell) * attack(index, 24)
}

private fun tomSamples(note: Int): ShortArray {
    val frequency = when (note) {
        41 -> 82.0
        43 -> 98.0
        45 -> 122.0
        47 -> 147.0
        48 -> 175.0
        else -> 208.0
    }
    return render(0.42) { index, time, progress, noise ->
        val bend = frequency * (1.0 + 0.24 * exp(-progress * 7.0))
        val body = sin(2.0 * PI * bend * time) + sin(2.0 * PI * bend * 1.98 * time) * 0.18
        (body * exp(-progress * 7.0) + noise * exp(-progress * 28.0) * 0.08) * attack(index, 38) * 0.72
    }
}

private fun metallicNoiseSamples(duration: Double, strength: Double): ShortArray {
    var previous = 0.0
    return render(duration) { index, _, progress, noise ->
        val highPassed = noise - previous * 0.82
        previous = noise
        highPassed * exp(-progress * 8.5) * attack(index, 8) * strength
    }
}

private fun cymbalSamples(duration: Double, note: Int): ShortArray = render(duration) { index, time, progress, noise ->
    val base = 285.0 + (note % 7) * 23.0
    val metal = sin(2.0 * PI * base * 5.31 * time) + sin(2.0 * PI * base * 7.17 * time) * 0.7 + sin(2.0 * PI * base * 9.43 * time) * 0.45
    (noise * 0.48 + metal * 0.22) * exp(-progress * 5.4) * attack(index, 10) * 0.72
}

private fun clickSamples(frequency: Double): ShortArray = render(0.12) { index, time, progress, noise ->
    (sin(2.0 * PI * frequency * time) * 0.72 + noise * 0.18) * exp(-progress * 13.0) * attack(index, 10)
}

private inline fun render(durationSeconds: Double, sample: (index: Int, time: Double, progress: Double, noise: Double) -> Double): ShortArray {
    val count = (SAMPLE_RATE * durationSeconds).toInt().coerceAtLeast(1)
    var random = 0x13579BDF
    return ShortArray(count) { index ->
        random = random xor (random shl 13)
        random = random xor (random ushr 17)
        random = random xor (random shl 5)
        val noise = random.toDouble() / Int.MAX_VALUE.toDouble()
        val value = sample(index, index.toDouble() / SAMPLE_RATE, index.toDouble() / count, noise).coerceIn(-1.0, 1.0)
        (value * Short.MAX_VALUE * 0.72).toInt().toShort()
    }
}

private fun attack(index: Int, samples: Int): Double = (index.toDouble() / samples.coerceAtLeast(1)).coerceIn(0.0, 1.0)
