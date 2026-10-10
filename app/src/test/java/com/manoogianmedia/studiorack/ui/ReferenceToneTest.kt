package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceToneTest {
    @Test
    fun referenceToneProducesAudiblePcmWithFadedEdges() {
        val samples = referenceToneSamples(440.0, 44_100, 1_800)

        assertEquals(79_380, samples.size)
        assertEquals(0, samples.first().toInt())
        assertTrue(samples.any { kotlin.math.abs(it.toInt()) > 10_000 })
        assertTrue(kotlin.math.abs(samples.last().toInt()) < 1_000)
    }

    @Test
    fun invalidToneInputsProduceNoSamples() {
        assertTrue(referenceToneSamples(0.0, 44_100, 1_800).isEmpty())
        assertTrue(referenceToneSamples(440.0, 0, 1_800).isEmpty())
        assertTrue(referenceToneSamples(440.0, 44_100, 0).isEmpty())
    }
}
