package com.manoogianmedia.studiorack.performance

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class MeteringAudioTest {
    @Test
    fun readsSixteenBitPcmWithoutMovingThePlaybackBuffer() {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(0)
            .putShort(8_192)
            .putShort((-16_384).toShort())
            .putShort(2_048)
            .flip() as ByteBuffer
        val originalPosition = buffer.position()

        assertEquals(0.5f, pcmPeak(buffer, C.ENCODING_PCM_16BIT), 0.001f)
        assertEquals(originalPosition, buffer.position())
    }

    @Test
    fun readsFloatPcmAndClampsOverload() {
        val buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putFloat(-0.25f)
            .putFloat(0.72f)
            .putFloat(1.4f)
            .flip() as ByteBuffer

        assertEquals(1f, pcmPeak(buffer, C.ENCODING_PCM_FLOAT), 0.001f)
    }
}
