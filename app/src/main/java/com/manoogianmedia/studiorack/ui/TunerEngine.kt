package com.manoogianmedia.studiorack.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal data class PitchReading(
    val frequencyHz: Double = 0.0,
    val confidence: Float = 0f,
    val inputLevel: Float = 0f,
    val state: PitchState = PitchState.Waiting,
)

internal enum class PitchState { Waiting, Clear, Weak, Clipping, Uncertain }

internal object PitchAnalyzer {
    fun analyze(samples: FloatArray, sampleRate: Int, minHz: Double = 35.0, maxHz: Double = 1400.0, noiseGate: Float = .008f, minimumConfidence: Double = .58): PitchReading {
        if (samples.size < 256 || sampleRate <= 0) return PitchReading(state = PitchState.Uncertain)
        val mean = samples.average().toFloat()
        val centered = FloatArray(samples.size) { samples[it] - mean }
        val peak = centered.maxOf { abs(it) }
        val rms = sqrt(centered.fold(0.0) { total, value -> total + value * value } / centered.size).toFloat()
        if (peak >= .985f) return PitchReading(confidence = .25f, inputLevel = rms, state = PitchState.Clipping)
        if (rms < noiseGate) return PitchReading(confidence = 0f, inputLevel = rms, state = PitchState.Weak)

        val minLag = max(2, (sampleRate / maxHz).toInt())
        val maxLag = minOf(centered.size / 2, (sampleRate / minHz).toInt())
        val scores = DoubleArray(maxLag + 1) { -1.0 }
        for (lag in minLag..maxLag) {
            var cross = 0.0
            var energyA = 0.0
            var energyB = 0.0
            var i = 0
            while (i < centered.size - lag) {
                val a = centered[i].toDouble()
                val b = centered[i + lag].toDouble()
                cross += a * b
                energyA += a * a
                energyB += b * b
                i += 2
            }
            val score = cross / sqrt((energyA * energyB).coerceAtLeast(1e-12))
            scores[lag] = score
        }
        val strongest = (minLag..maxLag).maxOf { scores[it] }
        val peakThreshold = max(.72, strongest * .92)
        val bestLag = (minLag + 1 until maxLag).firstOrNull { lag ->
            scores[lag] >= peakThreshold && scores[lag] >= scores[lag - 1] && scores[lag] >= scores[lag + 1]
        } ?: (minLag..maxLag).maxByOrNull { scores[it] } ?: 0
        val bestScore = if (bestLag > 0) scores[bestLag] else -1.0
        if (bestLag == 0 || bestScore < minimumConfidence) return PitchReading(confidence = bestScore.coerceIn(0.0, 1.0).toFloat(), inputLevel = rms, state = PitchState.Uncertain)

        fun correlation(lag: Int): Double {
            if (lag !in minLag..maxLag) return bestScore
            var sum = 0.0
            var i = 0
            while (i < centered.size - lag) {
                sum += centered[i] * centered[i + lag]
                i += 2
            }
            return sum
        }
        val left = correlation(bestLag - 1)
        val middle = correlation(bestLag)
        val right = correlation(bestLag + 1)
        val divisor = 2.0 * (2.0 * middle - left - right)
        val adjustment = if (abs(divisor) > 1e-9) (right - left) / divisor else 0.0
        val frequency = sampleRate / (bestLag + adjustment.coerceIn(-.5, .5))
        return PitchReading(frequency, bestScore.coerceIn(0.0, 1.0).toFloat(), rms, PitchState.Clear)
    }
}

internal class AndroidTunerEngine {
    private var recorder: AudioRecord? = null
    private var job: Job? = null

    @Suppress("MissingPermission")
    fun start(scope: CoroutineScope, noiseGate: Float = .008f, minimumConfidence: Double = .58, onReading: (PitchReading) -> Unit) {
        stop()
        val sampleRate = 48_000
        val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) {
            onReading(PitchReading(state = PitchState.Uncertain))
            return
        }
        val source = if (android.os.Build.VERSION.SDK_INT >= 24) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.DEFAULT
        val audioRecord = runCatching {
            AudioRecord(source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minimum * 2, 8192))
        }.getOrElse {
            AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minimum * 2, 8192))
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            onReading(PitchReading(state = PitchState.Uncertain))
            return
        }
        recorder = audioRecord
        audioRecord.startRecording()
        job = scope.launch(Dispatchers.Default) {
            val pcm = ShortArray(4096)
            while (isActive) {
                val count = audioRecord.read(pcm, 0, pcm.size)
                if (count > 0) {
                    val samples = FloatArray(count) { pcm[it] / 32768f }
                    onReading(PitchAnalyzer.analyze(samples, sampleRate, noiseGate = noiseGate, minimumConfidence = minimumConfidence))
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
    }
}

internal object TunerReferenceTone {
    private var track: AudioTrack? = null
    private val cleanupHandler = Handler(Looper.getMainLooper())
    private var cleanup: Runnable? = null

    fun play(frequencyHz: Double, durationMs: Int = 1800): Boolean {
        stop()
        if (frequencyHz !in 20.0..5000.0) return false
        val sampleRate = 44_100
        val samples = referenceToneSamples(frequencyHz, sampleRate, durationMs)
        return runCatching {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                audioTrack.release()
                return@runCatching false
            }
            val written = audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            if (written != samples.size) {
                audioTrack.release()
                return@runCatching false
            }
            audioTrack.setVolume(.42f)
            track = audioTrack
            audioTrack.play()
            val thisTrack = audioTrack
            cleanup = Runnable {
                if (track === thisTrack) stop()
            }.also { cleanupHandler.postDelayed(it, durationMs + 250L) }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        cleanup?.let(cleanupHandler::removeCallbacks)
        cleanup = null
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
    }
}

internal fun referenceToneSamples(frequencyHz: Double, sampleRate: Int, durationMs: Int): ShortArray {
    if (frequencyHz <= 0.0 || sampleRate <= 0 || durationMs <= 0) return ShortArray(0)
    val count = sampleRate * durationMs / 1000
    return ShortArray(count) { index ->
        val fade = minOf(1.0, index / 600.0, (count - index) / 900.0).coerceAtLeast(0.0)
        (sin(2.0 * Math.PI * frequencyHz * index / sampleRate) * fade * Short.MAX_VALUE * .42).toInt().toShort()
    }
}
