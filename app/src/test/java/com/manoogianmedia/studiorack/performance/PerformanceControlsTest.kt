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

    @Test
    fun settingsRoundTripForOfflinePersistence() {
        val expected = PerformanceSettings(
            metronomeAutostart = true,
            metronomeMode = "downbeat",
            metronomeSound = "cowbell",
            pedalEnabled = true,
            pedalMode = "scroll",
            pedalReverse = true,
            pedalScrollAmount = "full",
            previousKey = "ArrowUp",
            nextKey = "ArrowDown",
            metronomeKey = "ArrowLeft",
            muteKey = "ArrowRight",
            showClock = false,
        )
        val encoded = expected.toJson(pendingSync = true)
        assertEquals(1, encoded.getInt("_mobile_pending"))
        assertEquals(expected, PerformanceSettings.fromJson(encoded.toString()))
    }
}
