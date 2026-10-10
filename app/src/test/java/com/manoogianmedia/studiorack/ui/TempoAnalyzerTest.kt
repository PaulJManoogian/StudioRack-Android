package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoAnalyzerTest {
    @Test
    fun detectsSteadyOneHundredTwentyBpmPulse() {
        val reading = calculateTempo((0L..8L).map { it * 500L })
        assertEquals(120.0, reading.bpm, .15)
        assertTrue(reading.confidence > .8f)
    }

    @Test
    fun foldsVeryFastSubdivisionsIntoUsableRange() {
        val reading = calculateTempo((0L..8L).map { it * 250L })
        assertEquals(120.0, reading.bpm, .15)
    }

    @Test
    fun rejectsTooFewBeats() {
        assertEquals(0.0, calculateTempo(listOf(0L, 500L)).bpm, 0.0)
    }
}
