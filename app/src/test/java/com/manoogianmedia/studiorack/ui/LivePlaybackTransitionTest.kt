package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LivePlaybackTransitionTest {
    @Test
    fun firstSongDoesNotCreateAnAutoplayRequest() {
        assertEquals(
            0,
            nextPlaybackAutoStartRequest(
                currentRequest = 0,
                songChanged = false,
                playbackEnabled = true,
                autoPlayEnabled = true,
            ),
        )
    }

    @Test
    fun songTransitionFollowsTheFourPlaybackStates() {
        assertEquals(1, nextPlaybackAutoStartRequest(0, true, playbackEnabled = true, autoPlayEnabled = true))
        assertEquals(0, nextPlaybackAutoStartRequest(3, true, playbackEnabled = true, autoPlayEnabled = false))
        assertEquals(0, nextPlaybackAutoStartRequest(3, true, playbackEnabled = false, autoPlayEnabled = true))
        assertEquals(0, nextPlaybackAutoStartRequest(3, true, playbackEnabled = false, autoPlayEnabled = false))
    }

    @Test
    fun eachEligibleTransitionCreatesANewRequest() {
        assertEquals(8, nextPlaybackAutoStartRequest(7, true, playbackEnabled = true, autoPlayEnabled = true))
        assertEquals(1, nextPlaybackAutoStartRequest(Int.MAX_VALUE, true, playbackEnabled = true, autoPlayEnabled = true))
    }
}
