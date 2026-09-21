package com.manoogianmedia.studiorack.performance

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.roundToInt

data class PcmStemRoute(
    val file: File,
    val outputStartChannel: Int,
    val outputChannelCount: Int,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    val offsetMs: Long = 0,
    val muted: Boolean = false,
)

class MultichannelPcmEngine private constructor(
    private val stems: List<OpenStem>,
    private val outputChannels: Int,
    private val sampleRate: Int,
    preferredDevice: AudioDeviceInfo?,
) : Closeable {
    private val running = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val framePosition = AtomicLong(0)
    private val bytesPerOutputFrame = outputChannels * 2
    private val bufferFrames = 512
    private val track: AudioTrack
    private var worker: Thread? = null

    val positionMs: Long get() = framePosition.get() * 1000L / sampleRate
    val durationMs: Long = stems.maxOfOrNull { it.durationFrames * 1000L / sampleRate } ?: 0L
    val isPlaying: Boolean get() = running.get()

    init {
        require(outputChannels in 2..24) { "Discrete output supports 2 through 24 channels." }
        val channelMask = (1 shl outputChannels) - 1
        val minimum = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelIndexMask(channelMask).build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minimum, bufferFrames * bytesPerOutputFrame * 4))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        if (preferredDevice != null) track.preferredDevice = preferredDevice
    }

    fun play() {
        if (released.get() || running.getAndSet(true)) return
        track.play()
        worker = thread(name = "LeviathanLivePcm", isDaemon = true, start = true) { renderLoop() }
    }

    fun pause() {
        running.set(false)
        worker?.join(500)
        worker = null
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
    }

    fun stop() {
        pause()
        seekTo(0)
        track.flush()
    }

    fun seekTo(positionMs: Long) {
        val frame = (positionMs.coerceAtLeast(0) * sampleRate / 1000L)
        framePosition.set(frame)
        stems.forEach { it.seek(frame) }
        if (!running.get()) track.flush()
    }

    override fun close() {
        if (!released.compareAndSet(false, true)) return
        pause()
        stems.forEach(OpenStem::close)
        track.release()
    }

    private fun renderLoop() {
        val mix = FloatArray(bufferFrames * outputChannels)
        val output = ByteArray(bufferFrames * bytesPerOutputFrame)
        while (running.get() && !released.get()) {
            mix.fill(0f)
            var anyData = false
            stems.forEach { stem -> if (stem.mix(framePosition.get(), bufferFrames, mix, outputChannels)) anyData = true }
            if (!anyData) {
                running.set(false)
                break
            }
            var outputIndex = 0
            mix.forEach { value ->
                val sample = (value.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort().toInt()
                output[outputIndex++] = (sample and 0xff).toByte()
                output[outputIndex++] = ((sample shr 8) and 0xff).toByte()
            }
            val written = track.write(output, 0, output.size, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                running.set(false)
                break
            }
            framePosition.addAndGet((written / bytesPerOutputFrame).toLong())
        }
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
    }

    private class OpenStem(val route: PcmStemRoute, val wav: WavPcm16) : Closeable {
        val durationFrames: Long get() = wav.frameCount
        private val input = ShortArray(512 * wav.channels)

        fun seek(masterFrame: Long) {
            val offsetFrames = route.offsetMs * wav.sampleRate / 1000L
            wav.seekFrame((masterFrame + offsetFrames).coerceAtLeast(0))
        }

        fun mix(masterFrame: Long, frames: Int, mix: FloatArray, outputChannels: Int): Boolean {
            if (route.muted) return false
            seek(masterFrame)
            val readFrames = wav.readFrames(input, frames)
            if (readFrames <= 0) return false
            val gain = 10.0.pow(route.gainDb.toDouble() / 20.0).toFloat()
            val start = (route.outputStartChannel - 1).coerceIn(0, outputChannels - 1)
            val width = route.outputChannelCount.coerceIn(1, outputChannels - start)
            for (frame in 0 until readFrames) {
                val left = input[frame * wav.channels].toFloat() / Short.MAX_VALUE
                val right = if (wav.channels > 1) input[frame * wav.channels + 1].toFloat() / Short.MAX_VALUE else left
                val pan = route.pan.coerceIn(-1f, 1f)
                val leftGain = if (pan > 0) 1f - pan else 1f
                val rightGain = if (pan < 0) 1f + pan else 1f
                val base = frame * outputChannels + start
                if (width == 1) mix[base] += ((left + right) * .5f) * gain
                else {
                    mix[base] += left * gain * leftGain
                    mix[base + 1] += right * gain * rightGain
                }
            }
            return true
        }

        override fun close() = wav.close()
    }

    companion object {
        fun open(stems: List<PcmStemRoute>, outputChannels: Int, preferredDevice: AudioDeviceInfo?): MultichannelPcmEngine? {
            val opened = mutableListOf<OpenStem>()
            return try {
                require(stems.isNotEmpty())
                stems.forEach { route -> opened += OpenStem(route, WavPcm16.open(route.file)) }
                val sampleRate = opened.first().wav.sampleRate
                require(opened.all { it.wav.sampleRate == sampleRate }) { "All stems must use the same sample rate." }
                MultichannelPcmEngine(opened, outputChannels, sampleRate, preferredDevice)
            } catch (_: Exception) {
                opened.forEach(OpenStem::close)
                null
            }
        }
    }
}

private class WavPcm16 private constructor(
    private val file: RandomAccessFile,
    val channels: Int,
    val sampleRate: Int,
    private val dataOffset: Long,
    private val dataSize: Long,
) : Closeable {
    val frameCount: Long = dataSize / (channels * 2L)

    fun seekFrame(frame: Long) = file.seek(dataOffset + frame.coerceIn(0, frameCount) * channels * 2L)

    fun readFrames(target: ShortArray, requestedFrames: Int): Int {
        val bytes = ByteArray(requestedFrames * channels * 2)
        val read = file.read(bytes)
        if (read <= 0) return 0
        val sampleCount = read / 2
        for (index in 0 until sampleCount) {
            val low = bytes[index * 2].toInt() and 0xff
            val high = bytes[index * 2 + 1].toInt()
            target[index] = ((high shl 8) or low).toShort()
        }
        return sampleCount / channels
    }

    override fun close() = file.close()

    companion object {
        fun open(source: File): WavPcm16 {
            val file = RandomAccessFile(source, "r")
            require(readAscii(file, 4) == "RIFF")
            readUInt32(file)
            require(readAscii(file, 4) == "WAVE")
            var channels = 0
            var sampleRate = 0
            var bits = 0
            var format = 0
            var dataOffset = 0L
            var dataSize = 0L
            while (file.filePointer + 8 <= file.length()) {
                val id = readAscii(file, 4)
                val size = readUInt32(file)
                val next = file.filePointer + size + (size and 1L)
                if (id == "fmt ") {
                    format = readUInt16(file)
                    channels = readUInt16(file)
                    sampleRate = readUInt32(file).toInt()
                    file.skipBytes(6)
                    bits = readUInt16(file)
                } else if (id == "data") {
                    dataOffset = file.filePointer
                    dataSize = size
                }
                file.seek(next)
                if (channels > 0 && dataOffset > 0) break
            }
            require(format == 1 && bits == 16 && channels in 1..2 && sampleRate > 0) { "Only mono or stereo PCM16 WAV stems are supported for discrete routing." }
            return WavPcm16(file, channels, sampleRate, dataOffset, dataSize).also { it.seekFrame(0) }
        }

        private fun readAscii(file: RandomAccessFile, count: Int) = ByteArray(count).also(file::readFully).toString(Charsets.US_ASCII)
        private fun readUInt16(file: RandomAccessFile): Int {
            val low = file.readUnsignedByte(); val high = file.readUnsignedByte(); return low or (high shl 8)
        }
        private fun readUInt32(file: RandomAccessFile): Long {
            val low = readUInt16(file).toLong(); val high = readUInt16(file).toLong(); return low or (high shl 16)
        }
    }
}
