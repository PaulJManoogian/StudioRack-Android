package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeMetronomeTest {
    @Test
    fun regularGridIncludesEverySubdivision() {
        assertEquals(64, practiceGrid(PracticeMetronomeConfig(meter = 4, beatUnit = 4, subdivision = 64)))
    }

    @Test
    fun polyrhythmGridAlignsBothPulsesAndBeatSubdivisions() {
        val config = PracticeMetronomeConfig(meter = 4, beatUnit = 4, subdivision = 8, polyrhythm = true, pulseA = 3, pulseB = 2)
        assertEquals(24, practiceGrid(config))
    }

    @Test
    fun halfNotePulseUsesOneSchedulerStepPerBeat() {
        assertEquals(4, practiceGrid(PracticeMetronomeConfig(meter = 4, beatUnit = 4, subdivision = 2)))
    }

    @Test
    fun accentListAlwaysMatchesChangedMeter() {
        assertEquals(listOf(2, 1, 0, 1), normalizePracticeAccents(listOf(2, 1, 0, 1, 1, 1), 4))
        assertEquals(listOf(2, 1, 0, 1, 1, 1), normalizePracticeAccents(listOf(2, 1, 0, 1), 6))
    }
}
