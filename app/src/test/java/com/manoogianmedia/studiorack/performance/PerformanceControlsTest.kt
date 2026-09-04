package com.manoogianmedia.studiorack.performance

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformanceControlsTest {
    @Test
    fun mapsAllFourAssignablePedalActions() {
        val settings = PerformanceSettings(
            previousKey = "ArrowDown",
            nextKey = "ArrowUp",
            metronomeKey = "ArrowRight",
            muteKey = "ArrowLeft",
        )
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_DPAD_DOWN, settings))
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
        assertEquals(PedalAction.METRONOME, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertEquals(PedalAction.MUTE, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertNull(mappedPedalAction(KeyEvent.KEYCODE_ENTER, settings))
    }

    @Test
    fun reverseOnlySwapsSongNavigation() {
        val settings = PerformanceSettings(pedalReverse = true)
        assertEquals(PedalAction.NEXT, mappedPedalAction(KeyEvent.KEYCODE_DPAD_LEFT, settings))
        assertEquals(PedalAction.PREVIOUS, mappedPedalAction(KeyEvent.KEYCODE_DPAD_RIGHT, settings))
        assertEquals(PedalAction.METRONOME, mappedPedalAction(KeyEvent.KEYCODE_DPAD_UP, settings))
        assertEquals(PedalAction.MUTE, mappedPedalAction(KeyEvent.KEYCODE_DPAD_DOWN, settings))
    }
}
