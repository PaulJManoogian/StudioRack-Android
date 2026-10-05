package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MultitrackSelectionTest {
    @Test
    fun singleArrangementBecomesDefaultWhenEntryHasNoPlaybackChoice() {
        val arrangement = JSONObject().put("id", "arr_1").put("song_id", "song_1")
        val entry = JSONObject().put("song_id", "song_1")

        val selected = selectPlaybackArrangement(
            entry,
            mapOf("arr_1" to arrangement),
            mapOf("song_1" to listOf(arrangement)),
        )

        assertSame(arrangement, selected)
    }

    @Test
    fun explicitAttachmentPreventsImplicitArrangementSelection() {
        val arrangement = JSONObject().put("id", "arr_1").put("song_id", "song_1")
        val entry = JSONObject().put("song_id", "song_1").put("playback_attachment_id", "stem_1")

        val selected = selectPlaybackArrangement(
            entry,
            mapOf("arr_1" to arrangement),
            mapOf("song_1" to listOf(arrangement)),
        )

        assertNull(selected)
    }
}
