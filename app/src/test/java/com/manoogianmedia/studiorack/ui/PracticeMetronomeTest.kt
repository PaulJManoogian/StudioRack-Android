package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeMetronomeTest {
    @Test
    fun regularGridIncludesEverySubdivision() {
        assertEquals(16, practiceGrid(PracticeMetronomeConfig(meter = 4, subdivision = 4)))
    }

    @Test
    fun polyrhythmGridAlignsBothPulsesAndBeatSubdivisions() {
        val config = PracticeMetronomeConfig(meter = 4, subdivision = 2, polyrhythm = true, pulseA = 3, pulseB = 2)
        assertEquals(24, practiceGrid(config))
    }
}
