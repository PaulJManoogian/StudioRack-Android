package com.manoogianmedia.studiorack.performance

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/** Captures decoded PCM without changing the audio sent to the output device. */
@UnstableApi
class PlaybackPeakMeter : TeeAudioProcessor.AudioBufferSink {
    private val peakBits = AtomicInteger(0f.toBits())
    @Volatile private var encoding: Int = C.ENCODING_PCM_16BIT

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.encoding = encoding
        peakBits.set(0f.toBits())
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val measured = pcmPeak(buffer, encoding)
        while (true) {
            val previousBits = peakBits.get()
            val previous = Float.fromBits(previousBits)
            val next = maxOf(measured, previous * 0.82f)
            if (peakBits.compareAndSet(previousBits, next.toBits())) break
        }
    }

    fun readPeak(): Float {
        while (true) {
            val currentBits = peakBits.get()
            val current = Float.fromBits(currentBits)
            if (peakBits.compareAndSet(currentBits, (current * 0.58f).toBits())) return current.coerceIn(0f, 1f)
        }
    }
}

@UnstableApi
class MeteringRenderersFactory(
    context: Context,
    private val meter: PlaybackPeakMeter,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(TeeAudioProcessor(meter)))
            .build()
}

internal fun pcmPeak(source: ByteBuffer, encoding: Int): Float {
    val buffer = source.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    var peak = 0f
    when (encoding) {
        C.ENCODING_PCM_8BIT -> while (buffer.hasRemaining()) {
            peak = maxOf(peak, abs((buffer.get().toInt() and 0xFF) - 128) / 128f)
        }
        C.ENCODING_PCM_16BIT -> while (buffer.remaining() >= 2) {
            peak = maxOf(peak, abs(buffer.short.toInt()) / 32768f)
        }
        C.ENCODING_PCM_24BIT -> while (buffer.remaining() >= 3) {
            val raw = (buffer.get().toInt() and 0xFF) or
                ((buffer.get().toInt() and 0xFF) shl 8) or
                (buffer.get().toInt() shl 16)
            peak = maxOf(peak, abs(raw) / 8_388_608f)
        }
        C.ENCODING_PCM_32BIT -> while (buffer.remaining() >= 4) {
            peak = maxOf(peak, abs(buffer.int.toLong()) / 2_147_483_648f)
        }
        C.ENCODING_PCM_FLOAT -> while (buffer.remaining() >= 4) {
            peak = maxOf(peak, abs(buffer.float))
        }
    }
    return peak.coerceIn(0f, 1f)
}
