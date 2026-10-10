package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class PitchAnalyzerTest {
    @Test
    fun detectsConcertA() {
        val rate = 48_000
        val samples = FloatArray(4096) { index -> (sin(2.0 * Math.PI * 440.0 * index / rate) * .45).toFloat() }
        val reading = PitchAnalyzer.analyze(samples, rate)

        assertEquals(PitchState.Clear, reading.state)
        assertTrue(reading.frequencyHz in 437.0..443.0)
        assertTrue(reading.confidence > .8f)
    }

    @Test
    fun rejectsQuietSignal() {
        val reading = PitchAnalyzer.analyze(FloatArray(4096) { .0002f }, 48_000)
        assertEquals(PitchState.Weak, reading.state)
    }

    @Test
    fun identifiesClipping() {
        val samples = FloatArray(4096) { if (it % 2 == 0) 1f else -1f }
        assertEquals(PitchState.Clipping, PitchAnalyzer.analyze(samples, 48_000).state)
    }
}
