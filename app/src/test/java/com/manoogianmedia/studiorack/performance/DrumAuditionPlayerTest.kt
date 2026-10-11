package com.manoogianmedia.studiorack.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrumAuditionPlayerTest {
    @Test
    fun onboardVoicesProduceAudiblePcmForCoreKitPieces() {
        listOf(36, 38, 42, 46, 45, 49, 51).forEach { note ->
            val samples = percussionSamples(note)
            assertTrue("GM note $note should contain audio", samples.any { it.toInt() != 0 })
            assertTrue("GM note $note should have a useful duration", samples.size >= DRUM_AUDITION_SAMPLE_RATE / 12)
        }
    }

    @Test
    fun openHatAndCrashRingLongerThanClosedHat() {
        val closedHat = percussionSamples(42)
        assertTrue(percussionSamples(46).size > closedHat.size)
        assertTrue(percussionSamples(49).size > closedHat.size)
        assertEquals((DRUM_AUDITION_SAMPLE_RATE * 0.9).toInt(), percussionSamples(49).size)
    }
}
